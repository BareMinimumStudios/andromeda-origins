package andromeda.origins.command

import andromeda.origins.compat.EnhancedFxCompat
import andromeda.origins.compat.EnhancedFxConfig
import andromeda.origins.compat.HelperSpawner
import andromeda.origins.network.AndromedaConfigOpenNetworking
import andromeda.origins.platform.CommandCallback
import andromeda.origins.platform.Platform
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import java.util.function.Predicate
import java.util.function.Supplier
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer


 /** Operator-facing Andromeda commands. Internal power callbacks are typed Opoli actions now.  */
object AndromedaOriginsCommands {
    fun register() {
        Platform.commands(CommandCallback callback@ { dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: Commands.CommandSelection -> HelperSpawner.register(dispatcher)
        dispatcher.register(Commands.literal("andromedaorigins")
        .then(Commands.literal("config")
        .requires(Predicate { source: CommandSourceStack -> source.hasPermission(2)})
        .executes(Command { context: CommandContext<CommandSourceStack> -> val player: ServerPlayer = context.getSource().getPlayerOrException()
        AndromedaConfigOpenNetworking.open(player)
        1
        }))
        .then(Commands.literal("enhanced_fx")
        .requires(Predicate { source: CommandSourceStack -> source.hasPermission(2)})
        .executes(Command { context: CommandContext<CommandSourceStack> -> context.getSource().sendSuccess(
        Supplier {Component.literal("Andromeda Origins: " + EnhancedFxCompat.statusLine())}, false)
        1
        })
        .then(Commands.literal("enabled")
        .then(Commands.argument<Boolean>("value", BoolArgumentType.bool())
        .executes(Command { context: CommandContext<CommandSourceStack> -> val value: Boolean = BoolArgumentType.getBool(context, "value")
        EnhancedFxConfig.setEnabled(value)
        context.getSource().sendSuccess(
        Supplier {Component.literal("Andromeda Origins enhanced FX enabled=" + value)}, true)
        1
        })))
        .then(Commands.literal("particles")
        .then(Commands.argument<Boolean>("value", BoolArgumentType.bool())
        .executes(Command { context: CommandContext<CommandSourceStack> -> val value: Boolean = BoolArgumentType.getBool(context, "value")
        EnhancedFxConfig.setParticles(value)
        context.getSource().sendSuccess(
        Supplier {Component.literal("Andromeda Origins enhanced particles=" + value)}, true)
        1
        })))
        .then(Commands.literal("sounds")
        .then(Commands.argument<Boolean>("value", BoolArgumentType.bool())
        .executes(Command { context: CommandContext<CommandSourceStack> -> val value: Boolean = BoolArgumentType.getBool(context, "value")
        EnhancedFxConfig.setSounds(value)
        context.getSource().sendSuccess(
        Supplier {Component.literal("Andromeda Origins enhanced sounds=" + value)}, true)
        1
        }))))
        )}
        )
    }
}
