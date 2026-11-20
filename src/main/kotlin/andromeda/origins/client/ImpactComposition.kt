package andromeda.origins.client

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import org.joml.Matrix4f
import kotlin.math.*

/** Original filled compositions, not radial speed lines or borrowed sprites. */
object ImpactComposition {
    fun render(g: GuiGraphics, kind: Int, progress: Float, intensity: Float) {
        val fade = (1f - progress).coerceIn(0f, 1f).pow(.65f)
        val a = (255 * intensity * fade).toInt().coerceIn(0, 235)
        if (a <= 0) return
        val w = g.guiWidth().toFloat(); val h = g.guiHeight().toFloat()
        g.fill(0, 0, w.toInt(), h.toInt(), (a shl 24) or 0x080607)
        val p = g.pose(); p.pushPose(); p.translate(w * .5, h * .49, 0.0)
        val m = p.last().pose(); val v = g.bufferSource().getBuffer(RenderType.gui())
        val radius = min(w, h) * (.42f + .035f * progress)
        val detail = when (andromeda.origins.compat.OriginFxStyle.quality()) {
            andromeda.origins.compat.OriginFxStyle.Quality.MINIMAL -> 48
            andromeda.origins.compat.OriginFxStyle.Quality.STANDARD -> 64
            andromeda.origins.compat.OriginFxStyle.Quality.CINEMATIC -> 96
        }
        when (kind) {
            0 -> { // Instant Step: a swept eclipse, opening into negative space.
                crescent(v, m, radius, -.08f * radius, 0f, -.9, 4.65, .24f, a, detail)
                crescent(v, m, radius * .79f, .13f * radius, -.02f * radius, 2.2, 3.95, .12f, (a * .75f).toInt(), detail)
            }
            1, 2 -> { // Guard: opposing pressure crescents; the release opens outward.
                val open = if (kind == 2) .12f + progress * .12f else .04f
                crescent(v, m, radius, -radius * open, 0f, 1.7, 4.58, .21f, a, detail)
                crescent(v, m, radius, radius * open, 0f, -1.44, 1.44, .21f, a, detail)
                star(v, m, radius * .24f, (a * .65f).toInt(), detail, false)
            }
            else -> { // Bow: one broad, asymmetric four-lobed rupture with a black aperture.
                star(v, m, radius * 1.2f, a, detail, false)
                star(v, m, radius * .72f, a, detail, true)
                crescent(v, m, radius * 1.02f, 0f, 0f, .35, 2.15, .1f, (a * .65f).toInt(), detail)
            }
        }
        p.popPose()
        g.flush()
    }
    private fun vertex(v: VertexConsumer, m: Matrix4f, x: Float, y: Float, a: Int, light: Boolean, black: Boolean = false) {
        val rgb = if (black) 0x080607 else if (light) 0xffe29a else 0xc18427
        v.addVertex(m, x, y, 0f).setColor((rgb shr 16) and 255, (rgb shr 8) and 255, rgb and 255, a)
    }
    private fun crescent(v: VertexConsumer, m: Matrix4f, r: Float, x: Float, y: Float, start: Double, end: Double, thickness: Float, a: Int, n: Int) {
        for (i in 0 until n) {
            fun point(k: Int, inner: Boolean): Pair<Float, Float> {
                val t = k.toDouble() / n; val angle = start + (end - start) * t
                val edge = sin(t * PI).coerceAtLeast(0.0).pow(.65).toFloat()
                val radius = r * (if (inner) 1f - thickness * edge else 1f)
                return Pair(x + cos(angle).toFloat() * radius, y + sin(angle).toFloat() * radius * .87f)
            }
            val p0 = point(i, false); val p1 = point(i + 1, false); val p2 = point(i + 1, true); val p3 = point(i, true)
            vertex(v,m,p0.first,p0.second,a,false);vertex(v,m,p3.first,p3.second,a,true)
            vertex(v,m,p2.first,p2.second,a,true);vertex(v,m,p1.first,p1.second,a,false)
        }
    }
    private fun star(v: VertexConsumer, m: Matrix4f, r: Float, a: Int, n: Int, black: Boolean) {
        for (i in 0 until n) {
            fun point(k: Int): Pair<Float,Float> {
                val t = k * 2.0 * PI / n + .23
                val radius = r * (.28f + .72f * abs(cos(t * 2)).pow(5).toFloat())
                return Pair(cos(t).toFloat() * radius * 1.25f, sin(t).toFloat() * radius)
            }
            val p0=point(i);val p1=point(i+1)
            vertex(v,m,0f,0f,a,true,black);vertex(v,m,p1.first,p1.second,a,false,black)
            vertex(v,m,p0.first,p0.second,a,false,black);vertex(v,m,0f,0f,a,true,black)
        }
    }
}
