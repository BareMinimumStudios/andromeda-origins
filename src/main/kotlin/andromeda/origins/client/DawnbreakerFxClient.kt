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
    private var openMenuRequested = false
    fun openConfig() { openMenuRequested = true }
    fun register() {
        config = me.fzzyhmstrs.fzzy_config.api.ConfigApiJava.registerAndLoadConfig(java.util.function.Supplier { DawnbreakerVisualConfig() }, me.fzzyhmstrs.fzzy_config.api.RegisterType.CLIENT)
        ClientPlatform.endTick { c ->
            // ChatScreen closes itself after executing a command. Open after that close, not inside it.
            if (openMenuRequested) {
                openMenuRequested = false
                me.fzzyhmstrs.fzzy_config.api.ConfigApiJava.openScreen("andromeda_origins.dawnbreaker_visuals")
            }
            if (world !== c.level || c.player?.isAlive != true) { ghosts.clear(); impactAt = 0; nextImpact = 0; world = c.level }
            ghosts.entries.removeIf { it.key.isRemoved || System.nanoTime() >= it.value.until }
        }
        // Render at the departure point independently of the arrival entity's frustum/first-person pass.
        ClientPlatform.worldFx { context ->
            for (entity in ghosts.keys.toList()) renderGhost(entity, context.delta, context.pose, context.buffers)
        }
    }
    fun hasGhost(entity: LivingEntity) = config.afterimages.get() && ghosts[entity]?.let { System.nanoTime() < it.until } == true
    fun receive(packet: DawnbreakerFxPayload) {
        val c = Minecraft.getInstance(); val level = c.level ?: return; val player = c.player ?: return
        if (packet.kind !in 0..3 || !packet.position.x.isFinite() || !packet.position.y.isFinite() || !packet.position.z.isFinite()) return
        val actor = level.getEntity(packet.actor) as? LivingEntity ?: return
        if (actor.isInvisibleTo(player) || !actor.isAlive || actor.distanceToSqr(player) > 64.0 * 64.0) return
        val now = System.nanoTime()
        if (packet.kind == 0 && config.afterimages.get() && packet.flags and 1 != 0 && ghosts.size < 32) {
            ghosts[actor] = Ghost(packet.position, actor.yRot, actor.xRot, actor.yBodyRot, actor.yHeadRot, now + 450_000_000L)
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
        renderingGhost = true
        return try {
            val opacity = ((ghost.until - now) / 450_000_000.0).toFloat() * .65f
            if (!AfterimagesCompat.render(entity, ghost.at, ghost.yaw, ghost.pitch, ghost.body, ghost.head, opacity, delta, pose, buffers)) {
                // The bundled fallback has the same independent lifetime and world-space anchoring.
                val camera = Minecraft.getInstance().gameRenderer.mainCamera.position
                pose.pushPose(); pose.translate(ghost.at.x-camera.x, ghost.at.y-camera.y, ghost.at.z-camera.z)
                pose.mulPose(Axis.YP.rotationDegrees(-ghost.yaw))
                val scale=(entity.bbHeight/1.8f).coerceIn(.35f,1.8f);pose.scale(scale,scale,scale)
                val vertices=buffers.getBuffer(net.minecraft.client.renderer.RenderType.debugQuads())
                val matrix=pose.last().pose()
                fun band(x:Double,y:Double,bx:Double,by:Double) {
                    fun point(a:Double,b:Double){vertices.addVertex(matrix,a.toFloat(),b.toFloat(),0f).setColor(255,216,120,(opacity*255).toInt())}
                    point(x-.015,y);point(bx-.015,by);point(bx+.015,by);point(x+.015,y)
                }
                for(side in listOf(-1.0,1.0)) for(i in 0 until 16) {
                    val u=i/16.0;val v=(i+1)/16.0
                    band(side*(.15+.18*kotlin.math.sin(u*Math.PI)),.1+u*1.5,side*(.15+.18*kotlin.math.sin(v*Math.PI)),.1+v*1.5)
                }
                for(i in 0 until 24) {val a=i*Math.PI/12;val b=(i+1)*Math.PI/12;band(kotlin.math.cos(a)*.13,1.75+kotlin.math.sin(a)*.13,kotlin.math.cos(b)*.13,1.75+kotlin.math.sin(b)*.13)}
                pose.popPose()
                (buffers as? MultiBufferSource.BufferSource)?.endBatch(net.minecraft.client.renderer.RenderType.debugQuads())
            }
            true
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
