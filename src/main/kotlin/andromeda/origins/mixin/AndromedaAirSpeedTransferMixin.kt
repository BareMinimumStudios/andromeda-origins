package andromeda.origins.mixin

import andromeda.origins.compat.AndromedaAirSpeedTransferMixinState
import dev.overgrown.apoli.access.ModifierSummaryHolder
import dev.overgrown.apoli.data.ModifierSummary
import dev.overgrown.apoli.power.PowerContainer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Preserve legacy bucket-summed transfer; native total multipliers instead multiply individually. */
@Mixin(value = [LivingEntity::class], priority = 900)
abstract class AndromedaAirSpeedTransferMixin {
    @Unique private var `andromeda$movementSummary`: ModifierSummary? = null
    @Unique private var `andromeda$added`: Double = 0.0
    @Unique private var `andromeda$baseMultiplier`: Double = 0.0
    @Unique private var `andromeda$totalMultiplier`: Double = 0.0

    @Inject(method = ["getFlyingSpeed"], at = [At("RETURN")], cancellable = true)
    fun `andromeda$transferMovementModifiers`(cir: CallbackInfoReturnable<Float>) {
        val entity = this as Any as LivingEntity
        val container = PowerContainer.of(entity) ?: return
        if (container.isEmpty()) return
        if (AndromedaAirSpeedTransferMixinState.`ANDROMEDA$TRANSFER_OWNERS`.none {
                container.hasPower(it) && !container.isSuppressed(it)
            }) return
        val movement = entity.getAttribute(Attributes.MOVEMENT_SPEED) ?: return
        // Opoli invalidates this immutable summary whenever AttributeInstance becomes dirty.
        // Keep entity-local sums rather than introducing shared client/server cache maps.
        val summary = (movement as? ModifierSummaryHolder)?.`apoli$modifierSummary`()
        if (summary == null || summary !== `andromeda$movementSummary`) {
            var added = 0.0
            var base = 0.0
            var total = 0.0
            for (modifier in movement.modifiers) {
                when (modifier.operation()) {
                    AttributeModifier.Operation.ADD_VALUE -> added += modifier.amount()
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE -> base += modifier.amount()
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL -> total += modifier.amount()
                }
            }
            `andromeda$added` = added
            `andromeda$baseMultiplier` = base
            `andromeda$totalMultiplier` = total
            `andromeda$movementSummary` = summary
        }
        cir.returnValue = ((cir.returnValueF + `andromeda$added`) *
            (1.0 + `andromeda$baseMultiplier`) * (1.0 + `andromeda$totalMultiplier`)).toFloat()
    }
}
