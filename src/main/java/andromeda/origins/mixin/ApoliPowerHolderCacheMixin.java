package andromeda.origins.mixin;

import andromeda.origins.compat.ApoliPerformanceCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Apoli's stock getPowerTypes(Class, boolean) walks every power owned by an
 * entity for every query. A number of Apoli mixins call it from extremely hot
 * vanilla paths (attributes, collision, invisibility, fire checks, tags, etc.).
 *
 * <p>This mixin caches only the class-membership portion of that lookup. Active
 * state is still evaluated on every call, so conditioned powers keep their
 * normal behavior. The cache is cleared whenever the component mutates.</p>
 */
@Pseudo
@Mixin(targets = "io.github.apace100.apoli.component.PowerHolderComponentImpl", remap = false)
public abstract class ApoliPowerHolderCacheMixin {
    @Unique
    private final Map<Class<?>, List<Object>> andromeda$typeMembershipCache = new ConcurrentHashMap<>();

    @Unique
    private static final Map<Class<?>, Method> andromeda$isActiveMethods = new ConcurrentHashMap<>();

    @Inject(
        method = "getPowerTypes(Ljava/lang/Class;Z)Ljava/util/List;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0
    )
    private void andromeda$cachedPowerTypeLookup(
        Class<?> typeClass,
        boolean includeInactive,
        CallbackInfoReturnable<List<?>> cir
    ) {
        try {
            List<Object> candidates = andromeda$typeMembershipCache.computeIfAbsent(typeClass, requestedClass -> {
                List<Object> matches = new ArrayList<>();
                for (Object powerType : ApoliPerformanceCompat.getPowerTypesSnapshot(this)) {
                    if (requestedClass.isInstance(powerType)) {
                        matches.add(powerType);
                    }
                }
                return matches;
            });

            if (includeInactive || candidates.isEmpty()) {
                cir.setReturnValue(new ArrayList<>(candidates));
                return;
            }

            List<Object> active = new ArrayList<>(candidates.size());
            for (Object powerType : candidates) {
                Method isActive = andromeda$isActiveMethods.computeIfAbsent(powerType.getClass(), type -> {
                    try {
                        return type.getMethod("isActive");
                    } catch (ReflectiveOperationException exception) {
                        throw new IllegalStateException(exception);
                    }
                });

                if ((boolean) isActive.invoke(powerType)) {
                    active.add(powerType);
                }
            }

            cir.setReturnValue(active);
        } catch (Throwable ignored) {
            // If an upstream implementation changes, leave the callback unset
            // so Apoli's original implementation runs normally.
            andromeda$typeMembershipCache.clear();
        }
    }

    @Inject(method = {"addPower", "removePower"}, at = @At("RETURN"), remap = false, require = 0)
    private void andromeda$invalidateAfterPowerMutation(CallbackInfoReturnable<Boolean> cir) {
        andromeda$refreshCaches();
    }

    @Inject(method = {"readFromNbt", "applySyncPacket"}, at = @At("RETURN"), remap = false, require = 0)
    private void andromeda$invalidateAfterPowerLoad(CallbackInfo ci) {
        andromeda$refreshCaches();
    }

    @Unique
    private void andromeda$refreshCaches() {
        andromeda$typeMembershipCache.clear();
        ApoliPerformanceCompat.refreshEntitySetHolderFromComponent(this);
    }
}
