package andromeda.origins.mixin

import andromeda.origins.compat.NereidSubmersionDrowningMixinState

import andromeda.origins.compat.AndromedaOriginTags


 /** Server-authoritative real-air drain for hostile Nereid Submersion.  */
@org.spongepowered.asm.mixin.Mixin(net.minecraft.server.level.ServerPlayer::class)
abstract class NereidSubmersionDrowningMixin {
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$airAtStart`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$active`: kotlin.Boolean = false
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$damageTicks`: kotlin.Int = 0

    @org.spongepowered.asm.mixin.injection.Inject(method = ["tick"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")]) fun `andromeda$capture`(ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo?) {
        val player: net.minecraft.server.level.ServerPlayer = this as kotlin.Any as net.minecraft.server.level.ServerPlayer
        if (!player.getTags().contains(NereidSubmersionDrowningMixinState.`ANDROMEDA$TAG`)) {
            `andromeda$active` = false
            `andromeda$damageTicks` = 0
            return
        }
        if (!NereidSubmersionDrowningMixinState.`andromeda$shouldDrown`(player)) {
            player.removeTag(NereidSubmersionDrowningMixinState.`ANDROMEDA$TAG`)
            `andromeda$active` = false
            `andromeda$damageTicks` = 0
            return
        }
        `andromeda$active` = true
        `andromeda$airAtStart` = player.getAirSupply()
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = ["tick"], at = [org.spongepowered.asm.mixin.injection.At("TAIL")]) fun `andromeda$apply`(ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo?) {
        if (!`andromeda$active`) return
        val player: net.minecraft.server.level.ServerPlayer = this as kotlin.Any as net.minecraft.server.level.ServerPlayer
        if (!player.isAlive() || !player.getTags().contains(NereidSubmersionDrowningMixinState.`ANDROMEDA$TAG`)) {
            `andromeda$damageTicks` = 0
            return
        }
        val nextAir: kotlin.Int = kotlin.math.max(0, `andromeda$airAtStart` - 5)
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

