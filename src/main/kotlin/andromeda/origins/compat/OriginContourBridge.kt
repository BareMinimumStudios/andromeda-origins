package andromeda.origins.compat
import andromeda.origins.network.OriginFxPayload
import andromeda.origins.platform.Platform
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

object OriginContourBridge {
    private val families = listOf("arachne","faerie","fenrkin","gorgon","humanity","lichling","manticore","nereid","satyr","selkie","siren","veilborn","wyverian")
    fun play(entity: Entity, event: String, position: Vec3?, definition: EnhancedFxDefinitions.EventDefinition): Boolean {
        if (!andromeda.origins.config.AndromedaConfigBootstrap.config().visuals.refinedContours.get()) return false
        val family = families.indexOf(event.substringBefore('.')); if (family < 0) return false
        val level = entity.level() as? ServerLevel ?: return false; val at = position ?: entity.position()
        val viewers = level.players().filter { it.distanceToSqr(at) <= 32.0 * 32.0 }
        if (viewers.isEmpty()) return true
        if (viewers.any { !Platform.canReceiveCosmetic(it, OriginFxPayload.TYPE.id()) }) return false
        val mode = if (listOf("pulse","tick","trail","hover","shimmer","unstable","breath").any { event.contains(it) }) 1 else if (listOf("impact","hit","landing","aftershock","expiry").any { event.contains(it) }) 2 else 0
        val visual = definition.mrpg
        val packet = OriginFxPayload(family,entity.id,at,visual?.color ?: 0xd8d8e0,(visual?.radius ?: .8f).coerceIn(.35f,1.25f),mode or (if (position == null && visual?.attach == true) 4 else 0),OriginFxStyle.quality().ordinal)
        for (viewer in viewers) Platform.sendCosmetic(viewer,packet)
        return true
    }
}
