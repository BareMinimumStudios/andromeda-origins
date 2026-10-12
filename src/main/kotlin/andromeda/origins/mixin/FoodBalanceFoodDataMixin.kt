package andromeda.origins.mixin

import andromeda.origins.compat.FoodBalanceCompat
import andromeda.origins.config.AndromedaConfigRuntime
import net.minecraft.world.food.FoodData
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable

@Mixin(FoodData::class)
abstract class FoodBalanceFoodDataMixin {
    @ModifyVariable(method = ["eat(IF)V"], at = At("HEAD"), argsOnly = true, ordinal = 0)
    private fun `andromeda$balancePlacedNutrition`(nutrition: Int): Int =
        if (FoodBalanceCompat.balancingPlacedWellington()) AndromedaConfigRuntime.get().foodBalance.beefWellingtonNutrition else nutrition

    @ModifyVariable(method = ["eat(IF)V"], at = At("HEAD"), argsOnly = true, ordinal = 0)
    private fun `andromeda$balancePlacedSaturation`(saturationModifier: Float): Float =
        if (FoodBalanceCompat.balancingPlacedWellington()) AndromedaConfigRuntime.get().foodBalance.beefWellingtonSaturationModifier else saturationModifier
}
