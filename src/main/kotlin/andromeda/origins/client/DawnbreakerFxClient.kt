package andromeda.origins.client

import andromeda.origins.client.config.DawnbreakerVisualConfig
import andromeda.origins.compat.DawnbreakerPresentation
import andromeda.origins.network.DawnbreakerFxPayload
import andromeda.origins.platform.ClientPlatform
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource

object DawnbreakerFxClient {
    lateinit var config: DawnbreakerVisualConfig
    private data class Ghost(val at: Vec3, val yaw: Float, val pitch: Float, val body: Float, val head: Float, val until: Long)
    private val ghosts = java.util.WeakHashMap<LivingEntity, Ghost>()
    var renderingGhost = false; private set
    private var world: Any? = null
    private var impactKind = 0
    private var impactAt = 0L
    private var nextImpact = 0L
    fun openConfig() { me.fzzyhmstrs.fzzy_config.api.ConfigApiJava.openScreen("andromeda_origins.dawnbreaker_visuals") }
    fun register() {
        config = me.fzzyhmstrs.fzzy_config.api.ConfigApiJava.registerAndLoadConfig(java.util.function.Supplier { DawnbreakerVisualConfig() }, me.fzzyhmstrs.fzzy_config.api.RegisterType.CLIENT)
        ClientPlatform.endTick { c ->
            if (world !== c.level || c.player?.isAlive != true) { ghosts.clear(); impactAt = 0; nextImpact = 0; world = c.level }
            ghosts.entries.removeIf { it.key.isRemoved || System.nanoTime() >= it.value.until }
        }
    }
    fun receive(packet: DawnbreakerFxPayload) {
        val c = Minecraft.getInstance(); val level = c.level ?: return; val player = c.player ?: return
        if (packet.kind !in 0..3 || !packet.position.x.isFinite() || !packet.position.y.isFinite() || !packet.position.z.isFinite()) return
        val actor = level.getEntity(packet.actor) as? LivingEntity ?: return
        if (actor.isInvisibleTo(player) || !actor.isAlive || actor.distanceToSqr(player) > 64.0 * 64.0) return
        val now = System.nanoTime()
        if (packet.kind == 0 && config.afterimages.get() && packet.flags and 1 != 0 && ghosts.size < 32) {
            ghosts[actor] = Ghost(packet.position, actor.yRot, actor.xRot, actor.yBodyRot, actor.yHeadRot, now + 200_000_000L)
        }
        val allowed = packet.flags and 2 != 0
        if (actor === player && allowed && config.impactFrames.get() && !player.isSpectator && c.screen == null && now >= nextImpact) {
            impactKind = packet.kind
            impactAt = now; nextImpact = now + 300_000_000L
        }
    }
    fun renderGhost(entity: LivingEntity, delta: Float, pose: PoseStack, buffers: MultiBufferSource): Boolean {
        if (renderingGhost || !config.afterimages.get()) return false
        val ghost = ghosts[entity] ?: return false; val now = System.nanoTime()
        if (now >= ghost.until || Minecraft.getInstance().player?.let { entity.isInvisibleTo(it) } != false) { ghosts.remove(entity); return false }
        if (entity === Minecraft.getInstance().player && Minecraft.getInstance().options.cameraType.isFirstPerson) return false
        renderingGhost = true
        return try {
            AfterimagesCompat.render(entity, ghost.at, ghost.yaw, ghost.pitch, ghost.body, ghost.head, ((ghost.until - now) / 200_000_000.0).toFloat() * .45f, delta, pose, buffers)
        } finally { renderingGhost = false }
    }
    fun renderImpact(g: GuiGraphics) {
        val c = Minecraft.getInstance(); val start = impactAt
        if (start == 0L || !config.impactFrames.get() || c.level == null || c.player?.isAlive != true || c.player?.isSpectator == true || c.screen != null || c.options.hideGui) { impactAt = 0; return }
        val age = (System.nanoTime() - start) / 1_000_000.0
        if (age >= config.durationMs.get()) { impactAt = 0; return }
        val scale = when (config.intensity.get()) { DawnbreakerVisualConfig.Intensity.LOW -> .3; DawnbreakerVisualConfig.Intensity.STANDARD -> .55; else -> .8 }
        ImpactComposition.render(g, impactKind, (age / config.durationMs.get()).toFloat(), scale.toFloat())
    }
}
