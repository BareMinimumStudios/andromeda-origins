package andromeda.origins.mixin

import andromeda.origins.compat.AndromedaAirSpeedTransferMixinState




 /**
 * Restores stable Apoli 2.12.0-pre.2 attribute_modify_transfer behavior for Andromeda's
 * five movement -> air-speed transfer sites.
 *
 * Stable copied the currently-active MOVEMENT_SPEED attribute modifiers onto the air-speed
 * calculation, so potion/mod/origin speed modifiers propagated dynamically. Opoli 1.90 no
 * longer provides attribute_modify_transfer; fixed modify_air_speed replacements are not
 * behaviorally equivalent.
 */
@org.spongepowered.asm.mixin.Mixin(value = [net.minecraft.world.entity.LivingEntity::class], priority = 900)
abstract class AndromedaAirSpeedTransferMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["getFlyingSpeed"], at = [org.spongepowered.asm.mixin.injection.At("RETURN")], cancellable = true) fun `andromeda$transferMovementModifiers`(cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<kotlin.Float>) {
        val entity: net.minecraft.world.entity.LivingEntity = this as kotlin.Any as net.minecraft.world.entity.LivingEntity
        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(entity)
        if (container == null || container.isEmpty()) return

        var applies: kotlin.Boolean = false
        for (power in AndromedaAirSpeedTransferMixinState.`ANDROMEDA$TRANSFER_OWNERS`) {
            if (container.hasPower(power) && !container.isSuppressed(power)) {
                applies = true
                break
            }
        }
        if (!applies) return

        val movement: net.minecraft.world.entity.ai.attributes.AttributeInstance? = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
        if (movement == null) return

        val original: kotlin.Float = cir.getReturnValue()

                // Exact Apoli 2.12.0-pre.2 AttributeModifyTransfer semantics:
        // vanilla ADD_VALUE -> ADD_BASE_EARLY,
        // ADD_MULTIPLIED_BASE -> MULTIPLY_BASE_MULTIPLICATIVE,
        // ADD_MULTIPLIED_TOTAL -> MULTIPLY_TOTAL_MULTIPLICATIVE.
        // Each operation bucket is summed before it is applied.
        var addBase: kotlin.Double = 0.0
        var multiplyBase: kotlin.Double = 0.0
        var multiplyTotal: kotlin.Double = 0.0
        for (modifier in movement.getModifiers()) {
            when (modifier.operation()){net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE -> addBase += modifier.amount()
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE -> multiplyBase += modifier.amount()
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL -> multiplyTotal += modifier.amount()}
        }

        var total: kotlin.Double = original + addBase
        total *= 1.0 + multiplyBase
        total *= 1.0 + multiplyTotal

        cir.setReturnValue(total.toFloat())
    }

    }

