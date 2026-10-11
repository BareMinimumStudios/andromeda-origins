package andromeda.origins.mixin




@org.spongepowered.asm.mixin.Mixin(dev.overgrown.origins.client.screen.ChooseOriginScreen::class)
interface ChooseOriginScreenAccessor {
    @org.spongepowered.asm.mixin.gen.Accessor("originSelection") fun `andromeda$getOriginSelection`(): kotlin.collections.MutableList<dev.overgrown.origins.origin.Origin>

    @org.spongepowered.asm.mixin.gen.Accessor("currentOrigin") fun `andromeda$getCurrentOriginIndex`(): kotlin.Int

    @org.spongepowered.asm.mixin.gen.Accessor("currentOrigin") fun `andromeda$setCurrentOriginIndex`(index: kotlin.Int)

    @org.spongepowered.asm.mixin.gen.Accessor("maxSelection") fun `andromeda$getMaxSelection`(): kotlin.Int

    @org.spongepowered.asm.mixin.gen.Accessor("maxSelection") fun `andromeda$setMaxSelection`(value: kotlin.Int)

    @org.spongepowered.asm.mixin.gen.Accessor("layerList") fun `andromeda$getLayerList`(): java.util.ArrayList<dev.overgrown.origins.origin.OriginLayer>

    @org.spongepowered.asm.mixin.gen.Accessor("currentLayerIndex") fun `andromeda$getCurrentLayerIndex`(): kotlin.Int

    @org.spongepowered.asm.mixin.gen.Accessor("fromOrb") fun `andromeda$isFromOrb`(): kotlin.Boolean

    @org.spongepowered.asm.mixin.gen.Invoker("getCurrentOriginInternal") fun `andromeda$invokeGetCurrentOriginInternal`(): dev.overgrown.origins.origin.Origin?

    @org.spongepowered.asm.mixin.gen.Invoker("openNextLayerScreen") fun `andromeda$invokeOpenNextLayerScreen`()
}
