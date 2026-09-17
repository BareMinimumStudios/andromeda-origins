package andromeda.origins.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * Keeps Arachne's cobweb movement immunity without granting an Apoli phasing power.
 *
 * <p>The old data power used origins:phasing as a convenient way to suppress the
 * cobweb collision callback. That also made Arachne a phasing-power holder while
 * climbing, which could interact with Apoli's collision/push-out hooks and permit
 * wall clipping. This mixin bypasses only PlayerEntity#slowMovement for blocks in
 * Origins' cobweb tag.</p>
 *
 * <p>The server-side Arachne command tag is used as the cheap fast path. Command
 * tags are not relied on for client prediction; the fallback checks the synced
 * Apoli power component reflectively for the standard or Champion Arachne parent
 * power. Reflection keeps Andromeda's existing no-hard-Apoli-Java-dependency
 * boundary while avoiding client/server cobweb movement disagreement.</p>
 */
@Mixin(PlayerEntity.class)
public abstract class ArachneCobwebMovementMixin {

    @Unique
    private static final TagKey<Block> ANDROMEDA$COBWEBS = TagKey.of(
        RegistryKeys.BLOCK,
        Identifier.of("origins", "cobwebs")
    );

    @Unique
    private static final Identifier ANDROMEDA$ARACHNE = Identifier.of("andromeda_origins", "arachne/passives");

    @Unique
    private static final Identifier ANDROMEDA$CHAMPION_ARACHNE = Identifier.of("andromeda_origins", "champion/arachne_passives");

    @Unique
    private static volatile boolean andromeda$reflectionUnavailable;

    @Unique
    private static Method andromeda$getComponent;

    @Unique
    private static Method andromeda$getPower;

    @Unique
    private static Method andromeda$hasPower;

    @Inject(method = "slowMovement", at = @At("HEAD"), cancellable = true)
    private void andromeda$ignoreCobwebSlowdown(BlockState state, Vec3d multiplier, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (state.isIn(ANDROMEDA$COBWEBS) && andromeda$isArachne(player)) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean andromeda$isArachne(PlayerEntity player) {
        if (player.getCommandTags().contains("arachne")) {
            return true;
        }
        if (andromeda$reflectionUnavailable) {
            return false;
        }

        try {
            andromeda$resolveReflection();
            Object component = andromeda$getComponent.invoke(null, (Entity) player);
            if (component == null) {
                return false;
            }

            Object standard = andromeda$getPower.invoke(null, ANDROMEDA$ARACHNE);
            if (standard != null && Boolean.TRUE.equals(andromeda$hasPower.invoke(component, standard))) {
                return true;
            }

            Object champion = andromeda$getPower.invoke(null, ANDROMEDA$CHAMPION_ARACHNE);
            return champion != null && Boolean.TRUE.equals(andromeda$hasPower.invoke(component, champion));
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

        andromeda$getComponent = componentClass.getMethod("getNullable", Entity.class);
        andromeda$getPower = powerManagerClass.getMethod("getNullable", Identifier.class);
        andromeda$hasPower = componentClass.getMethod("hasPower", powerClass);
    }
}
