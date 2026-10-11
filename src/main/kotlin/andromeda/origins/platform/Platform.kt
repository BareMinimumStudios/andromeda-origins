package andromeda.origins.platform

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.BiConsumer

fun interface JoinCallback { fun run(handler: ServerGamePacketListenerImpl, sender: Any?, server: MinecraftServer) }
fun interface RespawnCallback { fun run(oldPlayer: ServerPlayer, newPlayer: ServerPlayer, alive: Boolean) }
fun interface DamageCallback { fun allow(entity: LivingEntity, source: DamageSource, amount: Float): Boolean }
fun interface CommandCallback { fun run(dispatcher: CommandDispatcher<CommandSourceStack>, context: CommandBuildContext, selection: Commands.CommandSelection) }
fun interface ReloadCallback { fun run(server: MinecraftServer, manager: ResourceManager, success: Boolean) }

interface PlatformBackend {
    fun registerItem(id: ResourceLocation)
    fun registerSound(id: ResourceLocation)
    fun isModLoaded(id: String): Boolean
    fun configDir(): Path
    fun version(): String
    fun serverTick(callback: Consumer<MinecraftServer>)
    fun serverStopped(callback: Consumer<MinecraftServer>)
    fun serverStarted(callback: Consumer<MinecraftServer>)
    fun reload(callback: ReloadCallback)
    fun join(callback: JoinCallback)
    fun disconnect(callback: BiConsumer<ServerGamePacketListenerImpl, MinecraftServer>)
    fun respawn(callback: RespawnCallback)
    fun damage(callback: DamageCallback)
    fun death(callback: DamageCallback)
    fun commands(callback: CommandCallback)
    fun resourceReload(id: ResourceLocation, callback: Consumer<ResourceManager>)
    fun registerNetworking()
    fun openConfig(player: ServerPlayer)
    fun versionHandshake()
}

/** All loader calls live behind this bridge; gameplay and data remain shared. */
object Platform {
    private lateinit var backend: PlatformBackend
    @JvmStatic fun install(value: PlatformBackend) { check(!::backend.isInitialized); backend = value }
    @JvmStatic fun registerItem(id: ResourceLocation) = backend.registerItem(id)
    @JvmStatic fun registerSound(id: ResourceLocation) = backend.registerSound(id)
    @JvmStatic fun isModLoaded(id: String) = backend.isModLoaded(id)
    @JvmStatic fun configDir() = backend.configDir()
    @JvmStatic fun version() = backend.version()
    @JvmStatic fun serverTick(callback: Consumer<MinecraftServer>) = backend.serverTick(callback)
    @JvmStatic fun serverStopped(callback: Consumer<MinecraftServer>) = backend.serverStopped(callback)
    @JvmStatic fun serverStarted(callback: Consumer<MinecraftServer>) = backend.serverStarted(callback)
    @JvmStatic fun reload(callback: ReloadCallback) = backend.reload(callback)
    @JvmStatic fun join(callback: JoinCallback) = backend.join(callback)
    @JvmStatic fun disconnect(callback: BiConsumer<ServerGamePacketListenerImpl, MinecraftServer>) = backend.disconnect(callback)
    @JvmStatic fun respawn(callback: RespawnCallback) = backend.respawn(callback)
    @JvmStatic fun damage(callback: DamageCallback) = backend.damage(callback)
    @JvmStatic fun death(callback: DamageCallback) = backend.death(callback)
    @JvmStatic fun commands(callback: CommandCallback) = backend.commands(callback)
    @JvmStatic fun resourceReload(id: ResourceLocation, callback: Consumer<ResourceManager>) = backend.resourceReload(id, callback)
    @JvmStatic fun registerNetworking() = backend.registerNetworking()
    @JvmStatic fun openConfig(player: ServerPlayer) = backend.openConfig(player)
    @JvmStatic fun versionHandshake() = backend.versionHandshake()
}
