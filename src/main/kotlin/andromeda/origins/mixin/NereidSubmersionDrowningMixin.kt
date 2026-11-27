package andromeda.origins.mixin

import andromeda.origins.compat.NereidSubmersionDrowningMixinState
import dev.overgrown.apoli.mixin.flag.LivingEntityAirAccessor
import kotlin.math.max
import net.minecraft.server.level.ServerPlayer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

import andromeda.origins.compat.NereidGills


 /** Drain after the player air update so vanilla recovery cannot undo Submersion. */
@Mixin(ServerPlayer::class)
abstract class NereidSubmersionDrowningMixin {
    @Unique
    var `andromeda$airAtStart`: Int = 0
    @Unique
    var `andromeda$active`: Boolean = false
    @Unique
    var `andromeda$gills`: Boolean = false
    @Unique
    var `andromeda$damageTicks`: Int = 0

    @Inject(method = ["doTick"], at = [At("HEAD")]) fun `andromeda$capture`(ci: CallbackInfo?) {
        val player: ServerPlayer = this as Any as ServerPlayer
        val gills = NereidGills.isDry(player)
        val hostile = player.tags.contains(NereidSubmersionDrowningMixinState.`ANDROMEDA$TAG`) &&
            NereidSubmersionDrowningMixinState.`andromeda$shouldDrown`(player)
        if (!hostile) player.removeTag(NereidSubmersionDrowningMixinState.`ANDROMEDA$TAG`)
        if (gills != `andromeda$gills`) `andromeda$damageTicks` = 0
        `andromeda$gills` = gills
        `andromeda$active` = gills || hostile
        if (!`andromeda$active`) {
            `andromeda$damageTicks` = 0
            return
        }
        `andromeda$airAtStart` = player.getAirSupply()
    }

    @Inject(method = ["doTick"], at = [At("TAIL")]) fun `andromeda$apply`(ci: CallbackInfo?) {
        if (!`andromeda$active`) return
        val player: ServerPlayer = this as Any as ServerPlayer
        if (!player.isAlive() || if (`andromeda$gills`) !NereidGills.isDry(player) else !player.getTags().contains(NereidSubmersionDrowningMixinState.`ANDROMEDA$TAG`)) {
            `andromeda$damageTicks` = 0
            return
        }
        val nextAir = max(0, if (`andromeda$gills`)
            (player as LivingEntityAirAccessor).`apoli$decreaseAirSupply`(`andromeda$airAtStart`)
            else `andromeda$airAtStart` - 5)
        player.setAirSupply(nextAir)
        if (nextAir > 0) {
            `andromeda$damageTicks` = 0
            return
        }
        if (++`andromeda$damageTicks` >= 20) {
            `andromeda$damageTicks` = 0
            player.hurt(player.damageSources().drown(), 2.0f)
        }
    }

    }

