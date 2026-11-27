package andromeda.origins.mixin

import dev.overgrown.origins.client.screen.ChooseOriginScreen
import dev.overgrown.origins.origin.Origin
import dev.overgrown.origins.origin.OriginLayer
import java.util.ArrayList
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker




@Mixin(ChooseOriginScreen::class)
interface ChooseOriginScreenAccessor {
    @Accessor("originSelection") fun `andromeda$getOriginSelection`(): MutableList<Origin>

    @Accessor("currentOrigin") fun `andromeda$getCurrentOriginIndex`(): Int

    @Accessor("currentOrigin") fun `andromeda$setCurrentOriginIndex`(index: Int)

    @Accessor("maxSelection") fun `andromeda$getMaxSelection`(): Int

    @Accessor("maxSelection") fun `andromeda$setMaxSelection`(value: Int)

    @Accessor("layerList") fun `andromeda$getLayerList`(): ArrayList<OriginLayer>

    @Accessor("currentLayerIndex") fun `andromeda$getCurrentLayerIndex`(): Int

    @Accessor("fromOrb") fun `andromeda$isFromOrb`(): Boolean

    @Invoker("getCurrentOriginInternal") fun `andromeda$invokeGetCurrentOriginInternal`(): Origin?

    @Invoker("openNextLayerScreen") fun `andromeda$invokeOpenNextLayerScreen`()
}
