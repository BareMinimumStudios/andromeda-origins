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
    fun render(entity: LivingEntity, x: Double, y: Double, z: Double, delta: Float, pose: PoseStack, buffers: MultiBufferSource) {
        val viewer = Minecraft.getInstance().player ?: return
        if (entity.isInvisibleTo(viewer) || viewer.distanceToSqr(entity) > 48.0 * 48.0) return
        val c = PowerContainer.of(entity) ?: return
        if (!c.hasPower(feature) || c.isSuppressed(feature) || PowerResources.read(c, active).orElse(0.0) <= 0 ||
            PowerResources.read(c, visible).orElse(0.0) <= 0) return
        val remaining = PowerResources.read(c, active).orElse(0.0).toFloat()
        val fade = min(1f, min((400f - remaining + delta) / 8f, remaining / 8f)).coerceIn(0f, 1f)
        if (fade <= 0) return
        pose.pushPose()
        pose.translate(x, y + 1.35, z)
        pose.mulPose(Axis.YP.rotationDegrees(-entity.yBodyRot))
        pose.translate(0.0, 0.0, .36)
        val vertices = buffers.getBuffer(RenderType.lightning())
        val matrix = pose.last().pose()
        val age = entity.tickCount + delta
        val pulse = .9f + sin(age * .07f) * .1f
        // Two broken aureoles with warm bodies and pale inner cores.
        for (i in 0..2) {
            val start = age * .003 + i * Math.PI * 2 / 3
            arc(vertices, matrix, 1.18, start, start + 1.55, .065, 255, 184, 43, fade * .32f * pulse)
            arc(vertices, matrix, 1.18, start, start + 1.55, .018, 255, 251, 209, fade * .95f)
            arc(vertices, matrix, 1.34, -start, -start + 1.35, .024, 113, 227, 163, fade * .65f)
        }
        // Swept wing-like light bows, kept behind the wearer rather than over the face.
        for (side in listOf(-1.0, 1.0)) for (feather in 0..3) {
            val px = side * (.42 + feather * .2)
            val py = -.38 + feather * .18
            line(vertices, matrix, px, py, side * (1.45 + feather * .1), .15 + feather * .3,
                .075, 255, 191, 56, fade * .32f)
            line(vertices, matrix, px, py, side * (1.45 + feather * .1), .15 + feather * .3,
                .017, 255, 251, 218, fade * .95f)
        }
        // Radiant crown/sunburst accents around the outer ring.
        for (i in 0..11) {
            val angle = i * Math.PI / 6 + Math.PI / 12
            val r = 1.43
            line(vertices, matrix, cos(angle) * r, sin(angle) * r, cos(angle) * 1.61, sin(angle) * 1.61,
                .025, 255, 222, 133, fade * .8f)
        }
        pose.popPose()
    }
    private val bowLight = ResourceLocation.parse("andromeda_origins:dawnbreaker/bow_radiance")
    fun renderProjectile(entity: net.minecraft.world.entity.Entity, x: Double, y: Double, z: Double, pose: PoseStack, buffers: MultiBufferSource) {
        if (entity !is net.minecraft.world.entity.projectile.AbstractArrow || entity.customName?.string != "Dawnbreaker Light Arrow") return
        val owner = entity.owner as? LivingEntity ?: return
        val c = PowerContainer.of(owner) ?: return
        if (PowerResources.read(c, bowLight).orElse(0.0) <= 0) return
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
    private fun arc(v: VertexConsumer, m: Matrix4f, radius: Double, from: Double, to: Double, width: Double, r: Int, g: Int, b: Int, a: Float) {
        for (i in 0 until 24) {
            val p = from + (to - from) * i / 24
            val q = from + (to - from) * (i + 1) / 24
            line(v, m, cos(p) * radius, sin(p) * radius, cos(q) * radius, sin(q) * radius, width, r, g, b, a)
        }
    }
    private fun line(v: VertexConsumer, m: Matrix4f, x1: Double, y1: Double, x2: Double, y2: Double, width: Double, r: Int, g: Int, b: Int, alpha: Float) {
        val length = hypot(x2 - x1, y2 - y1).coerceAtLeast(.0001)
        val dx = -(y2 - y1) / length * width / 2
        val dy = (x2 - x1) / length * width / 2
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        v.addVertex(m, (x1 + dx).toFloat(), (y1 + dy).toFloat(), 0f).setColor(r, g, b, a)
        v.addVertex(m, (x2 + dx).toFloat(), (y2 + dy).toFloat(), 0f).setColor(r, g, b, a)
        v.addVertex(m, (x2 - dx).toFloat(), (y2 - dy).toFloat(), 0f).setColor(r, g, b, a)
        v.addVertex(m, (x1 - dx).toFloat(), (y1 - dy).toFloat(), 0f).setColor(r, g, b, a)
    }
}
