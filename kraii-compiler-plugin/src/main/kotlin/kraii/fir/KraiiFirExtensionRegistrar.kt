package kraii.fir

import kraii.KraiiPluginKey
import kraii.autoCloseableClassId
import kraii.closeName
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

class KraiiFirExtensionRegistrar : FirExtensionRegistrar() {

  override fun ExtensionRegistrarContext.configurePlugin() {
    +::KraiiAddCloseMethodExtension
  }

}

/**
 * Adds a [AutoCloseable.close] method to all implementations of [AutoCloseable].
 */
class KraiiAddCloseMethodExtension(session: FirSession) :
  FirDeclarationGenerationExtension(session) {

  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
  ): Set<Name> {
    if (!classSymbol.implementsAutoClosable()) return emptySet()
    if (classSymbol.hasCloseMethod()) return emptySet()
    return setOf(closeName)
  }

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
      ).symbol
    )
  }

  private fun FirClassSymbol<*>.implementsAutoClosable() =
    lookupSuperTypes(
      symbol = this,
      lookupInterfaces = true,
      deep = true,
      useSiteSession = session,
    ).find { it.classId == autoCloseableClassId } != null

  @Suppress("DEPRECATION")
  private fun FirClassSymbol<*>.hasCloseMethod() =
    declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>()
      .filter { it.name == closeName }
      .any { it.valueParameterSymbols.isEmpty() }

}
