package andromeda.origins.compat

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import dev.overgrown.apoli.PowerContainerAttachment
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.Attributes

/** command_at_hit supplies the precise raycast position, which entity actions do not carry. */
object HelperSpawner {
    private data class Helper(val power: String, val height: Double)
    private val types = mapOf(
        "web_nest" to Helper("arachne/helper/webpuller_granter", 0.0),
        "fireball" to Helper("wyverian/helper/fireball_granter", 1.0),
        "dragon_charge" to Helper("champion/wyverian/dragon_charge_granter", 1.0)
    )

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(Commands.literal("andromedaorigins")
            .then(Commands.literal("internal").requires { it.hasPermission(2) }
                .then(Commands.literal("helper").then(Commands.argument("kind", StringArgumentType.word())
                    .suggests { _, builder ->
                        types.keys.forEach { builder.suggest(it) }
                        builder.buildFuture()
                    }.executes { context ->
                        val definition = types[StringArgumentType.getString(context, "kind")] ?: return@executes 0
                        val source = context.source
                        val entity = EntityType.BAT.create(source.level) ?: return@executes 0
                        val position = source.position
                        entity.moveTo(position.x, position.y + definition.height, position.z)
                        entity.isNoGravity = true
                        entity.isSilent = true
                        entity.addTag("helper")
                        entity.getAttribute(Attributes.SCALE)?.baseValue = 0.1
                        if (!source.level.addFreshEntity(entity)) return@executes 0
                        val container = PowerContainerAttachment.getOrCreate(entity)
                        val power = ResourceLocation.fromNamespaceAndPath("andromeda_origins", definition.power)
                        val owner = ResourceLocation.fromNamespaceAndPath("minecraft", "helper")
                        if (container == null || !container.addPower(power, owner)) {
                            entity.discard()
                            return@executes 0
                        }
                        1
                    }))))
    }
}
