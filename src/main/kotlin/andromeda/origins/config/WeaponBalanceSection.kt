package andromeda.origins.config

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean

/** Optional weapon implicit policy. Unique abilities and vanilla effects remain independent. */
class WeaponBalanceSection : ConfigSection() {
    @Comment("Enable Simply Swords / Simply More implicit compatibility. Turn off to restore all upstream implicit behavior. Applies to new hits immediately; existing effects expire normally.")
    var enabled = ValidatedBoolean(true)
    @Comment("Disable the shared Simply Swords Bleed implicit used by glaives, greataxes, halberds and addon weapons mapped to them. Does not disable Bleed from unique abilities.")
    var disableBleedImplicits = ValidatedBoolean(true)
    @Comment("Disable Simply More's Disarm implicit (implicit_mining_fatigue) on khopeshes and deer horns. Does not disable vanilla Mining Fatigue or Slowness.")
    var disableAttackSpeedImplicits = ValidatedBoolean(true)
    @Comment("Optionally disable Simply More's Pernach Stun implicit. Defaults off; unique stun abilities are unchanged.")
    var disableStunImplicits = ValidatedBoolean(false)

    fun toState() = WeaponBalanceState(enabled.get(), disableBleedImplicits.get(),
        disableAttackSpeedImplicits.get(), disableStunImplicits.get())
}

data class WeaponBalanceState(
    val enabled: Boolean = true,
    val disableBleedImplicits: Boolean = true,
    val disableAttackSpeedImplicits: Boolean = true,
    val disableStunImplicits: Boolean = false
)
