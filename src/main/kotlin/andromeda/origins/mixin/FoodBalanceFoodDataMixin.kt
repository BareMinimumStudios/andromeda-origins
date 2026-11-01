package andromeda.origins.mixin

import andromeda.origins.compat.FoodBalanceCompat
import andromeda.origins.config.AndromedaConfigRuntime
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.world.food.FoodData
import org.spongepowered.asm.mixin.Mixin

@Mixin(FoodData::class)
abstract class FoodBalanceFoodDataMixin {
    @WrapMethod(method = ["eat(IF)V"])
    private fun `andromeda$balancePlacedFood`(nutrition: Int, saturationModifier: Float, original: Operation<Void>) {
        if (FoodBalanceCompat.balancingPlacedWellington()) {
            val policy = AndromedaConfigRuntime.get().foodBalance
            original.call(policy.beefWellingtonNutrition, policy.beefWellingtonSaturationModifier)
        } else {
            original.call(nutrition, saturationModifier)
        }
    }
}
