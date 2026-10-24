package andromeda.origins.platform

import andromeda.origins.AndromedaOrigins
import andromeda.origins.network.OpenConfigPayload
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import java.util.function.Consumer
import java.util.function.BiConsumer

class FabricEntrypoint : ModInitializer {
    override fun onInitialize() {
        Platform.install(FabricPlatform())
        AndromedaOrigins.initialize()
        andromeda.origins.compat.EntityUnloadDiagnostics.register()
    }
}

class FabricPlatform : PlatformBackend {
    override fun registerItem(id: ResourceLocation) {
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM, id, net.minecraft.world.item.Item(net.minecraft.world.item.Item.Properties()))
    }
    override fun registerSound(id: ResourceLocation) {
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT, id, net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id))
    }
    override fun isModLoaded(id: String) = FabricLoader.getInstance().isModLoaded(id)
    override fun configDir() = FabricLoader.getInstance().configDir
    override fun version() = FabricLoader.getInstance().getModContainer(AndromedaOrigins.MOD_ID).orElseThrow().metadata.version.friendlyString
    override fun serverTick(callback: Consumer<MinecraftServer>) = ServerTickEvents.END_SERVER_TICK.register { callback.accept(it) }
    override fun serverStopped(callback: Consumer<MinecraftServer>) = ServerLifecycleEvents.SERVER_STOPPED.register { callback.accept(it) }
    override fun serverStarted(callback: Consumer<MinecraftServer>) = ServerLifecycleEvents.SERVER_STARTED.register { callback.accept(it) }
    override fun reload(callback: ReloadCallback) = ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { server, manager, success -> callback.run(server, manager, success) }
    override fun join(callback: JoinCallback) = ServerPlayConnectionEvents.JOIN.register { handler, sender, server -> callback.run(handler, sender, server) }
    override fun disconnect(callback: BiConsumer<ServerGamePacketListenerImpl, MinecraftServer>) =
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            // Network disconnect callbacks can arrive on Netty; gameplay maps/world state belong to the server.
            val cleanup = Runnable {
                val current = server.playerList.getPlayer(handler.player.uuid)
                if (current == null || current === handler.player) callback.accept(handler, server)
            }
            if (server.isSameThread) cleanup.run() else server.execute(cleanup)
        }
    override fun respawn(callback: RespawnCallback) = ServerPlayerEvents.AFTER_RESPAWN.register { old, new, alive -> callback.run(old, new, alive) }
    override fun damage(callback: DamageCallback) = ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount -> callback.allow(entity, source, amount) }
    override fun death(callback: DamageCallback) = ServerLivingEntityEvents.ALLOW_DEATH.register { entity, source, amount -> callback.allow(entity, source, amount) }
    override fun commands(callback: CommandCallback) = CommandRegistrationCallback.EVENT.register { dispatcher, context, selection -> callback.run(dispatcher, context, selection) }
    override fun resourceReload(id: ResourceLocation, callback: Consumer<ResourceManager>) {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(object : SimpleSynchronousResourceReloadListener {
            override fun getFabricId() = id
            override fun onResourceManagerReload(manager: ResourceManager) = callback.accept(manager)
        })
    }
    override fun registerNetworking() { PayloadTypeRegistry.playS2C().register(OpenConfigPayload.TYPE, OpenConfigPayload.CODEC) }
    override fun openConfig(player: ServerPlayer) { ServerPlayNetworking.send(player, OpenConfigPayload.INSTANCE) }
    override fun versionHandshake() = FabricVersionHandshake.registerServer()
}
