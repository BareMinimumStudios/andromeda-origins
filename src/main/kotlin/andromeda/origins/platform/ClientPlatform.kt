package andromeda.origins.platform

import net.minecraft.client.Minecraft
import java.util.function.Consumer

interface ClientPlatformBackend {
    fun startTick(callback: Consumer<Minecraft>)
    fun endTick(callback: Consumer<Minecraft>)
    fun configReceiver()
    fun versionHandshake()
}

object ClientPlatform {
    private lateinit var backend: ClientPlatformBackend
    @JvmStatic fun install(value: ClientPlatformBackend) { check(!::backend.isInitialized); backend = value }
    @JvmStatic fun startTick(callback: Consumer<Minecraft>) = backend.startTick(callback)
    @JvmStatic fun endTick(callback: Consumer<Minecraft>) = backend.endTick(callback)
    @JvmStatic fun configReceiver() = backend.configReceiver()
    @JvmStatic fun versionHandshake() = backend.versionHandshake()
}
