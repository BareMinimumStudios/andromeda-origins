package andromeda.origins.config

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat

/** Server policy, synchronized by the existing gameplay config. */
class FoodBalanceSection : ConfigSection() {
    @Comment("Disable Farmer's Blessing, including effects already active on players.")
    var disableFarmersBlessing = ValidatedBoolean(true)
    @Comment("Disable Grandma's Blessing, including effects already active on players.")
    var disableGrandmasBlessing = ValidatedBoolean(true)
    @Comment("Disable Stoutheart, including effects already active on players.")
    var disableStoutheart = ValidatedBoolean(true)
    @Comment("Override Beef Wellington nutrition and saturation, including placed dishes. Disable to use the other mods' values.")
    var balanceBeefWellington = ValidatedBoolean(true)
    @Comment("Hunger points restored per Beef Wellington serving. Two points equal one hunger icon.")
    var beefWellingtonNutrition = ValidatedInt(6, 0..20)
    @Comment("Saturation modifier; restored saturation is nutrition times modifier times two.")
    var beefWellingtonSaturationModifier = ValidatedFloat(0.5f, 2.0f, 0.0f)
    fun toState() = FoodBalanceState(disableFarmersBlessing.get(), disableGrandmasBlessing.get(),
        disableStoutheart.get(), balanceBeefWellington.get(), beefWellingtonNutrition.get(),
        beefWellingtonSaturationModifier.get())
}

data class FoodBalanceState(
    val disableFarmersBlessing: Boolean = true,
    val disableGrandmasBlessing: Boolean = true,
    val disableStoutheart: Boolean = true,
    val balanceBeefWellington: Boolean = true,
    val beefWellingtonNutrition: Int = 6,
    val beefWellingtonSaturationModifier: Float = 0.5f
)
