package kraii.fir

import kraii.KraiiPluginKey
import kraii.autoCloseableClassId
import kraii.closeName
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

/**
 * FIR extension that adds a `close()` method declaration to `AutoCloseable`
 * classes that don't already define one.
 *
 * This runs during the FIR (frontend) phase so that the generated `close()`
 * method is visible to the type checker. The actual method body is filled in
 * later during the IR phase by `KraiiCloseMethodBodyGenerator`.
 *
 * Only classes that implement `AutoCloseable` (directly or transitively) and
 * do not already declare a no-arg `close()` method are affected.
 */
class KraiiAddCloseMethodExtension(
  session: FirSession,
) : FirDeclarationGenerationExtension(session) {

  /**
   * Reports that `close` should be generated for classes that implement
   * `AutoCloseable` but don't already have a `close()` method.
   *
   * The compiler calls this to discover which callable names this extension
   * can generate for the given class.
   */
  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
  ): Set<Name> {
    if (!classSymbol.implementsAutoClosable()) return emptySet()
    if (classSymbol.hasCloseMethod()) return emptySet()
    return setOf(closeName)
  }

  /**
   * Generates the `close()` function stub for the owning class.
   *
   * The `context` parameter is nullable per the API contract. When null,
   * we return an empty list since there is no owning class to attach the
   * method to.
   */
  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?,
  ): List<FirNamedFunctionSymbol> {
    val owner = context?.owner ?: return emptyList()

    return listOf(
      createMemberFunction(
        owner = owner,
        key = KraiiPluginKey,
        name = callableId.callableName,
        returnType = session.builtinTypes.unitType.coneType,
      ).symbol,
    )
  }

  /**
   * Checks whether this class implements `AutoCloseable` (directly or
   * transitively).
   **/
  private fun FirClassSymbol<*>.implementsAutoClosable() =
    lookupSuperTypes(
      symbol = this,
      lookupInterfaces = true,
      deep = true,
      useSiteSession = session,
    ).any { it.classId == autoCloseableClassId }

  /**
   * Checks whether this class already declares a no-arg `close()` method.
   **/
  @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
  private fun FirClassSymbol<*>.hasCloseMethod(): Boolean =
    fir.declarations
      .filterIsInstance<FirNamedFunction>()
      .any { it.name == closeName && it.valueParameters.isEmpty() }
}
