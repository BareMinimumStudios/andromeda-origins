package andromeda.origins.mixin.compat

import andromeda.origins.client.compat.FiguraArmorVisibilityCompat
import andromeda.origins.compat.UndetectableCompat


 /**
 * Optional Armor Model API bridge.
 *
 *
 * Armor Model API renders registered geo armor through a path that is
 * separate from Figura's vanilla ArmorFeatureRenderer handling. Cancelling the
 * dispatcher here when Figura has hidden the matching armor slot makes custom
 * geo armor obey the same avatar visibility setting. The shared Andromeda
 * Undetectable state is also honored so custom geo armor cannot remain visible
 * around an otherwise invisible player. Returning `true` means the armor
 * was intentionally handled so no vanilla fallback is drawn.
 */
@org.spongepowered.asm.mixin.Pseudo @org.spongepowered.asm.mixin.Mixin(targets = ["net.rpg_foundation.armor_api.client.ArmorRenderDispatcher"], remap = false)
object ArmorModelApiRenderMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["render"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true, remap = false, require = 0) @JvmStatic private fun `andromeda_origins$respectFiguraArmorVisibility`(
    matrices: com.mojang.blaze3d.vertex.PoseStack,
    vertexConsumers: net.minecraft.client.renderer.MultiBufferSource,
    stack: net.minecraft.world.item.ItemStack,
    entity: net.minecraft.world.entity.LivingEntity,
    slot: net.minecraft.world.entity.EquipmentSlot,
    light: kotlin.Int,
    contextModel: net.minecraft.client.model.HumanoidModel<net.minecraft.world.entity.LivingEntity>,
    cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<kotlin.Boolean>
    ) {
        if (andromeda.origins.compat.VeilbornStealthCompat.isVeilStep(entity)
        || UndetectableCompat.isUndetectable(entity)
        || FiguraArmorVisibilityCompat.shouldHideArmor(entity, slot)) {
            cir.setReturnValue(true)
        }
    }
}
