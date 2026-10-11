package andromeda.origins.compat

import andromeda.origins.compat.EnhancedFxCompat


 /**
 * Lightweight presentation effects for Humanity's Mortal Resolve.
 *
 * Commands are used deliberately here: all effects are one-shot or very short-lived, they stay
 * server-authoritative for nearby players, and this avoids introducing a client packet solely for
 * cosmetic particles/sounds.
 */
object MortalResolveEffects {
    private fun run(player: net.minecraft.server.level.ServerPlayer, command: kotlin.String) {
        val silentSource: net.minecraft.commands.CommandSourceStack = player.createCommandSourceStack()
        .withPermission(4)
        .withSource(net.minecraft.commands.CommandSource.NULL)

        player.serverLevel().getServer().getCommands()
        .performPrefixedCommand(silentSource, command)
    }

     /** Dramatic one-time burst when the downed Human rises.  */
    fun playActivation(player: net.minecraft.server.level.ServerPlayer) {
        // The custom heartbeat is a short pulse. Replaying it from the player during the final
        // stand keeps it audible while the Human moves instead of leaving a 60-second sound
        // source behind at the revival location.
        MortalResolveEffects.playHeartbeatPulse(player)
        EnhancedFxCompat.play(player, "humanity.mortal_resolve")
        MortalResolveEffects.run(player, "playsound andromeda_origins:ability.humanity.mortal_resolve_ignite player @a[distance=..32] ~ ~ ~ 0.95 1.0 0")
        MortalResolveEffects.run(player, "playsound minecraft:block.beacon.activate player @a[distance=..32] ~ ~ ~ 0.65 0.78 0")
        MortalResolveEffects.run(player, "playsound minecraft:entity.player.levelup player @a[distance=..32] ~ ~ ~ 0.35 0.68 0")

        MortalResolveEffects.run(player, "particle minecraft:flash ~ ~1 ~ 0 0 0 0 1 force @a[distance=..48]")
        MortalResolveEffects.run(player, "particle minecraft:flame ~ ~1 ~ 0.60 0.95 0.60 0.025 50 force @a[distance=..48]")
        MortalResolveEffects.run(player, "particle minecraft:end_rod ~ ~1 ~ 0.55 0.90 0.55 0.025 32 force @a[distance=..48]")
        MortalResolveEffects.run(player, "particle minecraft:electric_spark ~ ~1 ~ 0.55 0.75 0.55 0.08 20 force @a[distance=..48]")
    }


     /** Repositions the custom heartbeat on the Human so it remains audible while moving.  */
    fun playHeartbeatPulse(player: net.minecraft.server.level.ServerPlayer) {
        // Play a guaranteed local pulse to the Human first, then a normal positional copy for
        // nearby listeners. Keeping the self copy on MASTER with minimum volume 1 avoids the
        // heartbeat disappearing because of player-category attenuation or selector edge cases.
        MortalResolveEffects.run(player, "playsound andromeda_origins:ability.humanity.mortal_resolve_heartbeat master @s ~ ~ ~ 2.0 1.0 1.0")
        MortalResolveEffects.run(player, "playsound andromeda_origins:ability.humanity.mortal_resolve_heartbeat player @a[distance=0.1..40] ~ ~ ~ 1.6 1.0 0")
    }

     /** Sparse rising particles during the first moments after activation.  */
    fun playAfterglow(player: net.minecraft.server.level.ServerPlayer) {
        MortalResolveEffects.run(player, "particle minecraft:flame ~ ~1 ~ 0.32 0.65 0.32 0.012 3 force @a[distance=..48]")
        MortalResolveEffects.run(player, "particle minecraft:end_rod ~ ~1 ~ 0.30 0.70 0.30 0.015 2 force @a[distance=..48]")
    }

     /** Stops any in-progress custom heartbeat pulse when the final stand ends.  */
    fun stopHeartbeat(player: net.minecraft.server.level.ServerPlayer) {
        MortalResolveEffects.run(player, "stopsound @s master andromeda_origins:ability.humanity.mortal_resolve_heartbeat")
        MortalResolveEffects.run(player, "stopsound @a[distance=..40] player andromeda_origins:ability.humanity.mortal_resolve_heartbeat")
    }

     /** Contrasting cue for the forced death when the borrowed minute runs out.  */
    fun playExpiry(player: net.minecraft.server.level.ServerPlayer) {
        MortalResolveEffects.stopHeartbeat(player)
        EnhancedFxCompat.play(player, "humanity.mortal_resolve_expiry")
        MortalResolveEffects.run(player, "playsound minecraft:entity.warden.heartbeat player @a[distance=..40] ~ ~ ~ 0.55 0.55 0")
        MortalResolveEffects.run(player, "particle minecraft:smoke ~ ~1 ~ 0.35 0.65 0.35 0.02 18 force @a[distance=..48]")
    }
}
