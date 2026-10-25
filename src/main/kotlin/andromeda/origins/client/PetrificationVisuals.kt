package andromeda.origins.client

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.logging.LogUtils
import dev.overgrown.apoli.power.PowerContainer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.PostChain
import net.minecraft.resources.ResourceLocation

/** Own the world-only pass so pause menus cannot compete for the global shader-power slot. */
object PetrificationVisuals {
    private val marker = ResourceLocation.parse("andromeda_origins:common/petrified_desaturated_vision")
    private val shader = ResourceLocation.withDefaultNamespace("shaders/post/desaturate.json")
    private var chain: PostChain? = null
    private var width = 0
    private var height = 0
    private var failed = false

    private fun active(client: Minecraft): Boolean {
        val player = client.player ?: return false
        if (client.level == null) return false
        val powers = PowerContainer.of(player) ?: return false
        return powers.hasPower(marker) && !powers.isSuppressed(marker)
    }

    fun prepare() {
        if (!active(Minecraft.getInstance())) reset()
    }

    fun render(delta: Float) {
        val client = Minecraft.getInstance()
        if (!active(client) || failed) return
        val target = client.mainRenderTarget
        try {
            var pass = chain
            if (pass == null) {
                pass = PostChain(client.textureManager, client.resourceManager, target, shader)
                chain = pass
            }
            if (target.width != width || target.height != height) {
                width = target.width
                height = target.height
                pass.resize(width, height)
            }
            RenderSystem.disableBlend()
            RenderSystem.disableDepthTest()
            RenderSystem.resetTextureMatrix()
            pass.process(delta)
        } catch (exception: Exception) {
            reset()
            failed = true
            LogUtils.getLogger().warn("Unable to render petrification desaturation", exception)
        } finally {
            target.bindWrite(true)
        }
    }

    fun reset() {
        chain?.close()
        chain = null
        width = 0
        height = 0
        failed = false
    }
}
