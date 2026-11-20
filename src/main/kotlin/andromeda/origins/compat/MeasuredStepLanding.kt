package andromeda.origins.compat

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.*

/** Collision-shape surfaces handle slabs, stairs and varying player dimensions without loading destination chunks. */
object MeasuredStepLanding {
    data class Result(val position: Vec3?, val reason: String)
    private val hazards = setOf(Blocks.MAGMA_BLOCK, Blocks.CACTUS, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
        Blocks.SWEET_BERRY_BUSH, Blocks.POWDER_SNOW, Blocks.WITHER_ROSE, Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.END_GATEWAY)
    private fun loaded(level: ServerLevel, box: AABB): Boolean =
        (floor(box.minX).toInt().shr(4)..floor(box.maxX).toInt().shr(4)).all { x ->
            (floor(box.minZ).toInt().shr(4)..floor(box.maxZ).toInt().shr(4)).all { z -> level.chunkSource.getChunkNow(x, z) != null }
        }
    fun find(player: ServerPlayer): Result {
        val level = player.serverLevel(); val eye = player.eyePosition
        // A grounded blink follows heading rather than intersecting the floor beside the caster
        // whenever the camera is tilted down. Pitch still influences which elevation is preferred.
        val yaw = Math.toRadians(player.yRot.toDouble())
        val look = Vec3(-sin(yaw), 0.0, cos(yaw))
        var available = 0.0
        for (quarter in 1..64) {
            val distance = quarter / 4.0
            if (!loaded(level, AABB(eye, eye.add(look.scale(distance))))) break
            available = distance
        }
        if (available < 1.5) return Result(null, "Measured Step — not enough loaded terrain ahead.")
        val wall = level.clip(ClipContext(eye, eye.add(look.scale(available)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
        val reach = min(available, eye.distanceTo(wall.location) - .45)
        for (half in floor(reach * 2).toInt() downTo 3) {
            val aim = player.position().add(look.scale(half / 2.0))
            val preferred = (player.lookAngle.y * half / 2.0).coerceIn(-3.0, 2.0)
            val heights = (-6..2).sortedBy { abs(it - preferred) }
            for (offset in heights) {
                val floor = BlockPos.containing(aim).above(offset)
                if (level.chunkSource.getChunkNow(floor.x shr 4, floor.z shr 4) == null) continue
                val state = level.getBlockState(floor)
                if (!state.fluidState.isEmpty || state.block in hazards || state.`is`(BlockTags.FIRE)) continue
                val shapes = state.getCollisionShape(level, floor).toAabbs().filter {
                    aim.x >= floor.x + it.minX - .0001 && aim.x <= floor.x + it.maxX + .0001 &&
                    aim.z >= floor.z + it.minZ - .0001 && aim.z <= floor.z + it.maxZ + .0001
                }.sortedByDescending { it.maxY }
                for (shape in shapes) {
                    val pos = Vec3(aim.x, floor.y + shape.maxY, aim.z)
                    if (player.distanceToSqr(pos) < 1.0 || player.distanceToSqr(pos) > 256.0) continue
                    val box = player.boundingBox.move(pos.subtract(player.position()))
                    if (!loaded(level, box.inflate(.001)) || !level.worldBorder.isWithinBounds(box) || !level.noCollision(player, box)) continue
                    val inside = box.deflate(.0001)
                    if (BlockPos.betweenClosed(BlockPos.containing(inside.minX, inside.minY, inside.minZ), BlockPos.containing(inside.maxX, inside.maxY, inside.maxZ)).any {
                        val at = level.getBlockState(it); !at.fluidState.isEmpty || at.block in hazards || at.`is`(BlockTags.FIRE)
                    }) continue
                    val sight = pos.add(0.0, player.eyeHeight.toDouble(), 0.0)
                    if (!loaded(level, AABB(eye, sight))) continue
                    val hit = level.clip(ClipContext(eye, sight, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                    if (eye.distanceToSqr(hit.location) + .01 < eye.distanceToSqr(sight)) continue
                    return Result(pos, "")
                }
            }
        }
        return Result(null, "Measured Step — no clear, dry landing fits your size before the obstruction.")
    }
}
