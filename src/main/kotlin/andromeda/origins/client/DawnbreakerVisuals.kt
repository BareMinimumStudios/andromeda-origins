package andromeda.origins.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import org.joml.Matrix4f
import kotlin.math.*

/** Independent, texture-free radiant geometry inspired by Dawnquiver's layered light presentation. */
object DawnbreakerVisuals {
    private val active = ResourceLocation.parse("andromeda_origins:dawnbreaker/presence_active")
    private val visible = ResourceLocation.parse("andromeda_origins:dawnbreaker/presence_radiance")
    private val feature = ResourceLocation.parse("andromeda_origins:dawnbreaker/radiance")
    private val appearing = java.util.WeakHashMap<LivingEntity, Float>()
    fun render(entity: LivingEntity, x: Double, y: Double, z: Double, delta: Float, pose: PoseStack, buffers: MultiBufferSource) {
        val viewer = Minecraft.getInstance().player ?: return
        if (entity.isInvisibleTo(viewer) || viewer.distanceToSqr(entity) > 48.0 * 48.0) return
        val c = PowerContainer.of(entity) ?: return
        SovereignVisuals.render(entity, x, y, z, delta, pose, buffers)
        if (!c.hasPower(feature) || c.isSuppressed(feature) || PowerResources.read(c, active).orElse(0.0) <= 0 ||
            PowerResources.read(c, visible).orElse(0.0) <= 0) { appearing.remove(entity); return }
        val age = entity.tickCount + delta
        val started = appearing.getOrPut(entity) { age }
        val remaining = PowerResources.read(c, active).orElse(0.0).toFloat()
        val fade = min(1f, min((age - started) / 10f, remaining / 12f)).coerceIn(0f, 1f)
        if (fade <= 0) return
        pose.pushPose()
        pose.translate(x, y, z)
        pose.mulPose(Axis.YP.rotationDegrees(-net.minecraft.util.Mth.rotLerp(delta, entity.yBodyRotO, entity.yBodyRot)))
        val scale = (entity.bbHeight / 1.8f).coerceIn(.65f, 1.6f)
        pose.scale(scale, scale, scale)
        pose.translate(0.0, 0.0, -.70)
        val vertices = buffers.getBuffer(RenderType.debugQuads())
        val matrix = pose.last().pose()
        val breath = .94f + sin(age * .025f) * .06f
        // A broad amber annulus with separate ivory/gold edges; no additive washout in daylight.
        halo(vertices, matrix, .72, .82, 0.0, Math.PI * 2, 224, 136, 22, fade * .25f)
        halo(vertices, matrix, .816, .834, 0.0, Math.PI * 2, 255, 199, 70, fade * .88f * breath)
        halo(vertices, matrix, .704, .716, 0.0, Math.PI * 2, 255, 239, 180, fade * .75f)
        val phase = age * .002
        for (i in 0..2) {
            val start = phase + i * Math.PI * 2 / 3
            halo(vertices, matrix, .91, .928, start, start + 1.15, 255, 174, 45, fade * .55f)
        }
        // Tapered feathers have filled surfaces, separated spines and angled banks for side views.
        for (side in listOf(-1.0, 1.0)) {
            pose.pushPose(); pose.translate(0.0, 0.0, .16)
            pose.mulPose(Axis.YP.rotationDegrees((side * 16).toFloat()))
            for (f in 0..5) {
                pose.pushPose(); pose.translate(0.0, 0.0, -f * .018)
                val wing = pose.last().pose()
                val sweep = sin(age * .018 + f * .18) * .025
                val rootX = side * (.23 + f * .05)
                val rootY = .92 + f * .14
                val tipX = side * (2.45 - f * .12)
                val tipY = .85 + f * .40 + sweep
                val midX = rootX + (tipX - rootX) * .58
                val midY = rootY + (tipY - rootY) * .58
                val width = .13 + f * .012
                quad(vertices, wing, listOf(rootX to rootY, midX to midY - width, tipX to tipY, midX to midY + width),
                    255, 179, 43, fade * (.40f + f * .035f))
                line(vertices, wing, rootX, rootY, tipX, tipY, .019, 255, 242, 190, fade * .92f)
                pose.popPose()
            }
            pose.popPose()
        }
        for (i in 0..7) {
            val angle = i * Math.PI / 4 + Math.PI / 8
            val x = cos(angle); val y = sin(angle)
            line(vertices, matrix, x * .97, 1.55 + y * .97, x * 1.12, 1.55 + y * 1.12,
                .025, 255, 193, 66, fade * .85f)
        }
        pose.popPose()
    }
    private fun halo(v: VertexConsumer, m: Matrix4f, inner: Double, outer: Double, start: Double, end: Double,
        r: Int, g: Int, b: Int, alpha: Float) {
        for (i in 0 until 64) {
            val a = start + (end - start) * i / 64
            val bAngle = start + (end - start) * (i + 1) / 64
            quad(v, m, listOf(cos(a) * inner to 1.55 + sin(a) * inner,
                cos(bAngle) * inner to 1.55 + sin(bAngle) * inner,
                cos(bAngle) * outer to 1.55 + sin(bAngle) * outer,
                cos(a) * outer to 1.55 + sin(a) * outer), r, g, b, alpha)
        }
    }
    private fun quad(v: VertexConsumer, m: Matrix4f, points: List<Pair<Double, Double>>, r: Int, g: Int, b: Int, alpha: Float) {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        for ((x, y) in points)
            v.addVertex(m, x.toFloat(), y.toFloat(), 0f).setColor(r, g, b, a)
    }
    private val projectileVisuals = java.util.WeakHashMap<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity>()
    fun renderProjectile(entity: net.minecraft.world.entity.Entity, x: Double, y: Double, z: Double, pose: PoseStack, buffers: MultiBufferSource) {
        if (entity !is net.minecraft.world.entity.projectile.AbstractArrow || entity.customName?.string != "Dawnbreaker Light Arrow") return
        val owner = entity.owner as? LivingEntity ?: return
        val c = PowerContainer.of(owner) ?: return
        if (!c.hasPower(feature) || c.isSuppressed(feature) || PowerResources.read(c, ResourceLocation.parse("andromeda_origins:dawnbreaker/bow_radiance")).orElse(0.0) <= 0) return
        val client = Minecraft.getInstance()
        val type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("simplyswords:dawnquiver_arrow"))
        val native = projectileVisuals[entity] ?: type?.create(entity.level())?.also { projectileVisuals[entity] = it }
        if (native != null && native.javaClass.simpleName == "DawnquiverArrowEntity") {
            native.setPos(entity.x, entity.y, entity.z)
            native.deltaMovement = entity.deltaMovement
            native.tickCount = entity.tickCount
            client.entityRenderDispatcher.render(native, x, y, z, entity.yRot, 0f, pose, buffers, 15728880)
            return
        }
        pose.pushPose(); pose.translate(x, y, z); pose.mulPose(Axis.YP.rotationDegrees(180 - entity.yRot)); pose.mulPose(Axis.XP.rotationDegrees(entity.xRot))
        val v = buffers.getBuffer(RenderType.lightning()); val m = pose.last().pose()
        val a = net.minecraft.world.phys.Vec3(0.0, 0.0, .3); val b = net.minecraft.world.phys.Vec3(0.0, 0.0, -.65)
        segment(v, m, a, b, .11, 255, 186, 37, .35f); segment(v, m, a, b, .025, 255, 255, 226, 1f)
        pose.popPose()
    }
    private fun segment(v: VertexConsumer, m: Matrix4f, a: net.minecraft.world.phys.Vec3, b: net.minecraft.world.phys.Vec3, width: Double, r: Int, g: Int, blue: Int, alpha: Float) {
        val direction = b.subtract(a).normalize()
        var side = direction.cross(net.minecraft.world.phys.Vec3(0.0, 1.0, 0.0))
        if (side.lengthSqr() < .001) side = net.minecraft.world.phys.Vec3(1.0, 0.0, 0.0)
        side = side.normalize().scale(width / 2)
        val other = direction.cross(side).normalize().scale(width / 2)
        for (offset in listOf(side, other)) for (p in listOf(a.add(offset), b.add(offset), b.subtract(offset), a.subtract(offset)))
            v.addVertex(m, p.x.toFloat(), p.y.toFloat(), p.z.toFloat()).setColor(r, g, blue, (alpha.coerceIn(0f, 1f) * 255).toInt())
    }
    private fun line(v: VertexConsumer, m: Matrix4f, x1: Double, y1: Double, x2: Double, y2: Double, width: Double, r: Int, g: Int, b: Int, alpha: Float) {
        val length = hypot(x2 - x1, y2 - y1).coerceAtLeast(.0001)
        val dx = -(y2 - y1) / length * width / 2
        val dy = (x2 - x1) / length * width / 2
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val points = listOf(x1 + dx to y1 + dy, x2 + dx to y2 + dy, x2 - dx to y2 - dy, x1 - dx to y1 - dy)
        for ((x, y) in points)
            v.addVertex(m, x.toFloat(), y.toFloat(), 0f).setColor(r, g, b, a)
    }
}
