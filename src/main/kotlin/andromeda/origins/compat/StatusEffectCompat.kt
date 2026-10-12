package andromeda.origins.compat




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
    fun clearHarmfulEffects(player: net.minecraft.server.level.ServerPlayer): kotlin.Int {
        val harmful: kotlin.collections.MutableList<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> = java.util.ArrayList<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>>()

        for (instance in player.getActiveEffects()) {
            val effect: net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> = instance.getEffect()
            if (effect.value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                harmful.add(effect)
            }
        }

        var removed: kotlin.Int = 0
        for (effect in harmful) {
            if (player.removeEffect(effect)) {
                removed++
            }
        }
        return removed
    }
}
