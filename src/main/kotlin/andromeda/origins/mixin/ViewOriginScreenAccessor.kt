package andromeda.origins.mixin




@org.spongepowered.asm.mixin.Mixin(dev.overgrown.origins.client.screen.ViewOriginScreen::class)
interface ViewOriginScreenAccessor {
    @org.spongepowered.asm.mixin.gen.Accessor("originLayers") fun `andromeda$getOriginLayers`(): java.util.ArrayList<net.minecraft.util.Tuple<dev.overgrown.origins.origin.OriginLayer, dev.overgrown.origins.origin.Origin>>

    @org.spongepowered.asm.mixin.gen.Accessor("currentLayer") fun `andromeda$getCurrentLayerIndex`(): kotlin.Int

    @org.spongepowered.asm.mixin.gen.Accessor("currentLayer") fun `andromeda$setCurrentLayerIndex`(index: kotlin.Int)

    @org.spongepowered.asm.mixin.gen.Accessor("chooseOriginButton") fun `andromeda$setChooseOriginButton`(button: net.minecraft.client.gui.components.Button?)

    @org.spongepowered.asm.mixin.gen.Accessor("swapButton") fun `andromeda$setSwapButton`(button: net.minecraft.client.gui.components.Button?)

    @org.spongepowered.asm.mixin.gen.Invoker("refreshChooseButton") fun `andromeda$invokeRefreshChooseButton`()

    @org.spongepowered.asm.mixin.gen.Invoker("refreshSwapButton") fun `andromeda$invokeRefreshSwapButton`()
}
