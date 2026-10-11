package andromeda.origins.mixin

import andromeda.origins.util.ArachneCraftingSafety


@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.inventory.CraftingMenu::class)
abstract class ArachneCraftingTableQuickMoveMixin {

    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    @JvmField val craftSlots: net.minecraft.world.inventory.CraftingContainer? = null
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    @JvmField val resultSlots: net.minecraft.world.inventory.ResultContainer? = null

    @org.spongepowered.asm.mixin.injection.Inject(method = ["quickMoveStack"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) private fun `andromeda$blockArachneCobwebQuickMove`(
    player: net.minecraft.world.entity.player.Player,
    slotId: kotlin.Int,
    cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.item.ItemStack>
    ) {
        if (ArachneCraftingSafety.shouldBlockQuickMove(player, slotId, craftSlots!!, resultSlots!!)) {
            cir.setReturnValue(net.minecraft.world.item.ItemStack.EMPTY)
        }
    }
}
