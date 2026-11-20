package andromeda.origins.client.config

import me.fzzyhmstrs.fzzy_config.annotations.RootConfig
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.minecraft.resources.ResourceLocation

/** Local preferences are deliberately never synchronized from the server. */
@RootConfig
class DawnbreakerVisualConfig : Config(ResourceLocation.parse("andromeda_origins:dawnbreaker_visuals")) {
    enum class Intensity { LOW, STANDARD, STRONG }
    var afterimages = ValidatedBoolean(true)
    var impactFrames = ValidatedBoolean(false)
    var intensity = ValidatedEnum(Intensity.LOW)
    var durationMs = ValidatedInt(80, 140, 35)
}
