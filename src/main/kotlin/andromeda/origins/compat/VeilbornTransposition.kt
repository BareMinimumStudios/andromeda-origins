package andromeda.origins.compat




 /** Performs Veil Transposition as a same-tick exchange of the exact Opoli raycast actor/target pair.  */
object VeilbornTransposition {
    fun transpose(actor: net.minecraft.world.entity.Entity?, target: net.minecraft.world.entity.Entity?): kotlin.Boolean {
        if (actor == null || target == null || actor === target) return false
        val level = actor.level() as? net.minecraft.server.level.ServerLevel
        if (level == null || target.level() !== level) return false
        if (actor.isRemoved() || target.isRemoved()) return false

        val actorPos: net.minecraft.world.phys.Vec3 = actor.position()
        val targetPos: net.minecraft.world.phys.Vec3 = target.position()
        val actorYaw: kotlin.Float = actor.getYRot()
        val actorPitch: kotlin.Float = actor.getXRot()
        val targetYaw: kotlin.Float = target.getYRot()
        val targetPitch: kotlin.Float = target.getXRot()

                // A passenger is continuously repositioned by its vehicle. Detach only the entity being
        // transposed; vehicles that are themselves targeted keep their passengers and move normally.
        val actorVehicle: net.minecraft.world.entity.Entity? = actor.getVehicle()
        val targetVehicle: net.minecraft.world.entity.Entity? = target.getVehicle()
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

    private fun teleport(entity: net.minecraft.world.entity.Entity, level: net.minecraft.server.level.ServerLevel, pos: net.minecraft.world.phys.Vec3, yaw: kotlin.Float, pitch: kotlin.Float): kotlin.Boolean {
        return entity.teleportTo(level, pos.x, pos.y, pos.z, kotlin.collections.mutableSetOf<net.minecraft.world.entity.RelativeMovement>(), yaw, pitch)
    }

    private fun restoreRide(passenger: net.minecraft.world.entity.Entity, vehicle: net.minecraft.world.entity.Entity?) {
        if (vehicle != null && !vehicle.isRemoved() && passenger.getVehicle() == null) {
            passenger.startRiding(vehicle, true)
        }
    }
}
