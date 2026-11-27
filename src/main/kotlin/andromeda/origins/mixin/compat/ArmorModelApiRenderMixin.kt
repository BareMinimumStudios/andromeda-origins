package andromeda.origins.mixin.compat

import andromeda.origins.client.compat.FiguraArmorVisibilityCompat
import andromeda.origins.compat.UndetectableCompat
import andromeda.origins.compat.VeilbornStealthCompat
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable


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
@Pseudo @Mixin(targets = ["net.rpg_foundation.armor_api.client.ArmorRenderDispatcher"], remap = false)
object ArmorModelApiRenderMixin {
    @Inject(method = ["render"], at = [At("HEAD")], cancellable = true, remap = false, require = 0) @JvmStatic private fun `andromeda_origins$respectFiguraArmorVisibility`(
    matrices: PoseStack,
    vertexConsumers: MultiBufferSource,
    stack: ItemStack,
    entity: LivingEntity,
    slot: EquipmentSlot,
    light: Int,
    contextModel: HumanoidModel<LivingEntity>,
    cir: CallbackInfoReturnable<Boolean>
    ) {
        if (VeilbornStealthCompat.isVeilStep(entity)
        || UndetectableCompat.isUndetectable(entity)
        || FiguraArmorVisibilityCompat.shouldHideArmor(entity, slot)) {
            cir.setReturnValue(true)
        }
    }
}
