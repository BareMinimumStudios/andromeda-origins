package andromeda.origins.mixin

import andromeda.origins.compat.CrowdControlLifecycle
import dev.overgrown.apoli.power.PowerContainer
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** Reconcile saved combat restraints after horse recall, chunk load and revival. */
@Mixin(LivingEntity::class)
abstract class NonPlayerRestraintMixin {
    @Inject(method = ["tick"], at = [At("TAIL")])
    private fun `andromeda$recoverRestraints`(ci: CallbackInfo) {
        val entity = this as Any as LivingEntity
        if (entity.level().isClientSide || entity is ServerPlayer || entity.tickCount % 20 != 0) return
        val modifier = ResourceLocation.parse("apoli:attribute/andromeda_origins/common_restrained_speed/0")
        val held = entity.getAttribute(Attributes.MOVEMENT_SPEED)?.hasModifier(modifier) == true
        if (held || PowerContainer.of(entity)?.hasPower(ResourceLocation.parse("andromeda_origins:common/restrained")) == true)
            CrowdControlLifecycle.reconcile(entity)
    }
}
