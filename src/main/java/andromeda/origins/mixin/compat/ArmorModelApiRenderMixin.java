package andromeda.origins.mixin.compat;

import andromeda.origins.client.compat.FiguraArmorVisibilityCompat;
import andromeda.origins.compat.UndetectableCompat;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional Armor Model API bridge.
 *
 * <p>Armor Model API renders registered geo armor through a path that is
 * separate from Figura's vanilla ArmorFeatureRenderer handling. Cancelling the
 * dispatcher here when Figura has hidden the matching armor slot makes custom
 * geo armor obey the same avatar visibility setting. The shared Andromeda
 * Undetectable state is also honored so custom geo armor cannot remain visible
 * around an otherwise invisible player. Returning {@code true} means the armor
 * was intentionally handled so no vanilla fallback is drawn.</p>
 */
@Pseudo
@Mixin(targets = "net.rpg_foundation.armor_api.client.ArmorRenderDispatcher", remap = false)
public abstract class ArmorModelApiRenderMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void andromeda_origins$respectFiguraArmorVisibility(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            ItemStack stack,
            LivingEntity entity,
            EquipmentSlot slot,
            int light,
            BipedEntityModel<LivingEntity> contextModel,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (UndetectableCompat.isUndetectable(entity)
                || FiguraArmorVisibilityCompat.shouldHideArmor(entity, slot)) {
            cir.setReturnValue(true);
        }
    }
}
