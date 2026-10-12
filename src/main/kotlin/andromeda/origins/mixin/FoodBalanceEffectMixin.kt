package andromeda.origins.mixin

import andromeda.origins.compat.FoodBalanceCompat
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffectInstance
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(LivingEntity::class)
abstract class FoodBalanceEffectMixin {
    @Inject(method = ["addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"],
        at = [At("HEAD")], cancellable = true)
    private fun `andromeda$blockBlessings`(effect: MobEffectInstance, source: Entity?, cir: CallbackInfoReturnable<Boolean>) {
        val entity = this as Any as LivingEntity
        if (!entity.level().isClientSide && FoodBalanceCompat.blocks(effect)) cir.returnValue = false
    }
}
