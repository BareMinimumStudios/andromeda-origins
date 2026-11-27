package andromeda.origins.mixin

import andromeda.origins.util.ArachneCraftingSafety
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.inventory.CraftingMenu
import net.minecraft.world.inventory.ResultContainer
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable


@Mixin(CraftingMenu::class)
abstract class ArachneCraftingTableQuickMoveMixin {

    @Shadow @Final
    @JvmField val craftSlots: CraftingContainer? = null
    @Shadow @Final
    @JvmField val resultSlots: ResultContainer? = null

    @Inject(method = ["quickMoveStack"], at = [At("HEAD")], cancellable = true) private fun `andromeda$blockArachneCobwebQuickMove`(
    player: Player,
    slotId: Int,
    cir: CallbackInfoReturnable<ItemStack>
    ) {
        if (ArachneCraftingSafety.shouldBlockQuickMove(player, slotId, craftSlots!!, resultSlots!!)) {
            cir.setReturnValue(ItemStack.EMPTY)
        }
    }
}
