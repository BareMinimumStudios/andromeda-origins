package andromeda.origins.compat

import java.util.ArrayList
import net.minecraft.core.Holder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory




 /**
 * Small status-effect helpers used by Origin abilities.
 *
 * Using the native effect category lets us classify vanilla and modded effects by Minecraft's actual
 * HARMFUL category instead of maintaining a fragile hard-coded list of effect IDs.
 */
object StatusEffectCompat {
     /**
 * Removes only HARMFUL status effects. Beneficial and neutral effects are preserved.
 *
 * @return the number of status effects removed
 */
    fun clearHarmfulEffects(player: ServerPlayer): Int {
        val harmful: MutableList<Holder<MobEffect>> = ArrayList<Holder<MobEffect>>()

        for (instance in player.getActiveEffects()) {
            val effect: Holder<MobEffect> = instance.getEffect()
            if (effect.value().getCategory() == MobEffectCategory.HARMFUL) {
                harmful.add(effect)
            }
        }

        var removed: Int = 0
        for (effect in harmful) {
            if (player.removeEffect(effect)) {
                removed++
            }
        }
        return removed
    }
}
