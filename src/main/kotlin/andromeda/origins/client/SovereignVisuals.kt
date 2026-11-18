package andromeda.origins.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import kotlin.math.*

/** Thin, double-sided light geometry. No full-screen flashes, textures or dependence on an avatar model. */
object SovereignVisuals {
    private fun id(s: String) = ResourceLocation.parse("andromeda_origins:dawnbreaker/$s")
    private fun read(e: LivingEntity, path: String) = PowerContainer.of(e)?.let { PowerResources.read(it, id(path)).orElse(0.0) } ?: 0.0
    fun render(e: LivingEntity, x: Double, y: Double, z: Double, delta: Float, pose: PoseStack, buffers: MultiBufferSource) {
        val c = PowerContainer.of(e) ?: return
        val guard = read(e, "stillwater_active") > 0 && read(e, "stillwater_radiance") > 0
        val stone = c.hasPower(id("helper/stone")) && read(e, "helper/stone_gradual") > 0
        val kneeling = c.hasPower(id("helper/kneeling")) && read(e, "helper/kneeling_radiance") > 0
        if (!guard && !stone && !kneeling) return
        pose.pushPose(); pose.translate(x, y, z)
        val v = buffers.getBuffer(RenderType.debugQuads())
        if (guard) {
            pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(-net.minecraft.util.Mth.rotLerp(delta, e.yBodyRotO, e.yBodyRot)))
            val m = pose.last().pose(); val count = read(e, "stillwater_count").toInt()
            // Eight distinct solar lenses trace a crown behind the shoulders, leaving the face clear.
            for (j in 0 until 64) {
                val a = Math.toRadians(-54.0 + j * 108.0 / 64)
                val b = Math.toRadians(-54.0 + (j + 1) * 108.0 / 64)
                if (j % 8 < 6) beam(v, m, Vec3(sin(a) * 2.2, 1.15 + cos(a) * .65, -1.15), Vec3(sin(b) * 2.2, 1.15 + cos(b) * .65, -1.15), .018, .35f)
            }
            for (i in 0 until count) {
                val a = Math.toRadians((i - 3.5) * 13.0)
                val centre = Vec3(sin(a) * 2.2, 1.15 + cos(a) * .65, -1.15)
                val lit = true
                for (j in 0 until 24) {
                    val t = j * PI / 12; val u = (j + 1) * PI / 12
                    beam(v, m, centre.add(cos(t) * .18, sin(t) * .18, 0.0), centre.add(cos(u) * .18, sin(u) * .18, 0.0), .012, if (lit) .9f else .2f)
                }
                if (lit) for (axis in listOf(Vec3(.32, 0.0, 0.0), Vec3(0.0, .32, 0.0))) beam(v, m, centre.subtract(axis), centre.add(axis), .008, .8f)
            }
            pose.popPose()
        }
        val m = pose.last().pose()
        if (kneeling) {
            val lease = read(e, "helper/kneeling_lease")
            val radius = .55 + (60 - lease).coerceIn(0.0, 10.0) * .06
            for (i in 0 until 48) {
                val a = i * PI / 24; val b = (i + 1) * PI / 24
                beam(v, m, Vec3(cos(a) * radius, .035, sin(a) * radius), Vec3(cos(b) * radius, .035, sin(b) * radius), .018, (lease / 12).coerceIn(0.0, .7).toFloat())
            }
        }
        if (stone) {
            val progress = ((read(e, "helper/stone_progress") + delta) / 40).coerceIn(0.0, 1.0)
            val height = e.bbHeight * progress; val w = (e.bbWidth * .52).toDouble()
            // Three rising mineral bands, with branching gold seams instead of overlapping body quads.
            for (band in 0..2) {
                val h = height - band * .09
                if (h <= 0) continue
                for (side in 0..3) {
                    val a = side * PI / 2 + PI / 4; val b = a + PI / 2
                    beam(v, m, Vec3(cos(a) * w, h, sin(a) * w), Vec3(cos(b) * w, h, sin(b) * w), .015 - band * .003, if (band == 0) .8f else .26f)
                }
            }
            for (i in 0..7) {
                val a = i * PI / 4; val foot = Vec3(cos(a) * w, .06, sin(a) * w)
                val mid = Vec3(cos(a + .13) * w, height * .48, sin(a + .13) * w)
                val top = Vec3(cos(a - .1) * w, height, sin(a - .1) * w)
                beam(v, m, foot, mid, .009, .48f); beam(v, m, mid, top, .009, .48f)
            }
        }
        pose.popPose()
    }
    private fun beam(v: VertexConsumer, m: Matrix4f, a: Vec3, b: Vec3, width: Double, alpha: Float) {
        val d = b.subtract(a); if (d.lengthSqr() < .000001) return
        var side = d.cross(Vec3(0.0, 1.0, 0.0)); if (side.lengthSqr() < .000001) side = Vec3(1.0, 0.0, 0.0)
        side = side.normalize().scale(width / 2)
        val other = d.normalize().cross(side).normalize().scale(width / 2)
        for (o in listOf(side, other)) {
            val points = listOf(a.add(o), b.add(o), b.subtract(o), a.subtract(o))
            for (p in points) v.addVertex(m, p.x.toFloat(), p.y.toFloat(), p.z.toFloat()).setColor(255, 172, 42, (alpha.coerceIn(0f, 1f) * 255).toInt())
        }
    }
}
