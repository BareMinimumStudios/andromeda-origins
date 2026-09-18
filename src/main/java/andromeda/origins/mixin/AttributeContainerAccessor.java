package andromeda.origins.mixin;

import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(AttributeContainer.class)
public interface AttributeContainerAccessor {

    @Accessor("custom")
    Map<RegistryEntry<EntityAttribute>, EntityAttributeInstance> andromeda$getCustomAttributes();
}
