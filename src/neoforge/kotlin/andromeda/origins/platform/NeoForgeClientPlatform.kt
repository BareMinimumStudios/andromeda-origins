package andromeda.origins.platform

import andromeda.origins.client.AndromedaOriginsClient
import andromeda.origins.config.AndromedaGameplayConfig
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava
import net.minecraft.client.Minecraft
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.client.event.ClientTickEvent
import java.util.function.Consumer

class NeoForgeClientPlatform : ClientPlatformBackend {
    companion object {
        fun install(bus: IEventBus) {
            bus.addListener(Consumer<FMLClientSetupEvent> { event ->
                event.enqueueWork {
                    ClientPlatform.install(NeoForgeClientPlatform())
                    AndromedaOriginsClient.initialize()
                }
            })
        }
        fun openConfig() { ConfigApiJava.openScreen(AndromedaGameplayConfig.SCREEN_SCOPE) }
    }
    override fun worldFx(callback: Consumer<WorldFxContext>) { NeoForge.EVENT_BUS.addListener(Consumer<net.neoforged.neoforge.client.event.RenderLevelStageEvent> { event ->
        if (event.stage == net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES) callback.accept(WorldFxContext(event.poseStack ?: com.mojang.blaze3d.vertex.PoseStack().also { it.last().pose().set(event.modelViewMatrix) }, Minecraft.getInstance().renderBuffers().bufferSource(), event.partialTick.getGameTimeDeltaPartialTick(false)))
    }) }
    override fun startTick(callback: Consumer<Minecraft>) { NeoForge.EVENT_BUS.addListener(Consumer<ClientTickEvent.Pre> { callback.accept(Minecraft.getInstance()) }) }
    override fun endTick(callback: Consumer<Minecraft>) { NeoForge.EVENT_BUS.addListener(Consumer<ClientTickEvent.Post> { callback.accept(Minecraft.getInstance()) }) }
    override fun configReceiver() {
        NeoForge.EVENT_BUS.addListener(Consumer<net.neoforged.neoforge.client.event.RegisterClientCommandsEvent> { event ->
            event.dispatcher.register(net.minecraft.commands.Commands.literal("andromedaorigins_visuals").executes { andromeda.origins.client.DawnbreakerFxClient.openConfig(); 1 })
        })
    }
    override fun versionHandshake() { /* Exact-version payload negotiation is loader-owned. */ }
}
