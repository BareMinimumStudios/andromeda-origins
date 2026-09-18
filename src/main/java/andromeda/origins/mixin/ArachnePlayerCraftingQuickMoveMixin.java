package andromeda.origins.mixin;

import andromeda.origins.util.ArachneCraftingSafety;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerScreenHandler.class)
public abstract class ArachnePlayerCraftingQuickMoveMixin {

    @Shadow @Final private RecipeInputInventory craftingInput;
    @Shadow @Final private CraftingResultInventory craftingResult;

    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void andromeda$blockArachneCobwebQuickMove(
        PlayerEntity player,
        int slotId,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        if (ArachneCraftingSafety.shouldBlockQuickMove(player, slotId, craftingInput, craftingResult)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
