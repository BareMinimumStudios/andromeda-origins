package andromeda.origins.action

import andromeda.origins.AndromedaOrigins
import andromeda.origins.compat.IncapacitatedCompat
import andromeda.origins.compat.StatusEffectCompat
import andromeda.origins.compat.OriginStateCleanup
import andromeda.origins.compat.EnhancedFxCompat
import andromeda.origins.compat.VeilStepManager
import andromeda.origins.compat.VeilbornTransposition
import andromeda.origins.compat.MortalResolveManager
import java.util.function.BiFunction


 /**
 * Typed Opoli actions for Andromeda-specific operations that cannot be expressed faithfully by
 * a stock action. These replace the old JSON -> Brigadier command -> Java round trip.
 */
object AndromedaActions {
    fun register() {
        andromeda.origins.compat.ManticoreTether.registerActions()
        andromeda.origins.compat.ParticleSpheres.register()
        AndromedaActions.register<NoConfig>("safe_revive", SafeReviveAction())
        AndromedaActions.register<NoConfig>("clear_harmful_effects", ClearHarmfulEffectsAction())
        AndromedaActions.register<OriginCfg>("cleanup_origin_state", CleanupOriginStateAction())
        AndromedaActions.register<NoConfig>("cosmetic_lightning", CosmeticLightningAction())
        AndromedaActions.register<NoConfig>("set_health_8", SetHealthEightAction())
        AndromedaActions.register<FxCfg>("enhanced_fx", EnhancedFxAction())
        AndromedaActions.register<LocalFxCfg>("enhanced_fx_local", EnhancedFxLocalAction())
        AndromedaActions.register<FxCfg>("enhanced_fx_hit", EnhancedFxHitAction())
        AndromedaActions.registerBi<NoConfig>("veil_transposition", VeilTranspositionAction())
        AndromedaActions.register<NoConfig>("veil_step_toggle", VeilStepToggleAction())
        AndromedaActions.register<NoConfig>("mortal_resolve", MortalResolveAction())
        AndromedaActions.register<NoConfig>("champion_mortal_resolve", ChampionMortalResolveAction())
        AndromedaActions.register<NoConfig>("stop_champion_mortal_resolve", StopChampionMortalResolveAction())
        AndromedaActions.register<TagCfg>("tag", AndromedaActions.TagAction())
        AndromedaActions.register<ActionbarCfg>("actionbar", ActionbarAction())
        AndromedaActions.register<NoConfig>("clear_fenrkin_stalking_slow", ClearFenrkinStalkingSlowAction())
        AndromedaActions.register<NoConfig>("discard", AndromedaActions.DiscardAction())
    }

    private fun <C>register(path: kotlin.String, action: dev.overgrown.apoli.action.ActionType<dev.overgrown.apoli.condition.context.EntityCtx, C>?) {
        dev.overgrown.apoli.action.ActionTypes.ENTITY.register<C>(AndromedaActions.id(path), action)
    }

    private fun <C>registerBi(path: kotlin.String, action: dev.overgrown.apoli.action.ActionType<dev.overgrown.apoli.condition.context.BiEntityCtx, C>?) {
        dev.overgrown.apoli.action.ActionTypes.BI_ENTITY.register<C>(AndromedaActions.id(path), action)
    }

    private fun id(path: kotlin.String): net.minecraft.resources.ResourceLocation {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, path)
    }

    private enum class NoConfig {
        INSTANCE
    }

    private abstract class NoConfigAction : dev.overgrown.apoli.action.ActionType<dev.overgrown.apoli.condition.context.EntityCtx, NoConfig> {
        override fun codec(): com.mojang.serialization.MapCodec<NoConfig>? {
            return com.mojang.serialization.MapCodec.unit<NoConfig>(NoConfig.INSTANCE)
        }
    }

    private class SafeReviveAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val player = ctx.raw() as? net.minecraft.server.level.ServerPlayer
            if (player != null) {
                IncapacitatedCompat.safeRevive(player)
            }
        }
    }

    private class ClearHarmfulEffectsAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val player = ctx.raw() as? net.minecraft.server.level.ServerPlayer
            if (player != null) {
                StatusEffectCompat.clearHarmfulEffects(player)
            }
        }
    }


    private data class OriginCfg (val origin: kotlin.String) {
    }

    private class CleanupOriginStateAction : dev.overgrown.apoli.action.ActionType<dev.overgrown.apoli.condition.context.EntityCtx, OriginCfg> {
        override fun codec(): com.mojang.serialization.MapCodec<OriginCfg>? {
            return CleanupOriginStateAction.Companion.CODEC
        }

        override fun run(cfg: OriginCfg, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val player = ctx.raw() as? net.minecraft.server.level.ServerPlayer
            if (player != null) {
                OriginStateCleanup.cleanup(player, cfg.origin)
            }
        }
        companion object {
            val CODEC: com.mojang.serialization.MapCodec<OriginCfg>? = com.mojang.serialization.Codec.STRING.fieldOf("origin")
            .xmap<OriginCfg>(java.util.function.Function { origin: kotlin.String -> OriginCfg(origin)}, OriginCfg::origin)

        }}

    private class CosmeticLightningAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val player = ctx.raw() as? net.minecraft.server.level.ServerPlayer
            if (player == null) {
                return
            }
            val lightning: net.minecraft.world.entity.LightningBolt? = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(player.serverLevel())
            if (lightning == null) {
                return
            }
            lightning.moveTo(player.getX(), player.getY(), player.getZ())
            lightning.setVisualOnly(true)
            player.serverLevel().addFreshEntity(lightning)
        }
    }

    private class SetHealthEightAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val player = ctx.raw() as? net.minecraft.server.level.ServerPlayer
            if (player != null) {
                player.setHealth(kotlin.math.min(8.0f, player.getMaxHealth()))
            }
        }
    }

    private class DiscardAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            // Temporary helper entities have no death/drop gameplay; remove them immediately
            // instead of routing through /kill followed by an out-of-world teleport.
            ctx.raw().discard()
        }
    }

    private class ClearFenrkinStalkingSlowAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            if (ctx.living() == null) return
            val movementSpeed: net.minecraft.world.entity.ai.attributes.AttributeInstance? = ctx.living().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
            if (movementSpeed != null) {
                movementSpeed.removeModifier(ClearFenrkinStalkingSlowAction.Companion.MODIFIER_ID)
            }
        }
        companion object {
            val MODIFIER_ID: net.minecraft.resources.ResourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            AndromedaOrigins.MOD_ID,
            "fenrkin_primary_stalking_slow"
            )

        }}


    private data class FxCfg (val event: kotlin.String) {
    }

    private class EnhancedFxAction : dev.overgrown.apoli.action.ActionType<dev.overgrown.apoli.condition.context.EntityCtx, FxCfg> {
        override fun codec(): com.mojang.serialization.MapCodec<FxCfg>? {
            return EnhancedFxAction.Companion.CODEC
        }

        override fun run(cfg: FxCfg, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            EnhancedFxCompat.play(ctx.raw(), cfg.event)
        }
        companion object {
            val CODEC: com.mojang.serialization.MapCodec<FxCfg>? = com.mojang.serialization.Codec.STRING.fieldOf("event")
            .xmap<FxCfg>(java.util.function.Function { event: kotlin.String -> FxCfg(event)}, FxCfg::event)

        }}


    private data class LocalFxCfg (val event: kotlin.String, val left: kotlin.Double, val up: kotlin.Double, val forwards: kotlin.Double) {
    }

    private class EnhancedFxLocalAction : dev.overgrown.apoli.action.ActionType<dev.overgrown.apoli.condition.context.EntityCtx, LocalFxCfg> {
        override fun codec(): com.mojang.serialization.MapCodec<LocalFxCfg>? {
            return EnhancedFxLocalAction.Companion.CODEC
        }

        override fun run(cfg: LocalFxCfg, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val position: net.minecraft.world.phys.Vec3 = net.minecraft.commands.arguments.coordinates.LocalCoordinates(cfg.left, cfg.up, cfg.forwards)
            .getPosition(ctx.raw().createCommandSourceStack())
            EnhancedFxCompat.playAt(ctx.raw(), cfg.event, position)
        }
        companion object {
            val CODEC: com.mojang.serialization.MapCodec<LocalFxCfg>? = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec<LocalFxCfg>(java.util.function.Function { i: com.mojang.serialization.codecs.RecordCodecBuilder.Instance<LocalFxCfg> -> i.group<kotlin.String, kotlin.Double, kotlin.Double, kotlin.Double>(
            com.mojang.serialization.Codec.STRING.fieldOf("event").forGetter<LocalFxCfg>(LocalFxCfg::event),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("left", 0.0).forGetter<LocalFxCfg>(LocalFxCfg::left),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("up", 0.0).forGetter<LocalFxCfg>(LocalFxCfg::up),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("forwards", 0.0).forGetter<LocalFxCfg>(LocalFxCfg::forwards)
            ).apply<LocalFxCfg>(i, com.mojang.datafixers.util.Function4 { event: kotlin.String, left: kotlin.Double, up: kotlin.Double, forwards: kotlin.Double -> LocalFxCfg(event, left, up, forwards)})})

        }}

    private class EnhancedFxHitAction : dev.overgrown.apoli.action.ActionType<dev.overgrown.apoli.condition.context.EntityCtx, FxCfg> {
        override fun codec(): com.mojang.serialization.MapCodec<FxCfg>? {
            return EnhancedFxHitAction.Companion.CODEC
        }

        override fun run(cfg: FxCfg, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val position: net.minecraft.world.phys.Vec3 = net.minecraft.world.phys.Vec3(dev.overgrown.apoli.data.expr.ExprContext.get(EnhancedFxHitAction.Companion.HIT_X), dev.overgrown.apoli.data.expr.ExprContext.get(EnhancedFxHitAction.Companion.HIT_Y), dev.overgrown.apoli.data.expr.ExprContext.get(EnhancedFxHitAction.Companion.HIT_Z))
            EnhancedFxCompat.playAt(ctx.raw(), cfg.event, position)
        }
        companion object {
            val HIT_X: kotlin.Int = dev.overgrown.apoli.data.expr.ExprContext.slot("hit_x")
            val HIT_Y: kotlin.Int = dev.overgrown.apoli.data.expr.ExprContext.slot("hit_y")
            val HIT_Z: kotlin.Int = dev.overgrown.apoli.data.expr.ExprContext.slot("hit_z")
            val CODEC: com.mojang.serialization.MapCodec<FxCfg>? = com.mojang.serialization.Codec.STRING.fieldOf("event")
            .xmap<FxCfg>(java.util.function.Function { event: kotlin.String -> FxCfg(event)}, FxCfg::event)

        }}

    private class VeilStepToggleAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val player = ctx.raw() as? net.minecraft.server.level.ServerPlayer
            if (player != null) {
                VeilStepManager.toggle(player)
            }
        }
    }

    private class VeilTranspositionAction : dev.overgrown.apoli.action.ActionType<dev.overgrown.apoli.condition.context.BiEntityCtx, NoConfig> {
        override fun codec(): com.mojang.serialization.MapCodec<NoConfig>? {
            return com.mojang.serialization.MapCodec.unit<NoConfig>(NoConfig.INSTANCE)
        }

        override fun run(cfg: NoConfig, ctx: dev.overgrown.apoli.condition.context.BiEntityCtx) {
            VeilbornTransposition.transpose(ctx.actor(), ctx.target())
        }
    }

    private class MortalResolveAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val player = ctx.raw() as? net.minecraft.server.level.ServerPlayer
            if (player != null) {
                IncapacitatedCompat.beginMortalResolve(player)
            }
        }
    }

    private class ChampionMortalResolveAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val player = ctx.raw() as? net.minecraft.server.level.ServerPlayer
            if (player != null) {
                IncapacitatedCompat.beginChampionMortalResolve(player)
            }
        }
    }

    private class StopChampionMortalResolveAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val player = ctx.raw() as? net.minecraft.server.level.ServerPlayer
            if (player != null) {
                MortalResolveManager.stopChampion(player)
            }
        }
    }


    private data class TagCfg (val tag: kotlin.String, val operation: kotlin.String) {
    }

    private class TagAction : dev.overgrown.apoli.action.ActionType<dev.overgrown.apoli.condition.context.EntityCtx, TagCfg> {
        override fun codec(): com.mojang.serialization.MapCodec<TagCfg>? {
            return andromeda.origins.action.AndromedaActions.TagAction.Companion.CODEC
        }

        override fun run(cfg: TagCfg, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            when (cfg.operation){"add" -> ctx.raw().addTag(cfg.tag)
                "remove" -> ctx.raw().removeTag(cfg.tag)
                else -> {}}
        }
        companion object {
            val CODEC: com.mojang.serialization.MapCodec<TagCfg>? = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec<TagCfg>(java.util.function.Function { i: com.mojang.serialization.codecs.RecordCodecBuilder.Instance<TagCfg> -> i.group<kotlin.String, kotlin.String>(
            com.mojang.serialization.Codec.STRING.fieldOf("tag").forGetter<TagCfg>(TagCfg::tag),
            com.mojang.serialization.Codec.STRING.fieldOf("operation").forGetter<TagCfg>(TagCfg::operation)
            ).apply<TagCfg>(i, BiFunction { tag: kotlin.String, operation: kotlin.String -> TagCfg(tag, operation)})})

        }}


    private data class ActionbarCfg (val text: net.minecraft.network.chat.Component) {
    }

    private class ActionbarAction : dev.overgrown.apoli.action.ActionType<dev.overgrown.apoli.condition.context.EntityCtx, ActionbarCfg> {
        override fun codec(): com.mojang.serialization.MapCodec<ActionbarCfg>? {
            return ActionbarAction.Companion.CODEC
        }

        override fun run(cfg: ActionbarCfg, ctx: dev.overgrown.apoli.condition.context.EntityCtx) {
            val player = ctx.raw() as? net.minecraft.server.level.ServerPlayer
            if (player != null) {
                // Uses the same vanilla action-bar packet path as /title @s actionbar.
                player.displayClientMessage(cfg.text, true)
            }
        }
        companion object {
            val CODEC: com.mojang.serialization.MapCodec<ActionbarCfg>? = dev.overgrown.apoli.data.TextComponent.CODEC.fieldOf("text")
            .xmap<ActionbarCfg>(java.util.function.Function { text: net.minecraft.network.chat.Component -> ActionbarCfg(text)}, ActionbarCfg::text)

        }}
}
