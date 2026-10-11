package andromeda.origins.compat

import andromeda.origins.platform.RespawnCallback


 /** Keeps Andromeda Figura boolean hook resources from getting stuck across respawns.  */
object FiguraCompatManager {
    val MORTAL_RESOLVE_RESOURCE: net.minecraft.resources.ResourceLocation = FiguraCompatManager.id("humanity/figura_mortal_resolve")

    val HOOK_RESOURCES: kotlin.Array<net.minecraft.resources.ResourceLocation> = kotlin.arrayOf<net.minecraft.resources.ResourceLocation>(FiguraCompatManager.id("common/figura_1"),
    FiguraCompatManager.id("common/figura_2"),
    FiguraCompatManager.id("common/figura_3"),
    FiguraCompatManager.id("common/figura_4"),
    FiguraCompatManager.id("common/figura_5"),
    FiguraCompatManager.MORTAL_RESOLVE_RESOURCE
    )

    private fun id(path: kotlin.String): net.minecraft.resources.ResourceLocation {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("andromeda_origins", path)
    }

    fun register() {
        andromeda.origins.platform.Platform.respawn(RespawnCallback callback@ { oldPlayer: net.minecraft.server.level.ServerPlayer, newPlayer: net.minecraft.server.level.ServerPlayer, alive: kotlin.Boolean -> FiguraCompatManager.resetHooks(newPlayer)})
    }

    fun resetHooks(player: net.minecraft.server.level.ServerPlayer) {
        for (resource in FiguraCompatManager.HOOK_RESOURCES) {
            FiguraCompatManager.setResource(player, resource, 0)
        }
        for (i in 1 .. 5) {
            player.removeTag("andromeda_figura_" + i)
        }
    }

    fun setResource(player: net.minecraft.server.level.ServerPlayer, resource: net.minecraft.resources.ResourceLocation, value: kotlin.Int) {
        dev.overgrown.apoli.power.PowerResources.write(dev.overgrown.apoli.power.PowerContainer.of(player), resource, value)
    }
}
