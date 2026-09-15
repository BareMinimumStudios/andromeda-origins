package andromeda.origins.mixin;

import andromeda.origins.compat.ApoliPerformanceCompat;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Avoids Apoli's O(all loaded entities) scan every time a non-player entity is
 * destroyed. The compatibility helper only visits holders that currently own
 * an EntitySet power and falls back to Apoli's original callback if reflection
 * cannot be resolved.
 */
@Pseudo
@Mixin(targets = "io.github.apace100.apoli.power.type.EntitySetPowerType", remap = false)
public abstract class ApoliEntitySetUnloadMixin {
    @Inject(
        method = "integrateUnloadCallback",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0
    )
    private static void andromeda$optimizeEntitySetUnload(Entity unloadedEntity, ServerWorld world, CallbackInfo ci) {
        if (ApoliPerformanceCompat.handleEntitySetUnload(unloadedEntity, world)) {
            ci.cancel();
        }
    }
}
