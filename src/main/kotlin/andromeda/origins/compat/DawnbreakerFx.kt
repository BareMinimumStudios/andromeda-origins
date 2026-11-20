package andromeda.origins.compat

import andromeda.origins.network.DawnbreakerFxPayload
import andromeda.origins.platform.Platform
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

object DawnbreakerFx {
    /** Step goes to nearby observers; screen effects are interpreted only by the actor's client. */
    fun cue(player: ServerPlayer, kind: Int, at: Vec3 = player.position()) {
        val s = DawnbreakerPresentation.settings()
        val impact = s.impactFrames.get() && when (kind) { 0 -> s.stepImpactFrames.get(); 1, 2 -> s.guardImpactFrames.get(); 3 -> s.bowImpactFrames.get(); else -> false }
        if (!Dawnbreaker.radiant(player) || (!impact && !(kind == 0 && s.afterimagesCompat.get()))) return
        val packet = DawnbreakerFxPayload(kind, player.id, at, (if (kind == 0 && s.afterimagesCompat.get()) 1 else 0) or (if (impact) 2 else 0))
        val viewers = if (kind == 0) player.serverLevel().players().filter { it === player || it.distanceToSqr(at) <= 48.0 * 48.0 } else listOf(player)
        for (viewer in viewers) if (Platform.canReceiveCosmetic(viewer, DawnbreakerFxPayload.TYPE.id())) Platform.sendCosmetic(viewer, packet)
    }
}
