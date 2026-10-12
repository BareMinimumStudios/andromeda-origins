package andromeda.origins.mixin

import andromeda.origins.compat.FoodBalanceCompat
import andromeda.origins.config.AndromedaConfigRuntime
import andromeda.origins.config.FoodBalanceState
import net.minecraft.core.component.DataComponentMap
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(ItemStack::class)
abstract class FoodBalanceItemStackMixin {
    @Unique private var `andromeda$foodPolicy`: FoodBalanceState? = null
    @Unique private var `andromeda$baseComponents`: DataComponentMap? = null
    @Unique private var `andromeda$balancedComponents`: DataComponentMap? = null
    @Unique private var `andromeda$baseFood`: net.minecraft.world.food.FoodProperties? = null

    @Inject(method = ["getComponents"], at = [At("RETURN")], cancellable = true)
    private fun `andromeda$balanceFood`(cir: CallbackInfoReturnable<DataComponentMap>) {
        val stack = this as Any as ItemStack
        if (!FoodBalanceCompat.applies(stack)) return
        val original = cir.returnValue
        val policy = AndromedaConfigRuntime.get().foodBalance
        val food = original.get(net.minecraft.core.component.DataComponents.FOOD)
        if (`andromeda$foodPolicy` != policy || `andromeda$baseComponents` !== original
            || `andromeda$baseFood` !== food) {
            `andromeda$balancedComponents` = FoodBalanceCompat.components(original, policy)
            `andromeda$foodPolicy` = policy
            `andromeda$baseComponents` = original
            `andromeda$baseFood` = food
        }
        cir.returnValue = `andromeda$balancedComponents`
    }
}
