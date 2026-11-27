package andromeda.origins.client

import andromeda.origins.compat.DawnbreakerPresentation
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import java.util.WeakHashMap
import kotlin.math.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.AbstractArrow
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f

/** Independent, texture-free radiant geometry inspired by Dawnquiver's layered light presentation. */
object DawnbreakerVisuals {
    private val active = ResourceLocation.parse("andromeda_origins:dawnbreaker/presence_active")
    private val visible = ResourceLocation.parse("andromeda_origins:dawnbreaker/presence_radiance")
    private val feature = ResourceLocation.parse("andromeda_origins:dawnbreaker/radiance")
    private val appearing = WeakHashMap<LivingEntity, Float>()
    fun render(entity: LivingEntity, x: Double, y: Double, z: Double, delta: Float, pose: PoseStack, buffers: MultiBufferSource) {
        val viewer = Minecraft.getInstance().player ?: return
        if (entity.isInvisibleTo(viewer) || viewer.distanceToSqr(entity) > 48.0 * 48.0) return
        val c = PowerContainer.of(entity) ?: return
        val nativeGhost = DawnbreakerFxClient.hasGhost(entity)
        SovereignVisuals.render(entity, x, y, z, delta, pose, buffers)
        fun resource(path: String) = PowerResources.read(c, ResourceLocation.parse("andromeda_origins:dawnbreaker/" + path)).orElse(0.0)
        val stepping = (resource("presentation_step").toFloat() / 2f).coerceIn(0f, 1f)
        val shot = resource("presentation_shot").toFloat() / 12f
        val drawing = resource("bow_draw").toFloat() / 60f
        val presence = PowerResources.read(c, active).orElse(0.0)
        val radiance = PowerResources.read(c, visible).orElse(0.0) > 0 || resource("bow_radiance") > 0
        if (!c.hasPower(feature) || c.isSuppressed(feature) || !radiance ||
            (presence <= 0 && stepping <= 0 && shot <= 0 && drawing <= 0)) { appearing.remove(entity); return }
        val age = entity.tickCount + delta
        val started = appearing.getOrPut(entity) { age }
        val remaining = max(presence.toFloat(), max(stepping * 20, max(shot * 20, drawing * 20)))
        val fade = min(1f, min((age - started) / 10f, remaining / 12f)).coerceIn(0f, 1f)
        if (fade <= 0) return
        pose.pushPose()
        pose.translate(x, y, z)
        val vertices = buffers.getBuffer(RenderType.debugQuads())
        if (!nativeGhost && stepping > 0 && resource("presentation_animation") > 0 && DawnbreakerPresentation.settings().animatedWings.get()) {
            pose.pushPose()
            pose.translate(resource("presentation_step_x") - entity.x, resource("presentation_step_y") - entity.y, resource("presentation_step_z") - entity.z)
            pose.mulPose(Axis.YP.rotationDegrees(-entity.yRot))
            val ghostScale = (entity.bbHeight / 1.8f).coerceIn(.5f, 2f); pose.scale(ghostScale, ghostScale, ghostScale)
            val ghost = pose.last().pose()
            for (side in listOf(-1.0, 1.0)) line(vertices, ghost, side * .2, .1, side * .36, 1.7, .035, 255, 218, 135, stepping * .45f)
            line(vertices, ghost, -.36, 1.7, .36, 1.7, .024, 255, 218, 135, stepping * .45f)
            for (i in 0 until 16) {
                val a = i * Math.PI / 8; val b = (i + 1) * Math.PI / 8
                line(vertices, ghost, cos(a) * .13, 1.85 + sin(a) * .13, cos(b) * .13, 1.85 + sin(b) * .13,
                    .018, 255, 218, 135, stepping * .4f)
            }
            pose.popPose()
        }
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(delta, entity.yBodyRotO, entity.yBodyRot)))
        val scale = (entity.bbHeight / 1.8f).coerceIn(.65f, 1.6f)
        pose.scale(scale, scale, scale)
        pose.translate(0.0, 0.0, -.70)
        val matrix = pose.last().pose()
        val breath = .94f + sin(age * .07f) * .08f
        // A broad amber annulus with separate ivory/gold edges; no additive washout in daylight.
        halo(vertices, matrix, .72, .812, 0.0, Math.PI * 2, 224, 136, 22, fade * .25f)
        halo(vertices, matrix, .816, .834, 0.0, Math.PI * 2, 255, 199, 70, fade * .88f * breath)
        halo(vertices, matrix, .704, .716, 0.0, Math.PI * 2, 255, 239, 180, fade * .75f)
        val phase = age * .018
        for (i in 0..2) {
            val start = phase + i * Math.PI * 2 / 3
            halo(vertices, matrix, .91, .928, start, start + 1.15, 255, 174, 45, fade * .55f)
        }
        val animate = resource("presentation_animation") > 0 && DawnbreakerPresentation.settings().animatedWings.get()
        val fold = if (animate) stepping * stepping * (3 - 2 * stepping) else 0f
        val flare = if (animate) sin(shot * Math.PI).toFloat() * .22f + drawing * .10f else 0f
        val featherCount = when (DawnbreakerPresentation.tier(entity)) { 0 -> 3; 2 -> 8; else -> 6 }
        // Tapered feathers breathe, fold on Step and flare on bow release. Their roots stay behind the body.
        for (side in listOf(-1.0, 1.0)) {
            pose.pushPose(); pose.translate(0.0, 0.0, .16)
            pose.mulPose(Axis.YP.rotationDegrees((side * (16 + fold * 58)).toFloat()))
            pose.scale(1f - fold * .55f + flare, 1f + flare * .18f, 1f)
            for (index in 0 until featherCount) {
                val f = index * 5.0 / (featherCount - 1)
                pose.pushPose(); pose.translate(0.0, 0.0, -f * .018)
                val wing = pose.last().pose()
                val sweep = if (animate) sin(age * .07 + f * .35) * .10 else 0.0
                val rootX = side * (.23 + f * .05)
                val rootY = .92 + f * .14
                val tipX = side * (2.45 - f * .12)
                val tipY = .85 + f * .40 + sweep
                val midX = rootX + (tipX - rootX) * .58
                val midY = rootY + (tipY - rootY) * .58
                val width = .13 + f * .012
                // Partition the feather into disjoint fill bands and a bright centre.
                // Rotating each concave band gives QUADS an interior triangulation diagonal.
                // No brighter quad lies on top of the translucent feather plane.
                val root = rootX to rootY; val tip = tipX to tipY
                val lower = midX to midY - .0095; val upper = midX to midY + .0095
                val fillAlpha = fade * (.40f + f * .035f).toFloat()
                quad(vertices, wing, listOf(midX to midY - width, tip, lower, root), 255, 179, 43, fillAlpha)
                quad(vertices, wing, listOf(upper, tip, midX to midY + width, root), 255, 179, 43, fillAlpha)
                quad(vertices, wing, listOf(root, lower, tip, upper), 255, 242, 190, fade * .92f)
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
    private val projectileVisuals = WeakHashMap<Entity, Entity>()
    fun renderProjectile(entity: Entity, x: Double, y: Double, z: Double, pose: PoseStack, buffers: MultiBufferSource) {
        if (entity !is AbstractArrow || entity.customName?.string != "Dawnbreaker Light Arrow") return
        val owner = entity.owner as? LivingEntity ?: return
        val c = PowerContainer.of(owner) ?: return
        if (!c.hasPower(feature) || c.isSuppressed(feature) || PowerResources.read(c, ResourceLocation.parse("andromeda_origins:dawnbreaker/bow_radiance")).orElse(0.0) <= 0) return
        val client = Minecraft.getInstance()
        val type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("simplyswords:dawnquiver_arrow"))
        val distance = sqrt(x * x + y * y + z * z)
        // Let the ordinary shaft remain visible at point-blank range without a screen-filling light mesh.
        if (distance < 1.2) return
        val native = projectileVisuals[entity] ?: type?.create(entity.level())?.also { projectileVisuals[entity] = it }
        if (native != null && native.javaClass.simpleName == "DawnquiverArrowEntity") {
            native.setPos(entity.x, entity.y, entity.z)
            native.deltaMovement = entity.deltaMovement
            native.tickCount = entity.tickCount
            pose.pushPose(); pose.translate(x, y, z)
            val size = (distance / 8.0).coerceIn(.18, 1.0).toFloat(); pose.scale(size, size, size)
            client.entityRenderDispatcher.render(native, 0.0, 0.0, 0.0, entity.yRot, 0f, pose, buffers, 15728880)
            pose.popPose()
            return
        }
        pose.pushPose(); pose.translate(x, y, z); pose.mulPose(Axis.YP.rotationDegrees(180 - entity.yRot)); pose.mulPose(Axis.XP.rotationDegrees(entity.xRot))
        val v = buffers.getBuffer(RenderType.lightning()); val m = pose.last().pose()
        val a = Vec3(0.0, 0.0, .3); val b = Vec3(0.0, 0.0, -.65)
        segment(v, m, a, b, .11, 255, 186, 37, .35f); segment(v, m, a, b, .025, 255, 255, 226, 1f)
        pose.popPose()
    }
    private fun segment(v: VertexConsumer, m: Matrix4f, a: Vec3, b: Vec3, width: Double, r: Int, g: Int, blue: Int, alpha: Float) {
        val direction = b.subtract(a).normalize()
        var side = direction.cross(Vec3(0.0, 1.0, 0.0))
        if (side.lengthSqr() < .001) side = Vec3(1.0, 0.0, 0.0)
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
