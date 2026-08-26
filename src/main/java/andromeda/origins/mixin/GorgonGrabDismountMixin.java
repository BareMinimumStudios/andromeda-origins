package andromeda.origins.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class GorgonGrabDismountMixin {
    private static final String GORGON_TAG = "gorgon";
    private static final String GRABBED_TAG = "andromeda_gorgon_grabbed";

    @Inject(method = "stopRiding", at = @At("HEAD"), cancellable = true)
    private void andromedaOrigins$preventGrabbedPlayerDismount(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity) || !self.getCommandTags().contains(GRABBED_TAG)) {
            return;
        }

        Entity vehicle = self.getVehicle();
        if (vehicle != null
            && vehicle.isAlive()
            && !vehicle.isRemoved()
            && vehicle.getCommandTags().contains(GORGON_TAG)) {
            ci.cancel();
        }
    }
}
