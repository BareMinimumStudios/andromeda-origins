package andromeda.origins.action

import andromeda.origins.AndromedaOrigins
import andromeda.origins.compat.Dawnbreaker
import andromeda.origins.compat.EnhancedFxCompat
import andromeda.origins.compat.HelperSpawner
import andromeda.origins.compat.IncapacitatedCompat
import andromeda.origins.compat.ManticoreTether
import andromeda.origins.compat.MortalResolveManager
import andromeda.origins.compat.OriginStateCleanup
import andromeda.origins.compat.ParticleSpheres
import andromeda.origins.compat.RefinedCosmeticActions
import andromeda.origins.compat.StatusEffectCompat
import andromeda.origins.compat.VeilStepManager
import andromeda.origins.compat.VeilbornTransposition
import andromeda.origins.config.AndromedaConfigBootstrap
import com.mojang.datafixers.util.Function4
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.overgrown.apoli.action.ActionType
import dev.overgrown.apoli.action.ActionTypes
import dev.overgrown.apoli.condition.context.BiEntityCtx
import dev.overgrown.apoli.condition.context.EntityCtx
import dev.overgrown.apoli.data.TextComponent
import dev.overgrown.apoli.data.expr.ExprContext
import java.util.function.BiFunction
import java.util.function.Function
import kotlin.math.min
import net.minecraft.commands.arguments.coordinates.LocalCoordinates
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.ai.attributes.AttributeInstance
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.phys.Vec3


 /**
 * Typed Opoli actions for Andromeda-specific operations that cannot be expressed faithfully by
 * a stock action. These replace the old JSON -> Brigadier command -> Java round trip.
 */
object AndromedaActions {
    fun register() {
        RefinedCosmeticActions.register()
        Dawnbreaker.registerActions()
        HelperSpawner.registerActions()
        ManticoreTether.registerActions()
        ParticleSpheres.register()
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

    private fun <C>register(path: String, action: ActionType<EntityCtx, C>?) {
        ActionTypes.ENTITY.register<C>(AndromedaActions.id(path), action)
    }

    private fun <C>registerBi(path: String, action: ActionType<BiEntityCtx, C>?) {
        ActionTypes.BI_ENTITY.register<C>(AndromedaActions.id(path), action)
    }

    private fun id(path: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, path)
    }

    private enum class NoConfig {
        INSTANCE
    }

    private abstract class NoConfigAction : ActionType<EntityCtx, NoConfig> {
        override fun codec(): MapCodec<NoConfig>? {
            return MapCodec.unit<NoConfig>(NoConfig.INSTANCE)
        }
    }

    private class SafeReviveAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: EntityCtx) {
            val player = ctx.raw() as? ServerPlayer
            if (player != null) {
                IncapacitatedCompat.safeRevive(player)
            }
        }
    }

    private class ClearHarmfulEffectsAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: EntityCtx) {
            val player = ctx.raw() as? ServerPlayer
            if (player != null) {
                StatusEffectCompat.clearHarmfulEffects(player)
            }
        }
    }


    private data class OriginCfg (val origin: String) {
    }

    private class CleanupOriginStateAction : ActionType<EntityCtx, OriginCfg> {
        override fun codec(): MapCodec<OriginCfg>? {
            return CleanupOriginStateAction.Companion.CODEC
        }

        override fun run(cfg: OriginCfg, ctx: EntityCtx) {
            val player = ctx.raw() as? ServerPlayer
            if (player != null) {
                OriginStateCleanup.cleanup(player, cfg.origin)
            }
        }
        companion object {
            val CODEC: MapCodec<OriginCfg>? = Codec.STRING.fieldOf("origin")
            .xmap<OriginCfg>(Function { origin: String -> OriginCfg(origin)}, OriginCfg::origin)

        }}

    private class CosmeticLightningAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: EntityCtx) {
            val player = ctx.raw() as? ServerPlayer
            if (player == null) {
                return
            }
            if (AndromedaConfigBootstrap.config().visuals.reducedFlashes.get()) {
                player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.x, player.y + 1.0, player.z, 6, .1, .6, .1, .01)
                return
            }
            val lightning: LightningBolt? = EntityType.LIGHTNING_BOLT.create(player.serverLevel())
            if (lightning == null) {
                return
            }
            lightning.moveTo(player.getX(), player.getY(), player.getZ())
            lightning.setVisualOnly(true)
            player.serverLevel().addFreshEntity(lightning)
        }
    }

    private class SetHealthEightAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: EntityCtx) {
            val player = ctx.raw() as? ServerPlayer
            if (player != null) {
                player.setHealth(min(8.0f, player.getMaxHealth()))
            }
        }
    }

    private class DiscardAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: EntityCtx) {
            // Temporary helper entities have no death/drop gameplay; remove them immediately
            // instead of routing through /kill followed by an out-of-world teleport.
            ctx.raw().discard()
        }
    }

    private class ClearFenrkinStalkingSlowAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: EntityCtx) {
            if (ctx.living() == null) return
            val movementSpeed: AttributeInstance? = ctx.living().getAttribute(Attributes.MOVEMENT_SPEED)
            if (movementSpeed != null) {
                movementSpeed.removeModifier(ClearFenrkinStalkingSlowAction.Companion.MODIFIER_ID)
            }
        }
        companion object {
            val MODIFIER_ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
            AndromedaOrigins.MOD_ID,
            "fenrkin_primary_stalking_slow"
            )

        }}


    private data class FxCfg (val event: String) {
    }

    private class EnhancedFxAction : ActionType<EntityCtx, FxCfg> {
        override fun codec(): MapCodec<FxCfg>? {
            return EnhancedFxAction.Companion.CODEC
        }

        override fun run(cfg: FxCfg, ctx: EntityCtx) {
            EnhancedFxCompat.play(ctx.raw(), cfg.event)
        }
        companion object {
            val CODEC: MapCodec<FxCfg>? = Codec.STRING.fieldOf("event")
            .xmap<FxCfg>(Function { event: String -> FxCfg(event)}, FxCfg::event)

        }}


    private data class LocalFxCfg (val event: String, val left: Double, val up: Double, val forwards: Double) {
    }

    private class EnhancedFxLocalAction : ActionType<EntityCtx, LocalFxCfg> {
        override fun codec(): MapCodec<LocalFxCfg>? {
            return EnhancedFxLocalAction.Companion.CODEC
        }

        override fun run(cfg: LocalFxCfg, ctx: EntityCtx) {
            val position: Vec3 = LocalCoordinates(cfg.left, cfg.up, cfg.forwards)
            .getPosition(ctx.raw().createCommandSourceStack())
            EnhancedFxCompat.playAt(ctx.raw(), cfg.event, position)
        }
        companion object {
            val CODEC: MapCodec<LocalFxCfg>? = RecordCodecBuilder.mapCodec<LocalFxCfg>(Function { i: RecordCodecBuilder.Instance<LocalFxCfg> -> i.group<String, Double, Double, Double>(
            Codec.STRING.fieldOf("event").forGetter<LocalFxCfg>(LocalFxCfg::event),
            Codec.DOUBLE.optionalFieldOf("left", 0.0).forGetter<LocalFxCfg>(LocalFxCfg::left),
            Codec.DOUBLE.optionalFieldOf("up", 0.0).forGetter<LocalFxCfg>(LocalFxCfg::up),
            Codec.DOUBLE.optionalFieldOf("forwards", 0.0).forGetter<LocalFxCfg>(LocalFxCfg::forwards)
            ).apply<LocalFxCfg>(i, Function4 { event: String, left: Double, up: Double, forwards: Double -> LocalFxCfg(event, left, up, forwards)})})

        }}

    private class EnhancedFxHitAction : ActionType<EntityCtx, FxCfg> {
        override fun codec(): MapCodec<FxCfg>? {
            return EnhancedFxHitAction.Companion.CODEC
        }

        override fun run(cfg: FxCfg, ctx: EntityCtx) {
            val position: Vec3 = Vec3(ExprContext.get(EnhancedFxHitAction.Companion.HIT_X), ExprContext.get(EnhancedFxHitAction.Companion.HIT_Y), ExprContext.get(EnhancedFxHitAction.Companion.HIT_Z))
            EnhancedFxCompat.playAt(ctx.raw(), cfg.event, position)
        }
        companion object {
            val HIT_X: Int = ExprContext.slot("hit_x")
            val HIT_Y: Int = ExprContext.slot("hit_y")
            val HIT_Z: Int = ExprContext.slot("hit_z")
            val CODEC: MapCodec<FxCfg>? = Codec.STRING.fieldOf("event")
            .xmap<FxCfg>(Function { event: String -> FxCfg(event)}, FxCfg::event)

        }}

    private class VeilStepToggleAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: EntityCtx) {
            val player = ctx.raw() as? ServerPlayer
            if (player != null) {
                VeilStepManager.toggle(player)
            }
        }
    }

    private class VeilTranspositionAction : ActionType<BiEntityCtx, NoConfig> {
        override fun codec(): MapCodec<NoConfig>? {
            return MapCodec.unit<NoConfig>(NoConfig.INSTANCE)
        }

        override fun run(cfg: NoConfig, ctx: BiEntityCtx) {
            VeilbornTransposition.transpose(ctx.actor(), ctx.target())
        }
    }

    private class MortalResolveAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: EntityCtx) {
            val player = ctx.raw() as? ServerPlayer
            if (player != null) {
                IncapacitatedCompat.beginMortalResolve(player)
            }
        }
    }

    private class ChampionMortalResolveAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: EntityCtx) {
            val player = ctx.raw() as? ServerPlayer
            if (player != null) {
                IncapacitatedCompat.beginChampionMortalResolve(player)
            }
        }
    }

    private class StopChampionMortalResolveAction : NoConfigAction() {
        override fun run(cfg: NoConfig, ctx: EntityCtx) {
            val player = ctx.raw() as? ServerPlayer
            if (player != null) {
                MortalResolveManager.stopChampion(player)
            }
        }
    }


    private data class TagCfg (val tag: String, val operation: String) {
    }

    private class TagAction : ActionType<EntityCtx, TagCfg> {
        override fun codec(): MapCodec<TagCfg>? {
            return TagAction.CODEC
        }

        override fun run(cfg: TagCfg, ctx: EntityCtx) {
            when (cfg.operation){"add" -> ctx.raw().addTag(cfg.tag)
                "remove" -> ctx.raw().removeTag(cfg.tag)
                else -> {}}
        }
        companion object {
            val CODEC: MapCodec<TagCfg>? = RecordCodecBuilder.mapCodec<TagCfg>(Function { i: RecordCodecBuilder.Instance<TagCfg> -> i.group<String, String>(
            Codec.STRING.fieldOf("tag").forGetter<TagCfg>(TagCfg::tag),
            Codec.STRING.fieldOf("operation").forGetter<TagCfg>(TagCfg::operation)
            ).apply<TagCfg>(i, BiFunction { tag: String, operation: String -> TagCfg(tag, operation)})})

        }}


    private data class ActionbarCfg (val text: Component) {
    }

    private class ActionbarAction : ActionType<EntityCtx, ActionbarCfg> {
        override fun codec(): MapCodec<ActionbarCfg>? {
            return ActionbarAction.Companion.CODEC
        }

        override fun run(cfg: ActionbarCfg, ctx: EntityCtx) {
            val player = ctx.raw() as? ServerPlayer
            if (player != null) {
                // Uses the same vanilla action-bar packet path as /title @s actionbar.
                player.displayClientMessage(cfg.text, true)
            }
        }
        companion object {
            val CODEC: MapCodec<ActionbarCfg>? = TextComponent.CODEC.fieldOf("text")
            .xmap<ActionbarCfg>(Function { text: Component -> ActionbarCfg(text)}, ActionbarCfg::text)

        }}
}
