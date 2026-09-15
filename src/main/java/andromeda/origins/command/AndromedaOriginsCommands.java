package andromeda.origins.command;

import andromeda.origins.compat.EnhancedFxCompat;
import andromeda.origins.compat.EnhancedFxConfig;
import andromeda.origins.compat.IncapacitatedCompat;
import andromeda.origins.compat.MortalResolveManager;
import andromeda.origins.compat.OriginAttributeRepair;
import andromeda.origins.compat.IronWeaknessMigration;
import andromeda.origins.compat.StatusEffectCompat;
import andromeda.origins.compat.UndetectableCompat;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class AndromedaOriginsCommands {

    private AndromedaOriginsCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(literal("andromedaorigins")
                // Internal command used by the Lichling power. Origins execute_command already
                // operates at an elevated permission level; normal survival players cannot invoke it.
                .then(literal("internal_safe_revive")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                            return 0;
                        }
                        return IncapacitatedCompat.safeRevive(player) ? 1 : 0;
                    }))
                .then(literal("internal_clear_harmful_effects")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                            return 0;
                        }
                        StatusEffectCompat.clearHarmfulEffects(player);
                        return 1;
                    }))
                .then(literal("internal_cosmetic_lightning")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                            return 0;
                        }
                        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(player.getServerWorld());
                        if (lightning == null) {
                            return 0;
                        }
                        lightning.refreshPositionAfterTeleport(player.getX(), player.getY(), player.getZ());
                        lightning.setCosmetic(true);
                        player.getServerWorld().spawnEntity(lightning);
                        return 1;
                    }))
                // Fenrkin Adrenaline runs this one tick after Apoli's prevent_death action.
                // Setting an exact value here avoids other death/downed hooks overwriting an additive heal.
                .then(literal("internal_set_health_8")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                            return 0;
                        }
                        player.setHealth(Math.min(8.0F, player.getMaxHealth()));
                        return 1;
                    }))
                // Cosmetic hook called from data powers. It is intentionally an internal op-level command
                // because Apoli execute_command runs power actions at an elevated permission level.
                .then(literal("internal_fx")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(argument("event", StringArgumentType.word())
                        .executes(context -> {
                            if (context.getSource().getEntity() == null) {
                                return 0;
                            }
                            return EnhancedFxCompat.play(
                                context.getSource().getEntity(),
                                StringArgumentType.getString(context, "event")
                            ) ? 1 : 0;
                        })))
                .then(literal("internal_fx_at")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(argument("event", StringArgumentType.word())
                        .then(argument("pos", Vec3ArgumentType.vec3())
                            .executes(context -> {
                                if (context.getSource().getEntity() == null) {
                                    return 0;
                                }
                                return EnhancedFxCompat.playAt(
                                    context.getSource().getEntity(),
                                    StringArgumentType.getString(context, "event"),
                                    Vec3ArgumentType.getVec3(context, "pos")
                                ) ? 1 : 0;
                            }))))
                // Admin-facing switches let a server compare enhanced and baseline presentation
                // without uninstalling Spell Engine / More RPG Library.
                .then(literal("enhanced_fx")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        context.getSource().sendFeedback(
                            () -> Text.literal("Andromeda Origins: " + EnhancedFxCompat.statusLine()), false);
                        return 1;
                    })
                    .then(literal("enabled")
                        .then(argument("value", BoolArgumentType.bool())
                            .executes(context -> {
                                boolean value = BoolArgumentType.getBool(context, "value");
                                EnhancedFxConfig.setEnabled(value);
                                context.getSource().sendFeedback(
                                    () -> Text.literal("Andromeda Origins enhanced FX enabled=" + value), true);
                                return 1;
                            })))
                    .then(literal("particles")
                        .then(argument("value", BoolArgumentType.bool())
                            .executes(context -> {
                                boolean value = BoolArgumentType.getBool(context, "value");
                                EnhancedFxConfig.setParticles(value);
                                context.getSource().sendFeedback(
                                    () -> Text.literal("Andromeda Origins enhanced particles=" + value), true);
                                return 1;
                            })))
                    .then(literal("sounds")
                        .then(argument("value", BoolArgumentType.bool())
                            .executes(context -> {
                                boolean value = BoolArgumentType.getBool(context, "value");
                                EnhancedFxConfig.setSounds(value);
                                context.getSource().sendFeedback(
                                    () -> Text.literal("Andromeda Origins enhanced sounds=" + value), true);
                                return 1;
                            }))))
                .then(literal("internal_mortal_resolve")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                            return 0;
                        }
                        return IncapacitatedCompat.beginMortalResolve(player) ? 1 : 0;
                    }))
                .then(literal("internal_champion_mortal_resolve")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                            return 0;
                        }
                        return IncapacitatedCompat.beginChampionMortalResolve(player) ? 1 : 0;
                    }))
                .then(literal("internal_stop_champion_mortal_resolve")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                            return 0;
                        }
                        MortalResolveManager.stopChampion(player);
                        return 1;
                    }))
                .then(literal("repair")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(argument("player", EntityArgumentType.player())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            IronWeaknessMigration.Result migrationResult = IronWeaknessMigration.migratePlayer(player);
                            OriginAttributeRepair.Result attributeResult = OriginAttributeRepair.repairAttributes(player);
                            IncapacitatedCompat.repairPlayer(player);
                            // Clear a stale stealth marker if an interrupted power lifecycle left one behind.
                            // A legitimately active Undetectable power reasserts it on its next one-second sync.
                            player.removeCommandTag(UndetectableCompat.COMMAND_TAG);

                            if (!attributeResult.apoliRebuilt()) {
                                context.getSource().sendError(Text.literal(
                                    "Andromeda Origins: repaired temporary/CC state for " + player.getName().getString()
                                        + ", but Origin attributes could not be rebuilt safely. Check the server log; raw attribute bases were not intentionally reset."
                                ));
                                return 0;
                            }

                            if (!migrationResult.successful()) {
                                context.getSource().sendError(Text.literal(
                                    "Andromeda Origins: rebuilt attributes/temporary state for " + player.getName().getString()
                                        + ", but the legacy iron-weakness power migration could not be completed safely. Check the server log."
                                ));
                                return 0;
                            }

                            context.getSource().sendFeedback(
                                () -> Text.literal(
                                    "Andromeda Origins: repaired " + player.getName().getString()
                                        + " (" + migrationResult.powersAdded() + " missing iron-weakness powers restored, "
                                        + attributeResult.baseAttributesReset() + " bases reset, "
                                        + attributeResult.staleAndromedaModifiersRemoved() + " stale Andromeda modifiers cleared, "
                                        + attributeResult.attributePowersReapplied() + " active attribute powers rebuilt)."
                                ),
                                true
                            );
                            return 1;
                        })))
                .then(literal("repair_attributes")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(argument("player", EntityArgumentType.player())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            OriginAttributeRepair.Result result = OriginAttributeRepair.repairAttributes(player);
                            if (!result.apoliRebuilt()) {
                                context.getSource().sendError(Text.literal(
                                    "Andromeda Origins: could not rebuild Origin attributes safely for "
                                        + player.getName().getString() + ". Check the server log."
                                ));
                                return 0;
                            }
                            context.getSource().sendFeedback(
                                () -> Text.literal(
                                    "Andromeda Origins: rebuilt Origin attributes for " + player.getName().getString()
                                        + " (" + result.baseAttributesReset() + " bases reset, "
                                        + result.staleAndromedaModifiersRemoved() + " stale Andromeda modifiers cleared, "
                                        + result.attributePowersReapplied() + " active attribute powers rebuilt)."
                                ),
                                true
                            );
                            return 1;
                        }))))
        );
    }
}
