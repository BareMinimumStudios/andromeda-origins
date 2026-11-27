package andromeda.origins.util

import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.inventory.ResultContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items




 /**
 * Shared guard for the Arachne cobweb power recipe quick-move exploit.
 *
 * Keep this helper outside the configured mixin package. Sponge Mixin treats every class under
 * andromeda.origins.mixin as mixin-owned and rejects ordinary runtime class loads from that package.
 */
object ArachneCraftingSafety {
    fun shouldBlockQuickMove(
    player: Player,
    slotId: Int,
    input: CraftingContainer,
    result: ResultContainer
    ): Boolean {
        if (slotId != 0 || !player.getTags().contains("arachne")) {
            return false
        }

        val output: ItemStack = result.getItem(0)
        if (!output.`is`(Items.COBWEB)) {
            return false
        }

        var stringSlots: Int = 0
        var occupied: Int = 0
        for (i in 0 ..< input.getContainerSize()) {
            val stack: ItemStack = input.getItem(i)
            if (stack.isEmpty()) {
                continue
            }
            occupied++
            if (stack.`is`(Items.STRING)) {
                stringSlots++
            }
        }

                // Guard exactly the Arachne power recipe (2 string -> cobweb). Normal click crafting still
        // consumes the ingredients through vanilla/Apoli; only the broken pre.2 quick-move path is blocked.
        return occupied == 2 && stringSlots == 2
    }
}
