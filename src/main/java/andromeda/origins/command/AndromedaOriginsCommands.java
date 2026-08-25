package andromeda.origins.command;

import andromeda.origins.compat.IncapacitatedCompat;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
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
                .then(literal("repair")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(argument("player", EntityArgumentType.player())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            IncapacitatedCompat.repairPlayer(player);
                            context.getSource().sendFeedback(
                                () -> Text.literal("Andromeda Origins: repaired " + player.getName().getString() + "."),
                                true
                            );
                            return 1;
                        }))))
        );
    }
}
