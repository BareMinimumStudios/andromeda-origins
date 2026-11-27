package andromeda.origins.client

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import org.slf4j.LoggerFactory

/** Optional native Afterimages compositing, with no linked classes or global resource-pack overrides. */
object AfterimagesCompat {
    private class Api {
        val type = Class.forName("com.evandev.afterimages.client.TransparencyBufferSource")
        val constructor = type.getConstructor(MultiBufferSource::class.java)
        val alpha = type.getMethod("setAlpha", Float::class.javaPrimitiveType)
        val color = type.getMethod("setColor", Int::class.javaPrimitiveType)
        val current = type.getField("CURRENT_INSTANCE")
    }
    private val api by lazy { try { Api() } catch (_: ReflectiveOperationException) { null } catch (_: LinkageError) { null } }
    private var failed = false
    fun render(entity: LivingEntity, at: Vec3, yaw: Float, pitch: Float, body: Float, head: Float, opacity: Float, delta: Float, pose: PoseStack, buffers: MultiBufferSource): Boolean {
        val bridge = api ?: return false; if (failed) return false
        val c = Minecraft.getInstance(); val camera = c.gameRenderer.mainCamera.position
        val oldYaw = entity.yRot; val oldPitch = entity.xRot; val oldBody = entity.yBodyRot; val oldHead = entity.yHeadRot
        val previous = bridge.current.get(null)
        return try {
            val nativeBuffer = bridge.constructor.newInstance(buffers)
            bridge.alpha.invoke(nativeBuffer, opacity); bridge.color.invoke(nativeBuffer, 0xffd878)
            bridge.current.set(null, nativeBuffer)
            entity.yRot = yaw; entity.xRot = pitch; entity.yBodyRot = body; entity.yHeadRot = head
            c.entityRenderDispatcher.render(entity, at.x - camera.x, at.y - camera.y, at.z - camera.z, yaw, delta, pose, nativeBuffer as MultiBufferSource, c.entityRenderDispatcher.getPackedLightCoords(entity, delta))
            true
        } catch (e: Exception) {
            failed = true
            LoggerFactory.getLogger("andromeda_origins").warn("Optional Afterimages compositing unavailable; using the light-outline fallback.", e)
            false
        } finally {
            bridge.current.set(null, previous)
            entity.yRot = oldYaw; entity.xRot = oldPitch; entity.yBodyRot = oldBody; entity.yHeadRot = oldHead
        }
    }
}
