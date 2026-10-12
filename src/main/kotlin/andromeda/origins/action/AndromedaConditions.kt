package andromeda.origins.action

import andromeda.origins.compat.IncapacitatedCompat
import com.mojang.serialization.MapCodec
import dev.overgrown.apoli.condition.ConditionType
import dev.overgrown.apoli.condition.ConditionTypes
import dev.overgrown.apoli.condition.context.EntityCtx
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/** Optional integrations expose their authoritative state to server-side powers. */
object AndromedaConditions {
    fun register() {
        ConditionTypes.ENTITY.register(ResourceLocation.parse("andromeda_origins:incapacitated"),
            object : ConditionType<EntityCtx, Unit> {
                override fun codec(): MapCodec<Unit> = MapCodec.unit(Unit)
                override fun test(cfg: Unit, ctx: EntityCtx): Boolean {
                    val player = ctx.raw() as? ServerPlayer ?: return false
                    return IncapacitatedCompat.isDowned(player)
                }
            })
    }
}
