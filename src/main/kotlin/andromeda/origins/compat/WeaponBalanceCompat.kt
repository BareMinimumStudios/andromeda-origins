package andromeda.origins.compat

import andromeda.origins.config.AndromedaConfigRuntime
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.ChatFormatting

/** Hook the implicit handlers, not global status-effect application or every entity tick. */
object WeaponBalanceCompat {
    fun blocksBleed(): Boolean = AndromedaConfigRuntime.get().weaponBalance.let { it.enabled && it.disableBleedImplicits }
    fun blocksFatigue(): Boolean = AndromedaConfigRuntime.get().weaponBalance.let { it.enabled && it.disableAttackSpeedImplicits }
    fun blocksStun(): Boolean = AndromedaConfigRuntime.get().weaponBalance.let { it.enabled && it.disableStunImplicits }

    fun tooltip(original: Component?): Component? {
        if (original == null) return null
        if ((blocksBleed() && contains(original, "tooltip.simplyswords.implicit.bleed")) ||
            (blocksFatigue() && contains(original, "tooltip.simplymore.implicit.disarm")) ||
            (blocksStun() && contains(original, "tooltip.simplymore.implicit.stun")))
            return Component.translatable("andromeda_origins.weapon_implicit.disabled").withStyle(ChatFormatting.GRAY)
        return original
    }

    private fun contains(text: Component, key: String): Boolean {
        val contents = text.contents
        if (contents is TranslatableContents &&
            (contents.key == key || contents.args.any { it is Component && contains(it, key) })) return true
        return text.siblings.any { contains(it, key) }
    }
}
