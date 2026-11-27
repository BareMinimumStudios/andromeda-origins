package andromeda.origins.mixin

import dev.overgrown.origins.client.screen.ViewOriginScreen
import dev.overgrown.origins.origin.Origin
import dev.overgrown.origins.origin.OriginLayer
import java.util.ArrayList
import net.minecraft.client.gui.components.Button
import net.minecraft.util.Tuple
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker




@Mixin(ViewOriginScreen::class)
interface ViewOriginScreenAccessor {
    @Accessor("originLayers") fun `andromeda$getOriginLayers`(): ArrayList<Tuple<OriginLayer, Origin>>

    @Accessor("currentLayer") fun `andromeda$getCurrentLayerIndex`(): Int

    @Accessor("currentLayer") fun `andromeda$setCurrentLayerIndex`(index: Int)

    @Accessor("chooseOriginButton") fun `andromeda$setChooseOriginButton`(button: Button?)

    @Accessor("swapButton") fun `andromeda$setSwapButton`(button: Button?)

    @Invoker("refreshChooseButton") fun `andromeda$invokeRefreshChooseButton`()

    @Invoker("refreshSwapButton") fun `andromeda$invokeRefreshSwapButton`()
}
