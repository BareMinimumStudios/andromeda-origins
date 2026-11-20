package andromeda.origins.compat

import net.minecraft.world.phys.Vec3

/** Shared local-space slots keep held projectiles and their client lenses aligned. Negative Z is behind. */
object DawnbreakerLayout {
    fun guardSlot(index: Int): Vec3 {
        val row = index.coerceIn(0, 7) / 2
        val side = if (index % 2 == 0) -1.0 else 1.0
        return Vec3(side * (1.0 + row * .45), 1.2 + row * .45, -1.15 - row * .16)
    }
}
