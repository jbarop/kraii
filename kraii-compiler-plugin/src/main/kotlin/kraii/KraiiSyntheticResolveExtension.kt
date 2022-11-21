package kraii

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory.simpleNotNullType
import org.jetbrains.kotlin.types.TypeAttributes

/**
 * Makes a class implement [AutoCloseable].
 */
class KraiiSyntheticResolveExtension : SyntheticResolveExtension {

  override fun addSyntheticSupertypes(
    thisDescriptor: ClassDescriptor,
    supertypes: MutableList<KotlinType>,
  ) {
    val autoClosableDescriptor =
      thisDescriptor.module.findClassAcrossModuleDependencies(autoCloseableClassId)
        ?: error("Cloud not find $autoCloseableClassId")

    supertypes.add(
      simpleNotNullType(
        attributes = TypeAttributes.Empty,
        descriptor = autoClosableDescriptor,
        arguments = emptyList(),
      )
    )
  }

  override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
    listOf(closeName)

  override fun generateSyntheticMethods(
    thisDescriptor: ClassDescriptor,
    name: Name,
    bindingContext: BindingContext,
    fromSupertypes: List<SimpleFunctionDescriptor>,
    result: MutableCollection<SimpleFunctionDescriptor>,
  ) {
    if (thisDescriptor.implementsAutoClosable() && name == closeName) {
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

}
