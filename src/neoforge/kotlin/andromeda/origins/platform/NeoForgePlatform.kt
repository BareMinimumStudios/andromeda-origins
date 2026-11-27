package andromeda.origins.platform

import andromeda.origins.AndromedaOrigins
import andromeda.origins.client.DawnbreakerAudio
import andromeda.origins.client.DawnbreakerFxClient
import andromeda.origins.client.OriginFxRenderer
import andromeda.origins.network.BanishmentHeartbeatPayload
import andromeda.origins.network.DawnbreakerFxPayload
import andromeda.origins.network.OpenConfigPayload
import andromeda.origins.network.OriginFxPayload
import java.util.UUID
import java.util.function.BiConsumer
import java.util.function.Consumer
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.Item
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModList
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.AddReloadListenerEvent
import net.neoforged.neoforge.event.OnDatapackSyncEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.registries.RegisterEvent

@Mod(AndromedaOrigins.MOD_ID)
class NeoForgeEntrypoint(bus: IEventBus) {
    init {
        Platform.install(NeoForgePlatform(bus))
        AndromedaOrigins.initialize()
        if (FMLEnvironment.dist == Dist.CLIENT) NeoForgeClientPlatform.install(bus)
    }
}

class NeoForgePlatform(private val modBus: IEventBus) : PlatformBackend {
    override fun registerItem(id: ResourceLocation) {
        modBus.addListener(Consumer<RegisterEvent> { event ->
            event.register(Registries.ITEM, id) {
                Item(Item.Properties())
            }
        })
    }
    override fun registerSound(id: ResourceLocation) {
        modBus.addListener(Consumer<RegisterEvent> { event ->
            event.register(Registries.SOUND_EVENT, id) {
                SoundEvent.createVariableRangeEvent(id)
            }
        })
    }
    override fun isModLoaded(id: String) = ModList.get().isLoaded(id)
    override fun configDir() = FMLPaths.CONFIGDIR.get()
    override fun version() = ModList.get().getModContainerById(AndromedaOrigins.MOD_ID).orElseThrow().modInfo.version.toString()
    override fun serverTick(callback: Consumer<MinecraftServer>) { NeoForge.EVENT_BUS.addListener(Consumer<ServerTickEvent.Post> { callback.accept(it.server) }) }
    override fun serverStopped(callback: Consumer<MinecraftServer>) { NeoForge.EVENT_BUS.addListener(Consumer<ServerStoppedEvent> { callback.accept(it.server) }) }
    override fun serverStarted(callback: Consumer<MinecraftServer>) { NeoForge.EVENT_BUS.addListener(Consumer<ServerStartedEvent> { callback.accept(it.server) }) }
    override fun reload(callback: ReloadCallback) {
        NeoForge.EVENT_BUS.addListener(Consumer<OnDatapackSyncEvent> { event ->
            if (event.player == null) {
                val server = event.playerList.server
                callback.run(server, server.resourceManager, true)
            }
        })
    }
    override fun join(callback: JoinCallback) {
        NeoForge.EVENT_BUS.addListener(Consumer<PlayerEvent.PlayerLoggedInEvent> { event ->
            (event.entity as? ServerPlayer)?.let { callback.run(it.connection, null, it.server) }
        })
    }
    override fun disconnect(callback: BiConsumer<ServerGamePacketListenerImpl, MinecraftServer>) {
        NeoForge.EVENT_BUS.addListener(Consumer<PlayerEvent.PlayerLoggedOutEvent> { event ->
            (event.entity as? ServerPlayer)?.let { callback.accept(it.connection, it.server) }
        })
    }
    override fun respawn(callback: RespawnCallback) {
        val originals = mutableMapOf<UUID, Pair<ServerPlayer, Boolean>>()
        NeoForge.EVENT_BUS.addListener(Consumer<PlayerEvent.Clone> { event ->
            val new = event.entity as? ServerPlayer ?: return@Consumer
            val old = event.original as? ServerPlayer ?: return@Consumer
            originals[new.uuid] = old to !event.isWasDeath
        })
        NeoForge.EVENT_BUS.addListener(Consumer<PlayerEvent.PlayerRespawnEvent> { event ->
            val new = event.entity as? ServerPlayer ?: return@Consumer
            val (old, alive) = originals.remove(new.uuid) ?: return@Consumer
            callback.run(old, new, alive)
        })
    }
    override fun damage(callback: DamageCallback) {
        NeoForge.EVENT_BUS.addListener(Consumer<LivingIncomingDamageEvent> { if (!callback.allow(it.entity, it.source, it.amount)) it.isCanceled = true })
    }
    override fun death(callback: DamageCallback) {
        NeoForge.EVENT_BUS.addListener(Consumer<LivingDeathEvent> { if (!callback.allow(it.entity, it.source, 0f)) it.isCanceled = true })
    }
    override fun commands(callback: CommandCallback) {
        NeoForge.EVENT_BUS.addListener(Consumer<RegisterCommandsEvent> { callback.run(it.dispatcher, it.buildContext, it.commandSelection) })
    }
    override fun resourceReload(id: ResourceLocation, callback: Consumer<ResourceManager>) {
        NeoForge.EVENT_BUS.addListener(Consumer<AddReloadListenerEvent> { event ->
            event.addListener(ResourceManagerReloadListener { manager -> callback.accept(manager) })
        })
    }
    override fun registerNetworking() {
        modBus.addListener(Consumer<RegisterPayloadHandlersEvent> { event ->
            // Required, exact-version payload negotiation rejects missing or mismatched clients.
            event.registrar(version()).playToClient(DawnbreakerFxPayload.TYPE, DawnbreakerFxPayload.CODEC) { payload, context ->
                context.enqueueWork { DawnbreakerFxClient.receive(payload) }
            }
            event.registrar(version()).playToClient(OriginFxPayload.TYPE, OriginFxPayload.CODEC) { payload, context ->
                context.enqueueWork { OriginFxRenderer.receive(payload) }
            }
            event.registrar(version()).playToClient(OpenConfigPayload.TYPE, OpenConfigPayload.CODEC) { _, context ->
                context.enqueueWork { NeoForgeClientPlatform.openConfig() }
            }
            event.registrar(version()).playToClient(BanishmentHeartbeatPayload.TYPE, BanishmentHeartbeatPayload.CODEC) { _, context ->
                context.enqueueWork { DawnbreakerAudio.playBanishmentHeartbeat() }
            }
        })
    }
    override fun canReceiveCosmetic(player: ServerPlayer, channel: ResourceLocation): Boolean = player.connection.hasChannel(channel)
    override fun sendCosmetic(player: ServerPlayer, payload: CustomPacketPayload) { PacketDistributor.sendToPlayer(player, payload) }

    override fun openConfig(player: ServerPlayer) = PacketDistributor.sendToPlayer(player, OpenConfigPayload.INSTANCE)
    override fun versionHandshake() { /* NeoForge negotiates the required payload version. */ }
}
