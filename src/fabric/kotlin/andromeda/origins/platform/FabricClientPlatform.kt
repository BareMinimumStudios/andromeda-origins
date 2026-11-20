package andromeda.origins.platform

import andromeda.origins.client.AndromedaOriginsClient
import andromeda.origins.config.AndromedaGameplayConfig
import andromeda.origins.network.OpenConfigPayload
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import java.util.function.Consumer

class FabricClientEntrypoint : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlatform.install(FabricClientPlatform())
        AndromedaOriginsClient.initialize()
    }
}
class FabricClientPlatform : ClientPlatformBackend {
    override fun startTick(callback: Consumer<Minecraft>) = ClientTickEvents.START_CLIENT_TICK.register { callback.accept(it) }
    override fun endTick(callback: Consumer<Minecraft>) = ClientTickEvents.END_CLIENT_TICK.register { callback.accept(it) }
    override fun configReceiver() {
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("andromedaorigins_visuals").executes { andromeda.origins.client.DawnbreakerFxClient.openConfig(); 1 })
        }
        ClientPlayNetworking.registerGlobalReceiver(andromeda.origins.network.DawnbreakerFxPayload.TYPE) { payload, context ->
            context.client().execute { andromeda.origins.client.DawnbreakerFxClient.receive(payload) }
        }
        ClientPlayNetworking.registerGlobalReceiver(OpenConfigPayload.TYPE) { _, context ->
            context.client().execute { ConfigApiJava.openScreen(AndromedaGameplayConfig.SCREEN_SCOPE) }
        }
        ClientPlayNetworking.registerGlobalReceiver(andromeda.origins.network.BanishmentHeartbeatPayload.TYPE) { _, context ->
            context.client().execute { andromeda.origins.client.DawnbreakerAudio.playBanishmentHeartbeat() }
        }
    }
    override fun versionHandshake() = FabricVersionHandshakeClient.register()
}
