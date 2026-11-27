package andromeda.origins.platform

import andromeda.origins.client.AndromedaOriginsClient
import andromeda.origins.client.DawnbreakerAudio
import andromeda.origins.client.DawnbreakerFxClient
import andromeda.origins.client.OriginFxRenderer
import andromeda.origins.config.AndromedaGameplayConfig
import andromeda.origins.network.BanishmentHeartbeatPayload
import andromeda.origins.network.DawnbreakerFxPayload
import andromeda.origins.network.OpenConfigPayload
import andromeda.origins.network.OriginFxPayload
import java.util.function.Consumer
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.Minecraft

class FabricClientEntrypoint : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlatform.install(FabricClientPlatform())
        AndromedaOriginsClient.initialize()
    }
}
class FabricClientPlatform : ClientPlatformBackend {
    override fun worldFx(callback: Consumer<WorldFxContext>) = WorldRenderEvents.AFTER_ENTITIES.register { context ->
        val pose = context.matrixStack(); val buffers = context.consumers()
        if (pose != null && buffers != null) callback.accept(WorldFxContext(pose, buffers, context.tickCounter().getGameTimeDeltaPartialTick(false)))
    }
    override fun startTick(callback: Consumer<Minecraft>) = ClientTickEvents.START_CLIENT_TICK.register { callback.accept(it) }
    override fun endTick(callback: Consumer<Minecraft>) = ClientTickEvents.END_CLIENT_TICK.register { callback.accept(it) }
    override fun configReceiver() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(ClientCommandManager.literal("andromedaorigins_visuals").executes { DawnbreakerFxClient.openConfig(); 1 })
        }
        ClientPlayNetworking.registerGlobalReceiver(DawnbreakerFxPayload.TYPE) { payload, context ->
            context.client().execute { DawnbreakerFxClient.receive(payload) }
        }
        ClientPlayNetworking.registerGlobalReceiver(OriginFxPayload.TYPE) { payload, context ->
            context.client().execute { OriginFxRenderer.receive(payload) }
        }
        ClientPlayNetworking.registerGlobalReceiver(OpenConfigPayload.TYPE) { _, context ->
            context.client().execute { ConfigApiJava.openScreen(AndromedaGameplayConfig.SCREEN_SCOPE) }
        }
        ClientPlayNetworking.registerGlobalReceiver(BanishmentHeartbeatPayload.TYPE) { _, context ->
            context.client().execute { DawnbreakerAudio.playBanishmentHeartbeat() }
        }
    }
    override fun versionHandshake() = FabricVersionHandshakeClient.register()
}
