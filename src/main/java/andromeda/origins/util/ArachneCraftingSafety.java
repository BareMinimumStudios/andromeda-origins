package andromeda.origins.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Shared guard for the Arachne cobweb power recipe quick-move exploit.
 *
 * Keep this helper outside the configured mixin package. Sponge Mixin treats every class under
 * andromeda.origins.mixin as mixin-owned and rejects ordinary runtime class loads from that package.
 */
public final class ArachneCraftingSafety {

    private ArachneCraftingSafety() {}

    public static boolean shouldBlockQuickMove(
        PlayerEntity player,
        int slotId,
        RecipeInputInventory input,
        CraftingResultInventory result
    ) {
        if (slotId != 0 || !player.getCommandTags().contains("arachne")) {
            return false;
        }

        ItemStack output = result.getStack(0);
        if (!output.isOf(Items.COBWEB)) {
            return false;
        }

        int stringSlots = 0;
        int occupied = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            occupied++;
            if (stack.isOf(Items.STRING)) {
                stringSlots++;
            }
        }

        // Guard exactly the Arachne power recipe (2 string -> cobweb). Normal click crafting still
        // consumes the ingredients through vanilla/Apoli; only the broken pre.2 quick-move path is blocked.
        return occupied == 2 && stringSlots == 2;
    }
}
