package andromeda.origins.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * Makes hostile Nereid Submersion drain a target's real air supply instead of
 * relying on Origins' gill power or Apoli's ignore-water movement shortcut.
 *
 * <p>The old combination was internally contradictory: Submersion granted Wet,
 * which suppressed its own land-gill helper, while ignore_water made submerged
 * players stop participating in normal water movement and could prevent vanilla
 * drowning from behaving normally. The corrected mechanic keeps real water
 * physics and drains air directly only for non-aquatic hostile targets.</p>
 *
 * <p>A command tag is the cheap per-tick fast path. When the tag is present we
 * additionally verify that the actual Submersion debuff power still exists; a
 * stale tag is removed immediately instead of allowing permanent drowning.</p>
 */
@Mixin(ServerPlayerEntity.class)
public abstract class NereidSubmersionDrowningMixin {

    @Unique
    private static final String ANDROMEDA$SUBMERSION_DROWN_TAG = "andromeda_nereid_submersion_drown";

    @Unique
    private static final Identifier ANDROMEDA$SUBMERSION_DEBUFF =
        Identifier.of("andromeda_origins", "nereid/helper/whirlpooldebuff");

    @Unique
    private static final Identifier ANDROMEDA$AQUATIC_ORIGIN =
        Identifier.of("andromeda_origins", "common/aquatic_origin");

    @Unique
    private static volatile boolean andromeda$reflectionUnavailable;

    @Unique
    private static Method andromeda$getComponent;

    @Unique
    private static Method andromeda$getPower;

    @Unique
    private static Method andromeda$hasPower;

    @Unique
    private static Method andromeda$getPowerTypes;

    @Unique
    private static Class<?> andromeda$waterBreathingPowerType;

    @Unique
    private int andromeda$submersionAirAtTickStart;

    @Unique
    private boolean andromeda$submersionDrowningAtTickStart;

    @Unique
    private int andromeda$submersionDamageTicks;

    @Inject(method = "tick", at = @At("HEAD"))
    private void andromeda$captureSubmersionAir(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        if (!player.getCommandTags().contains(ANDROMEDA$SUBMERSION_DROWN_TAG)) {
            andromeda$submersionDrowningAtTickStart = false;
            andromeda$submersionDamageTicks = 0;
            return;
        }

        if (!andromeda$hasSubmersionDebuff(player) || andromeda$isAquatic(player)) {
            player.removeCommandTag(ANDROMEDA$SUBMERSION_DROWN_TAG);
            andromeda$submersionDrowningAtTickStart = false;
            andromeda$submersionDamageTicks = 0;
            return;
        }

        andromeda$submersionDrowningAtTickStart = true;
        andromeda$submersionAirAtTickStart = player.getAir();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void andromeda$applySubmersionDrowning(CallbackInfo ci) {
        if (!andromeda$submersionDrowningAtTickStart) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        // HEAD already performed the authoritative Apoli/debuff/aquatic validation for this tick.
        // Avoid repeating two reflective component scans at TAIL; only cheap state that can change
        // during the tick is checked here. At worst, a power removed mid-tick can drain one final
        // five-air step, while the next HEAD immediately clears the tag/state.
        if (!player.isAlive() || !player.getCommandTags().contains(ANDROMEDA$SUBMERSION_DROWN_TAG)) {
            player.removeCommandTag(ANDROMEDA$SUBMERSION_DROWN_TAG);
            andromeda$submersionDamageTicks = 0;
            return;
        }

        // Origins' normal gill weakness drains land air much faster than vanilla
        // underwater breathing. Five points/tick preserves that threatening feel,
        // while actual drowning damage remains one heart per second after air is gone.
        int nextAir = Math.max(0, andromeda$submersionAirAtTickStart - 5);
        player.setAir(nextAir);

        if (nextAir > 0) {
            andromeda$submersionDamageTicks = 0;
            return;
        }

        andromeda$submersionDamageTicks++;
        if (andromeda$submersionDamageTicks >= 20) {
            andromeda$submersionDamageTicks = 0;
            player.damage(player.getDamageSources().drown(), 2.0F);
        }
    }

    @Unique
    private static boolean andromeda$isAquatic(ServerPlayerEntity player) {
        if (andromeda$reflectionUnavailable) {
            return false;
        }

        try {
            andromeda$resolveReflection();
            Object component = andromeda$getComponent.invoke(null, (Entity) player);
            if (component == null) {
                return false;
            }

            // All Andromeda aquatic Origins own this direct marker, including Siren and
            // Champion Nereid whose breathing is not represented by origins:water_breathing.
            Object marker = andromeda$getPower.invoke(null, ANDROMEDA$AQUATIC_ORIGIN);
            if (marker != null && Boolean.TRUE.equals(andromeda$hasPower.invoke(component, marker))) {
                return true;
            }

            // External aquatic Origins commonly expose a WaterBreathingPowerType under their own
            // power ID. Check the actual type rather than relying on the literal Origins power ID.
            Object value = andromeda$getPowerTypes.invoke(component, andromeda$waterBreathingPowerType);
            return value instanceof java.util.Collection<?> collection && !collection.isEmpty();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            andromeda$reflectionUnavailable = true;
            return false;
        }
    }

    @Unique
    private static boolean andromeda$hasSubmersionDebuff(ServerPlayerEntity player) {
        if (andromeda$reflectionUnavailable) {
            return false;
        }

        try {
            andromeda$resolveReflection();
            Object component = andromeda$getComponent.invoke(null, (Entity) player);
            if (component == null) {
                return false;
            }
            Object power = andromeda$getPower.invoke(null, ANDROMEDA$SUBMERSION_DEBUFF);
            return power != null && Boolean.TRUE.equals(andromeda$hasPower.invoke(component, power));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            andromeda$reflectionUnavailable = true;
            return false;
        }
    }

    @Unique
    private static synchronized void andromeda$resolveReflection() throws ReflectiveOperationException {
        if (andromeda$getComponent != null) {
            return;
        }

        Class<?> componentClass = Class.forName("io.github.apace100.apoli.component.PowerHolderComponent");
        Class<?> powerClass = Class.forName("io.github.apace100.apoli.power.Power");
        Class<?> powerManagerClass = Class.forName("io.github.apace100.apoli.power.PowerManager");
        andromeda$waterBreathingPowerType =
            Class.forName("io.github.apace100.origins.power.type.WaterBreathingPowerType");

        andromeda$getComponent = componentClass.getMethod("getNullable", Entity.class);
        andromeda$getPower = powerManagerClass.getMethod("getNullable", Identifier.class);
        andromeda$hasPower = componentClass.getMethod("hasPower", powerClass);
        andromeda$getPowerTypes = componentClass.getMethod("getPowerTypes", Class.class);
    }
}
