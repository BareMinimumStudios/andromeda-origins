package andromeda.origins.compat;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Small status-effect helpers used by Origin abilities.
 *
 * Keeping this in Java lets us classify vanilla and modded effects by Minecraft's actual
 * HARMFUL category instead of maintaining a fragile hard-coded list of effect IDs.
 */
public final class StatusEffectCompat {

    private StatusEffectCompat() {}

    /**
     * Removes only HARMFUL status effects. Beneficial and neutral effects are preserved.
     *
     * @return the number of status effects removed
     */
    public static int clearHarmfulEffects(ServerPlayerEntity player) {
        List<RegistryEntry<StatusEffect>> harmful = new ArrayList<>();

        for (StatusEffectInstance instance : player.getStatusEffects()) {
            RegistryEntry<StatusEffect> effect = instance.getEffectType();
            if (effect.value().getCategory() == StatusEffectCategory.HARMFUL) {
                harmful.add(effect);
            }
        }

        int removed = 0;
        for (RegistryEntry<StatusEffect> effect : harmful) {
            if (player.removeStatusEffect(effect)) {
                removed++;
            }
        }
        return removed;
    }
}
