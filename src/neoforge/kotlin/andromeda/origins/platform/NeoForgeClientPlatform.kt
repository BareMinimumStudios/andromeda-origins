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
    override fun startTick(callback: Consumer<Minecraft>) { NeoForge.EVENT_BUS.addListener(Consumer<ClientTickEvent.Pre> { callback.accept(Minecraft.getInstance()) }) }
    override fun endTick(callback: Consumer<Minecraft>) { NeoForge.EVENT_BUS.addListener(Consumer<ClientTickEvent.Post> { callback.accept(Minecraft.getInstance()) }) }
    override fun configReceiver() { /* Registered on the mod bus by the server bridge. */ }
    override fun versionHandshake() { /* Exact-version payload negotiation is loader-owned. */ }
}
