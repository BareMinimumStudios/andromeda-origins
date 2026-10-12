package andromeda.origins.mixin.compat

import dev.overgrown.apoli.power.builtin.NightVisionPower
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.LivingEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Iris bypasses Opoli's LightTexture hook when supplying the shader nightVision uniform. */
@Pseudo
@Mixin(targets = ["net.irisshaders.iris.uniforms.CommonUniforms"], remap = false)
object IrisNightVisionMixin {
    @JvmStatic
    @Inject(method = ["getNightVision()F"], at = [At("RETURN")], cancellable = true, remap = false, require = 0)
    private fun `andromeda$shaderNightVision`(cir: CallbackInfoReturnable<Float>) {
        val camera = Minecraft.getInstance().cameraEntity as? LivingEntity ?: return
        val strength = NightVisionPower.strengthFor(camera).coerceIn(0f, 1f)
        // Preserve Iris's vanilla potion/underwater value; never reduce an existing brightness.
        if (strength > cir.returnValueF) cir.returnValue = strength
    }
}
