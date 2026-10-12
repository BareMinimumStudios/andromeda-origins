package andromeda.origins.mixin

import andromeda.origins.compat.NereidSubmersionDrowningMixinState

import andromeda.origins.compat.NereidGills


 /** Drain after the player air update so vanilla recovery cannot undo Submersion. */
@org.spongepowered.asm.mixin.Mixin(net.minecraft.server.level.ServerPlayer::class)
abstract class NereidSubmersionDrowningMixin {
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$airAtStart`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$active`: kotlin.Boolean = false
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$gills`: kotlin.Boolean = false
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$damageTicks`: kotlin.Int = 0

    @org.spongepowered.asm.mixin.injection.Inject(method = ["doTick"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")]) fun `andromeda$capture`(ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo?) {
        val player: net.minecraft.server.level.ServerPlayer = this as kotlin.Any as net.minecraft.server.level.ServerPlayer
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

    @org.spongepowered.asm.mixin.injection.Inject(method = ["doTick"], at = [org.spongepowered.asm.mixin.injection.At("TAIL")]) fun `andromeda$apply`(ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo?) {
        if (!`andromeda$active`) return
        val player: net.minecraft.server.level.ServerPlayer = this as kotlin.Any as net.minecraft.server.level.ServerPlayer
        if (!player.isAlive() || if (`andromeda$gills`) !NereidGills.isDry(player) else !player.getTags().contains(NereidSubmersionDrowningMixinState.`ANDROMEDA$TAG`)) {
            `andromeda$damageTicks` = 0
            return
        }
        val nextAir = kotlin.math.max(0, if (`andromeda$gills`)
            (player as dev.overgrown.apoli.mixin.flag.LivingEntityAirAccessor).`apoli$decreaseAirSupply`(`andromeda$airAtStart`)
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

