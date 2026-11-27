package andromeda.origins.platform

import com.mojang.blaze3d.vertex.PoseStack
import java.util.function.Consumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource

data class WorldFxContext(val pose: PoseStack, val buffers: MultiBufferSource, val delta: Float = 1f)

interface ClientPlatformBackend {
    fun worldFx(callback: Consumer<WorldFxContext>)
    fun startTick(callback: Consumer<Minecraft>)
    fun endTick(callback: Consumer<Minecraft>)
    fun configReceiver()
    fun versionHandshake()
}

object ClientPlatform {
    private lateinit var backend: ClientPlatformBackend
    @JvmStatic fun install(value: ClientPlatformBackend) { check(!::backend.isInitialized); backend = value }
    @JvmStatic fun worldFx(callback: Consumer<WorldFxContext>) = backend.worldFx(callback)
    @JvmStatic fun startTick(callback: Consumer<Minecraft>) = backend.startTick(callback)
    @JvmStatic fun endTick(callback: Consumer<Minecraft>) = backend.endTick(callback)
    @JvmStatic fun configReceiver() = backend.configReceiver()
    @JvmStatic fun versionHandshake() = backend.versionHandshake()
}
