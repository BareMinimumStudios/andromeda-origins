package andromeda.origins.compat

import andromeda.origins.platform.Platform
import com.mojang.serialization.MapCodec
import dev.overgrown.apoli.action.ActionType
import dev.overgrown.apoli.action.ActionTypes
import dev.overgrown.apoli.condition.context.BiEntityCtx
import dev.overgrown.apoli.condition.context.EntityCtx
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import java.util.UUID

/** A transient tether belongs to the caster that actually hit the victim. */
object ManticoreTether {
    private data class Tether(val caster: UUID, val expires: Long)
    private val victims = mutableMapOf<UUID, Tether>()
    private fun id(path: String) = ResourceLocation.fromNamespaceAndPath("andromeda_origins", path)

    @JvmStatic
    fun registerActions() {
        Platform.serverStopped { victims.clear() }
        ActionTypes.BI_ENTITY.register(id("manticore_bind"), object : ActionType<BiEntityCtx, Unit> {
            override fun codec(): MapCodec<Unit> = MapCodec.unit(Unit)
            override fun run(cfg: Unit, ctx: BiEntityCtx) {
                val caster = ctx.actor() as? ServerPlayer ?: return
                val victim = ctx.target() ?: return
                if (caster === victim || caster.level() !== victim.level()) return
                val now = caster.serverLevel().gameTime
                val existing = victims[victim.uuid]
                // Simultaneous lunges cannot take ownership of another caster's active victim.
                if (existing == null || existing.expires <= now) {
                    victims[victim.uuid] = Tether(caster.uuid, now + 10)
                }
                victims.entries.removeIf { it.value.expires <= now }
            }
        })
        ActionTypes.ENTITY.register(id("manticore_drag"), object : ActionType<EntityCtx, Unit> {
            override fun codec(): MapCodec<Unit> = MapCodec.unit(Unit)
            override fun run(cfg: Unit, ctx: EntityCtx) = drag(ctx.raw())
        })
    }

    private fun drag(victim: Entity) {
        val level = victim.level() as? ServerLevel ?: return
        val tether = victims[victim.uuid] ?: return
        val caster = level.getEntity(tether.caster) as? ServerPlayer
        if (tether.expires <= level.gameTime || !victim.isAlive || caster == null ||
            !caster.isAlive || "mantileap" !in caster.tags || victim.distanceToSqr(caster) > 100.0) {
            victims.remove(victim.uuid)
            return
        }
        victim.teleportTo(caster.x, caster.y, caster.z)
    }
}
