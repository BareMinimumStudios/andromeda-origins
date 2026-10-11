package andromeda.origins.command

import andromeda.origins.compat.EnhancedFxCompat
import andromeda.origins.network.AndromedaConfigOpenNetworking
import andromeda.origins.platform.CommandCallback
import andromeda.origins.compat.EnhancedFxConfig


 /** Operator-facing Andromeda commands. Internal power callbacks are typed Opoli actions now.  */
object AndromedaOriginsCommands {
    fun register() {
        andromeda.origins.platform.Platform.commands(CommandCallback callback@ { dispatcher: com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>, registryAccess: net.minecraft.commands.CommandBuildContext, environment: net.minecraft.commands.Commands.CommandSelection -> andromeda.origins.compat.HelperSpawner.register(dispatcher)
        dispatcher.register(net.minecraft.commands.Commands.literal("andromedaorigins")
        .then(net.minecraft.commands.Commands.literal("config")
        .requires(java.util.function.Predicate { source: net.minecraft.commands.CommandSourceStack -> source.hasPermission(2)})
        .executes(com.mojang.brigadier.Command { context: com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> -> val player: net.minecraft.server.level.ServerPlayer = context.getSource().getPlayerOrException()
        AndromedaConfigOpenNetworking.open(player)
        1
        }))
        .then(net.minecraft.commands.Commands.literal("enhanced_fx")
        .requires(java.util.function.Predicate { source: net.minecraft.commands.CommandSourceStack -> source.hasPermission(2)})
        .executes(com.mojang.brigadier.Command { context: com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> -> context.getSource().sendSuccess(
        java.util.function.Supplier {net.minecraft.network.chat.Component.literal("Andromeda Origins: " + EnhancedFxCompat.statusLine())}, false)
        1
        })
        .then(net.minecraft.commands.Commands.literal("enabled")
        .then(net.minecraft.commands.Commands.argument<kotlin.Boolean>("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
        .executes(com.mojang.brigadier.Command { context: com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> -> val value: kotlin.Boolean = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "value")
        EnhancedFxConfig.setEnabled(value)
        context.getSource().sendSuccess(
        java.util.function.Supplier {net.minecraft.network.chat.Component.literal("Andromeda Origins enhanced FX enabled=" + value)}, true)
        1
        })))
        .then(net.minecraft.commands.Commands.literal("particles")
        .then(net.minecraft.commands.Commands.argument<kotlin.Boolean>("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
        .executes(com.mojang.brigadier.Command { context: com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> -> val value: kotlin.Boolean = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "value")
        EnhancedFxConfig.setParticles(value)
        context.getSource().sendSuccess(
        java.util.function.Supplier {net.minecraft.network.chat.Component.literal("Andromeda Origins enhanced particles=" + value)}, true)
        1
        })))
        .then(net.minecraft.commands.Commands.literal("sounds")
        .then(net.minecraft.commands.Commands.argument<kotlin.Boolean>("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
        .executes(com.mojang.brigadier.Command { context: com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> -> val value: kotlin.Boolean = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "value")
        EnhancedFxConfig.setSounds(value)
        context.getSource().sendSuccess(
        java.util.function.Supplier {net.minecraft.network.chat.Component.literal("Andromeda Origins enhanced sounds=" + value)}, true)
        1
        }))))
        )}
        )
    }
}
