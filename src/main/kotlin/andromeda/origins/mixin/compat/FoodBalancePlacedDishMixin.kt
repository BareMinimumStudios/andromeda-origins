package andromeda.origins.mixin.compat

import andromeda.origins.compat.FoodBalanceCompat
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo

/** Current Let's Do dishes use Foundation's private consumption method. */
@Pseudo
@Mixin(targets = ["net.satisfy.foundation.food.IngredientEffectFoodBlock",
    "net.satisfy.foundation.food.FoodBlock"], remap = false)
abstract class FoodBalancePlacedDishMixin {
    @WrapMethod(method = ["tryEat"], remap = false, require = 0)
    private fun `andromeda$placedDishContext`(level: LevelAccessor, pos: BlockPos, state: BlockState,
        player: Player, original: Operation<InteractionResult>): InteractionResult =
        FoodBalanceCompat.withBlock(BuiltInRegistries.BLOCK.getKey(state.block)) {
            original.call(level, pos, state, player)
        }
}
