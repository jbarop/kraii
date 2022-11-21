package kraii

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

/**
 * Adds [AutoCloseable.close] to classes which implement [AutoCloseable].
 */
class KraiiSyntheticResolveExtension : SyntheticResolveExtension {

  override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
    listOf(closeName)

  override fun generateSyntheticMethods(
    thisDescriptor: ClassDescriptor,
    name: Name,
    bindingContext: BindingContext,
    fromSupertypes: List<SimpleFunctionDescriptor>,
    result: MutableCollection<SimpleFunctionDescriptor>,
  ) {
    if (name != closeName) return
    if (result.any { it.name == closeName }) return
    if (!thisDescriptor.implementsAutoClosable()) return

    val functionDescriptor = SimpleFunctionDescriptorImpl.create(
      /* containingDeclaration = */ thisDescriptor,
      /* annotations = */ Annotations.EMPTY,
      /* name = */ name,
      /* kind = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
      /* source = */ thisDescriptor.source,
    )

    functionDescriptor.initialize(
      /* extensionReceiverParameter = */ null,
      /* dispatchReceiverParameter = */ thisDescriptor.thisAsReceiverParameter,
      /* contextReceiverParameters = */ emptyList(),
      /* typeParameters = */ emptyList(),
      /* unsubstitutedValueParameters = */ emptyList(),
      /* unsubstitutedReturnType = */ thisDescriptor.builtIns.unitType,
      /* modality = */ Modality.OPEN,
      /* visibility = */ DescriptorVisibilities.PUBLIC,
    )

    result += functionDescriptor
  }

}
