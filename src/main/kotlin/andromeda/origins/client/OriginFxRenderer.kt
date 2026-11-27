package andromeda.origins.client

import andromeda.origins.network.OriginFxPayload
import andromeda.origins.platform.ClientPlatform
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import kotlin.math.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import org.joml.Matrix4f

/** Small finite ribbon compositions. Never creates billboard clouds or world entities. */
object OriginFxRenderer {
    private data class Effect(val packet: OriginFxPayload, val started: Long, val duration: Long, val yaw: Float, val scale: Float)
    private val effects = mutableListOf<Effect>()
    private var world: Any? = null
    fun register() {
        ClientPlatform.endTick { c ->
            if (!DawnbreakerFxClient.config.originContours.get()) effects.clear()
            if (world !== c.level) { effects.clear(); world = c.level }
            val now = System.nanoTime(); effects.removeIf { now - it.started >= it.duration }
        }
        ClientPlatform.worldFx { context ->
            val c = Minecraft.getInstance(); val level = c.level ?: return@worldFx; val player = c.player ?: return@worldFx
            val camera = c.gameRenderer.mainCamera.position; val now = System.nanoTime()
            for (fx in effects.toList()) {
                val actor = level.getEntity(fx.packet.actor)
                val attached = fx.packet.mode and 4 != 0
                if (attached && (actor == null || !actor.isAlive)) continue
                val at = if (attached) actor!!.getPosition(context.delta) else fx.packet.at
                if (actor?.isInvisibleTo(player) == true || camera.distanceToSqr(at) > 32.0 * 32.0) continue
                val t = ((now - fx.started).toDouble() / fx.duration).toFloat(); if (t !in 0f..1f) continue
                context.pose.pushPose(); context.pose.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z)
                val scale = if (attached && actor is LivingEntity) (actor.bbHeight / 1.8f).coerceIn(.35f,1.8f) else fx.scale
                val yaw = if (attached) Mth.rotLerp(context.delta,actor!!.yRotO,actor.yRot) else fx.yaw
                context.pose.mulPose(Axis.YP.rotationDegrees(-yaw))
                context.pose.scale(scale,scale,scale)
                val mode = fx.packet.mode and 3
                val sweep = if (mode == 1) t * 55f else t * 105f
                context.pose.mulPose(Axis.YP.rotationDegrees(sweep))
                val expansion = if (mode == 2) .45f + t * .85f else .72f + .3f * sin(t * PI.toFloat() / 2)
                context.pose.scale(expansion, 1f, expansion)
                context.pose.translate(0.0, (if (mode == 1) sin(t * PI) * .08 else t * .18).toDouble(), 0.0)
                draw(fx.packet,t,context.pose.last().pose(),context.buffers.getBuffer(RenderType.debugQuads()))
                context.pose.popPose()
            }
            (context.buffers as? MultiBufferSource.BufferSource)?.endBatch(RenderType.debugQuads())
        }
    }
    fun receive(p: OriginFxPayload) {
        if (!DawnbreakerFxClient.config.originContours.get()) return
        val c = Minecraft.getInstance(); val player = c.player ?: return; val level = c.level ?: return
        if (p.family !in 0..12 || (p.mode and 3) !in 0..2 || p.mode !in 0..6 || p.detail !in 0..2 || !p.radius.isFinite() || p.radius !in .35f..1.25f || !p.at.x.isFinite() || !p.at.y.isFinite() || !p.at.z.isFinite() || player.distanceToSqr(p.at)>40.0*40.0) return
        val actor = level.getEntity(p.actor)
        if (p.mode and 4 != 0 && actor == null) return
        if (actor?.isInvisibleTo(player) == true) return
        // At most two complementary moments per caster and 24 compositions per view.
        effects.removeIf { it.packet.actor==p.actor && it.packet.family==p.family && it.packet.mode==p.mode }
        while (effects.count { it.packet.actor==p.actor }>=2) effects.removeAt(effects.indexOfFirst { it.packet.actor==p.actor })
        if (effects.size>=24) effects.removeAt(0)
        effects.add(Effect(p,System.nanoTime(),if(p.mode and 3==1) 450_000_000L else if(p.mode and 3==2) 550_000_000L else 700_000_000L,actor?.yRot ?: 0f,if(actor is LivingEntity) (actor.bbHeight / 1.8f).coerceIn(.35f,1.8f) else 1f))
    }
    private fun draw(p: OriginFxPayload, t: Float, m: Matrix4f, v: VertexConsumer) {
        val fade = min(1f,t/.08f) * (1f-t).pow(1.4f); val alpha = (.7f*fade).coerceIn(0f,.7f)
        val detail = min(p.detail,DawnbreakerFxClient.config.contourDetail.get().ordinal)
        val r = p.radius.coerceIn(.35f,1.1f);val n=when(detail){0->20;1->32;else->48}
        fun ribbon(width: Float=.021f, curve: (Double)->DoubleArray) {
            for(i in 0 until n){fun flowing(u:Double):DoubleArray {
                    val q=curve(u); val phase=u*PI*2-t*PI*2
                    q[0]+=sin(phase)*.025; q[1]+=sin(phase+PI/2)*.035; q[2]+=cos(phase)*.025
                    return q
                }
                val a=flowing(i.toDouble()/n);val b=flowing((i+1.0)/n)
                val dx=b[0]-a[0];val dz=b[2]-a[2];val len=sqrt(dx*dx+dz*dz).coerceAtLeast(.0001)
                val nx=-dz/len*width;val nz=dx/len*width
                fun point(q:DoubleArray,s:Double,opacity:Float){v.addVertex(m,(q[0]+nx*s).toFloat(),q[1].toFloat(),(q[2]+nz*s).toFloat()).setColor((p.color shr 16)and 255,(p.color shr 8)and 255,p.color and 255,(255*opacity).toInt())}
                val taper=sin((i+.5)*PI/n).toFloat().pow(.5f)
                point(a,-1.0,alpha*taper*.15f);point(b,-1.0,alpha*taper*.15f);point(b,1.0,alpha*taper);point(a,1.0,alpha*taper)
            }
        }
        fun arc(radius:Double,y:Double,start:Double,length:Double)=ribbon { u ->val a=start+u*length+t*.45;doubleArrayOf(cos(a)*radius,y+sin(u*PI)*.025,sin(a)*radius) }
        val count=if(detail==0)1 else 2
        when(p.family){
            0->{for(k in 0 until count)arc(r*(.65+k*.16),.12+k*.12,k*PI,PI*1.7)}
            1->{for(side in listOf(-1,1).take(if(p.detail==0)1 else 2))ribbon(.017f){u->doubleArrayOf(side*sin(u*PI)*r*.7,.35+u*1.1,cos(u*PI)*r*.3)}}
            2->{for(k in 0 until count)ribbon(.023f){u->doubleArrayOf((u-.5)*r*1.4,.15+sin(u*PI)*.28,k*.16+sin(u*PI)*r*.35)}}
            3->{arc(r*.8,.13,t*.2,PI*1.75);if(count>1)ribbon{u->doubleArrayOf(sin(u*PI)*r*.5,.2+u*1.1,cos(u*PI)*r*.4)}}
            4,5->{for(k in 0 until count)ribbon(.02f){u->val a=u*PI*1.5+k*PI+t*.6;doubleArrayOf(cos(a)*r*.45,.12+u*1.35,sin(a)*r*.45)}}
            6->{for(k in 0 until count)ribbon(.023f){u->doubleArrayOf((u-.5)*r*1.5,.25+u*.75,sin(u*PI*2)*r*.18+k*.12)}}
            7->{for(k in 0 until count)arc(r*(.55+k*.18),.12+k*.17,k*PI+t*.3,PI*1.8)}
            8->{for(k in 0 until count)ribbon(.025f){u->val a=u*PI*1.5+k*PI;doubleArrayOf(cos(a)*r*.6,.08+sin(u*PI)*.25,sin(a)*r*.6)}}
            9->{for(k in 0 until count)ribbon(.021f){u->val a=u*PI*1.8+k*PI;doubleArrayOf(cos(a)*r*.55,.2+sin(u*PI)*1.2,sin(a)*r*.55)}}
            10->{for(k in 0 until count)arc(r*(.4+.5*t+k*.14),.15+k*.05,k*.4,PI*1.8)}
            11->{for(k in 0 until count)ribbon(.025f){u->val a=u*PI*1.6+k*PI;doubleArrayOf(cos(a)*r*.5,.1+u*1.3,sin(a)*r*.35)}}
            12->{for(k in 0 until count)ribbon(.021f){u->doubleArrayOf(sin(u*PI)*r*(if(k==0)1 else -1)*.4,.15+u*.8,(u-.5)*r*1.4)}}
        }
    }
}
