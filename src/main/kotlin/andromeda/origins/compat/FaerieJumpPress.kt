package andromeda.origins.compat

import net.minecraft.resources.ResourceLocation

/** Classify the press before vanilla movement changes onGround. */
class FaerieJumpPress {
    private var down = false
    private var groundPress = false

    fun sample(jumpDown: Boolean, grounded: Boolean) {
        if (!jumpDown) groundPress = false
        else if (!down) groundPress = grounded
        down = jumpDown
    }

    fun blocks(power: ResourceLocation): Boolean = groundPress && power.namespace == "andromeda_origins" &&
        power.path in setOf("faerie/passives_airjump", "champion/faerie_passives_airjump")
}
