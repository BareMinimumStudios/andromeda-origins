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
        val b = budget(e); val limit = when (quality()) { Quality.MINIMAL -> 12; Quality.STANDARD -> 48; Quality.CINEMATIC -> 64 }
        val remaining = limit - b.particles
        if (remaining <= 0) return null
        // Zero is the native directed-single-particle mode, not an empty burst.
        if (requested == 0) { b.particles++; return 0 }
        val factor = when (quality()) { Quality.MINIMAL -> .45; Quality.STANDARD -> .85; Quality.CINEMATIC -> 1.0 }
        val burst = when (quality()) { Quality.MINIMAL -> 6; Quality.STANDARD -> 12; Quality.CINEMATIC -> 16 }
        val n = kotlin.math.ceil(requested.coerceAtLeast(1) * factor).toInt().coerceIn(1, minOf(burst, remaining))
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
    fun sustained(event: String) = listOf("pulse", "_tick", "trail", "hover", "shimmer", "unstable", "breath").any { event.contains(it) }
    fun layers() = if (quality() == Quality.MINIMAL) 1 else 4
    fun particleScaleLimit() = when (quality()) { Quality.MINIMAL -> .7f; Quality.STANDARD -> 1.8f; Quality.CINEMATIC -> 2.2f }
    fun volume(value: Float) = value.coerceIn(0f, if (quality() == Quality.MINIMAL) .4f else .6f)
    fun mrpg(d: EnhancedFxDefinitions.MrpgDefinition) = d.copy(
        intensity = d.intensity.coerceIn(0f, when (quality()) { Quality.MINIMAL -> .12f; Quality.STANDARD -> .4f; Quality.CINEMATIC -> .55f }),
        radius = d.radius.coerceIn(.15f, if (quality() == Quality.MINIMAL) 1.1f else 2.4f),
        duration = minOf(d.duration, when (quality()) { Quality.MINIMAL -> 4; Quality.STANDARD -> 10; Quality.CINEMATIC -> 14 }))
}
