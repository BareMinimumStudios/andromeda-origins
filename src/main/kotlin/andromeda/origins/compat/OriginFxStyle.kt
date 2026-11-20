package andromeda.origins.compat

import andromeda.origins.config.AndromedaConfigBootstrap
import net.minecraft.world.entity.Entity

/** Cosmetic budgets are per emitting entity; unrelated mods and gameplay actions are untouched. */
object OriginFxStyle {
    enum class Quality { MINIMAL, STANDARD, CINEMATIC }
    private class Budget(var tick: Int, var particles: Int = 0, var sounds: Int = 0, val recent: MutableMap<String, Int> = mutableMapOf())
    private val budgets = java.util.WeakHashMap<Entity, Budget>()
    fun quality() = AndromedaConfigBootstrap.config().visuals.quality.get()
    private fun budget(e: Entity): Budget {
        val b = budgets.getOrPut(e) { Budget(e.tickCount) }
        if (b.tick != e.tickCount) { b.tick = e.tickCount; b.particles = 0; b.sounds = 0; b.recent.entries.removeIf { e.tickCount - it.value > 40 || e.tickCount < it.value } }
        return b
    }
    @Synchronized fun particles(e: Entity, requested: Int): Int? {
        if (requested < 0) return null
        val b = budget(e); val limit = if (quality() == Quality.MINIMAL) 12 else 20
        val remaining = limit - b.particles
        if (remaining <= 0) return null
        // Zero is the native directed-single-particle mode, not an empty burst.
        if (requested == 0) { b.particles++; return 0 }
        val n = kotlin.math.ceil(requested.coerceAtLeast(1) * if (quality() == Quality.MINIMAL) .45 else .65).toInt().coerceIn(1, minOf(6, remaining))
        b.particles += n; return n
    }
    @Synchronized fun sound(e: Entity, id: String): Boolean {
        val b = budget(e); val last = b.recent["s:$id"]
        if (b.sounds >= 2 || (last != null && e.tickCount - last < 5)) return false
        if (b.recent.size >= 64) b.recent.clear()
        b.recent["s:$id"] = e.tickCount; b.sounds++; return true
    }
    @Synchronized fun event(e: Entity, id: String): Boolean {
        val b = budget(e); val key = "e:$id"; val last = b.recent[key]
        val sustained = listOf("pulse", "_tick", "trail", "hover", "shimmer", "unstable", "breath").any { id.contains(it) }
        if (last != null && e.tickCount - last < if (sustained) 8 else 2) return false
        if (b.recent.size >= 64) b.recent.clear()
        b.recent[key] = e.tickCount; return true
    }
    fun volume(value: Float) = value.coerceIn(0f, if (quality() == Quality.MINIMAL) .4f else .6f)
    fun mrpg(d: EnhancedFxDefinitions.MrpgDefinition) = d.copy(
        intensity = d.intensity.coerceIn(0f, if (quality() == Quality.MINIMAL) .12f else .22f),
        radius = d.radius.coerceIn(.15f, 1.6f),
        duration = minOf(d.duration, when (quality()) { Quality.MINIMAL -> 4; Quality.STANDARD -> 6; Quality.CINEMATIC -> 8 }))
}
