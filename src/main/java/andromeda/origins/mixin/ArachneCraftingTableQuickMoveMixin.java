package andromeda.origins.mixin;

import andromeda.origins.util.ArachneCraftingSafety;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingScreenHandler.class)
public abstract class ArachneCraftingTableQuickMoveMixin {

    @Shadow @Final private RecipeInputInventory input;
    @Shadow @Final private CraftingResultInventory result;

    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void andromeda$blockArachneCobwebQuickMove(
        PlayerEntity player,
        int slotId,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        if (ArachneCraftingSafety.shouldBlockQuickMove(player, slotId, input, result)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
