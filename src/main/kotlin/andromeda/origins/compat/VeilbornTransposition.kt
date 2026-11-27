package andromeda.origins.compat

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.RelativeMovement
import net.minecraft.world.phys.Vec3




 /** Performs Veil Transposition as a same-tick exchange of the exact Opoli raycast actor/target pair.  */
object VeilbornTransposition {
    fun transpose(actor: Entity?, target: Entity?): Boolean {
        if (actor == null || target == null || actor === target) return false
        val level = actor.level() as? ServerLevel
        if (level == null || target.level() !== level) return false
        if (actor.isRemoved() || target.isRemoved()) return false

        val actorPos: Vec3 = actor.position()
        val targetPos: Vec3 = target.position()
        val actorYaw: Float = actor.getYRot()
        val actorPitch: Float = actor.getXRot()
        val targetYaw: Float = target.getYRot()
        val targetPitch: Float = target.getXRot()

                // A passenger is continuously repositioned by its vehicle. Detach only the entity being
        // transposed; vehicles that are themselves targeted keep their passengers and move normally.
        val actorVehicle: Entity? = actor.getVehicle()
        val targetVehicle: Entity? = target.getVehicle()
        if (actorVehicle != null) actor.stopRiding()
        if (targetVehicle != null) target.stopRiding()

                // Actor-first matches Opoli's native swap ordering and avoids the caster briefly seeing the
        // target moved onto the caster before their own teleport packet is delivered. Both happen in
        // this invocation/server tick; the target is never re-raycast or re-selected.
        if (!VeilbornTransposition.teleport(actor, level, targetPos, actorYaw, actorPitch)) {
            VeilbornTransposition.restoreRide(actor, actorVehicle)
            VeilbornTransposition.restoreRide(target, targetVehicle)
            return false
        }
        if (!VeilbornTransposition.teleport(target, level, actorPos, targetYaw, targetPitch)) {
            VeilbornTransposition.teleport(actor, level, actorPos, actorYaw, actorPitch)
            VeilbornTransposition.restoreRide(actor, actorVehicle)
            VeilbornTransposition.restoreRide(target, targetVehicle)
            return false
        }
        return true
    }

    private fun teleport(entity: Entity, level: ServerLevel, pos: Vec3, yaw: Float, pitch: Float): Boolean {
        return entity.teleportTo(level, pos.x, pos.y, pos.z, mutableSetOf<RelativeMovement>(), yaw, pitch)
    }

    private fun restoreRide(passenger: Entity, vehicle: Entity?) {
        if (vehicle != null && !vehicle.isRemoved() && passenger.getVehicle() == null) {
            passenger.startRiding(vehicle, true)
        }
    }
}
