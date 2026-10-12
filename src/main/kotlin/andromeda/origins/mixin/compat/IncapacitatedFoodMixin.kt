package andromeda.origins.mixin.compat

import andromeda.origins.config.AndromedaConfigRuntime
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** Cancels only Incapacitated's food hook; vanilla consumption and effects still run. */
@Pseudo
@Mixin(targets = ["com.cartoonishvillain.incapacitated.events.AbstractedIncapacitation"], remap = false)
object IncapacitatedFoodMixin {
    @JvmStatic
    @Inject(method = ["eat"], at = [At("HEAD")], cancellable = true, remap = false, require = 1)
    private fun `andromeda_origins$blockFoodBypass`(entity: LivingEntity, stack: ItemStack, ci: CallbackInfo) {
        if (AndromedaConfigRuntime.get().blockIncapacitatedFoodBypass
            && (stack.`is`(Items.GOLDEN_APPLE) || stack.`is`(Items.ENCHANTED_GOLDEN_APPLE)
                || stack.`is`(Items.GOLDEN_CARROT))) ci.cancel()
    }
}
