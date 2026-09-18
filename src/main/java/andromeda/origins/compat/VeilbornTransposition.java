package andromeda.origins.compat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.function.Predicate;

/** Performs Veil Transposition atomically without shared marker entities or global selector tags. */
public final class VeilbornTransposition {

    private static final double RANGE = 32.0D;

    private VeilbornTransposition() {}

    public static boolean transpose(ServerPlayerEntity player) {
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d origin = new Vec3d(
            player.getX(),
            player.getY() + player.getEyeHeight(player.getPose()),
            player.getZ()
        );
        Vec3d ray = direction.multiply(RANGE);
        Vec3d destination = origin.add(ray);
        Box box = player.getBoundingBox().stretch(ray).expand(1.0D);

        Predicate<Entity> predicate = EntityPredicates.EXCEPT_SPECTATOR
            .and(entity -> entity instanceof LivingEntity)
            .and(entity -> canSee(player, entity));

        EntityHitResult hit = ProjectileUtil.raycast(
            player,
            origin,
            destination,
            box,
            predicate,
            RANGE * RANGE
        );
        if (hit == null) {
            return false;
        }

        Entity target = hit.getEntity();
        Vec3d playerPos = player.getPos();
        Vec3d targetPos = target.getPos();

        // Request both teleports from the saved pre-swap positions so no shared marker state exists.
        target.requestTeleport(playerPos.x, playerPos.y, playerPos.z);
        player.requestTeleport(targetPos.x, targetPos.y, targetPos.z);
        return true;
    }

    private static boolean canSee(Entity actor, Entity target) {
        if (actor.getWorld() != target.getWorld()) {
            return false;
        }
        RaycastContext context = new RaycastContext(
            actor.getEyePos(),
            target.getEyePos(),
            RaycastContext.ShapeType.VISUAL,
            RaycastContext.FluidHandling.NONE,
            actor
        );
        return actor.getWorld().raycast(context).getType() == HitResult.Type.MISS;
    }
}
