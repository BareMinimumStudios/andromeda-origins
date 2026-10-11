package andromeda.origins.util




 /**
 * Shared guard for the Arachne cobweb power recipe quick-move exploit.
 *
 * Keep this helper outside the configured mixin package. Sponge Mixin treats every class under
 * andromeda.origins.mixin as mixin-owned and rejects ordinary runtime class loads from that package.
 */
object ArachneCraftingSafety {
    fun shouldBlockQuickMove(
    player: net.minecraft.world.entity.player.Player,
    slotId: kotlin.Int,
    input: net.minecraft.world.inventory.CraftingContainer,
    result: net.minecraft.world.inventory.ResultContainer
    ): kotlin.Boolean {
        if (slotId != 0 || !player.getTags().contains("arachne")) {
            return false
        }

        val output: net.minecraft.world.item.ItemStack = result.getItem(0)
        if (!output.`is`(net.minecraft.world.item.Items.COBWEB)) {
            return false
        }

        var stringSlots: kotlin.Int = 0
        var occupied: kotlin.Int = 0
        for (i in 0 ..< input.getContainerSize()) {
            val stack: net.minecraft.world.item.ItemStack = input.getItem(i)
            if (stack.isEmpty()) {
                continue
            }
            occupied++
            if (stack.`is`(net.minecraft.world.item.Items.STRING)) {
                stringSlots++
            }
        }

                // Guard exactly the Arachne power recipe (2 string -> cobweb). Normal click crafting still
        // consumes the ingredients through vanilla/Apoli; only the broken pre.2 quick-move path is blocked.
        return occupied == 2 && stringSlots == 2
    }
}
