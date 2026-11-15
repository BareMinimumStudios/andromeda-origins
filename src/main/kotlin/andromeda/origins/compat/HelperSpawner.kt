package andromeda.origins.compat

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import dev.overgrown.apoli.PowerContainerAttachment
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import dev.overgrown.apoli.action.ActionTypes
import dev.overgrown.apoli.action.ActionType
import dev.overgrown.apoli.condition.context.EntityCtx
import dev.overgrown.apoli.data.expr.ExprContext
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec

/** Typed ray-hit helpers retain the exact hit coordinates and existing one-tick impact powers. */
object HelperSpawner {
    private data class Helper(val power: String, val height: Double)
    private val types = mapOf(
        "web_nest" to Helper("arachne/helper/webpuller_granter", 0.0),
        "fireball" to Helper("wyverian/helper/fireball_granter", 1.0),
        "dragon_charge" to Helper("champion/wyverian/dragon_charge_granter", 1.0)
    )


    private val hitX = ExprContext.slot("hit_x")
    private val hitY = ExprContext.slot("hit_y")
    private val hitZ = ExprContext.slot("hit_z")

    fun registerActions() {
        ActionTypes.ENTITY.register(ResourceLocation.parse("andromeda_origins:spawn_ray_helper"),
            object : ActionType<EntityCtx, String> {
                override fun codec(): MapCodec<String> = Codec.STRING.fieldOf("kind")
                override fun run(cfg: String, ctx: EntityCtx) {
                    val level = ctx.level() as? ServerLevel ?: return
                    val definition = types[cfg] ?: return
                    // Opoli exposes these only for the synchronous ray hit action. Never delay this action.
                    val position = Vec3(ExprContext.get(hitX), ExprContext.get(hitY), ExprContext.get(hitZ))
                    if (position.x.isFinite() && position.y.isFinite() && position.z.isFinite())
                        spawnAt(level, position, definition)
                }
            })
    }

    private fun spawnAt(level: ServerLevel, position: Vec3, definition: Helper): Int {
        val entity = EntityType.BAT.create(level) ?: return 0
        entity.moveTo(position.x, position.y + definition.height, position.z)
        entity.isNoGravity = true
        entity.isSilent = true
        entity.addTag("helper")
        entity.getAttribute(Attributes.SCALE)?.baseValue = 0.1
        if (!level.addFreshEntity(entity)) return 0
        val container = PowerContainerAttachment.getOrCreate(entity)
        val power = ResourceLocation.parse("andromeda_origins:" + definition.power)
        val owner = ResourceLocation.parse("minecraft:helper")
        if (container == null || !container.addPower(power, owner)) {
            entity.discard()
            return 0
        }
        return 1
    }

    /** Retain the OP-only command for existing external datapacks. Built-in powers use the typed action. */
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
                        spawnAt(source.level, source.position, definition)
                    }))))
    }
}
