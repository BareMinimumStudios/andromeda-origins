package andromeda.origins.compat

import andromeda.origins.platform.Platform
import andromeda.origins.platform.RespawnCallback
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer


 /** Keeps Andromeda Figura boolean hook resources from getting stuck across respawns.  */
object FiguraCompatManager {
    val MORTAL_RESOLVE_RESOURCE: ResourceLocation = FiguraCompatManager.id("humanity/figura_mortal_resolve")

    val HOOK_RESOURCES: Array<ResourceLocation> = arrayOf<ResourceLocation>(FiguraCompatManager.id("common/figura_1"),
    FiguraCompatManager.id("common/figura_2"),
    FiguraCompatManager.id("common/figura_3"),
    FiguraCompatManager.id("common/figura_4"),
    FiguraCompatManager.id("common/figura_5"),
    FiguraCompatManager.MORTAL_RESOLVE_RESOURCE
    )

    private fun id(path: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath("andromeda_origins", path)
    }

    fun register() {
        Platform.respawn(RespawnCallback callback@ { oldPlayer: ServerPlayer, newPlayer: ServerPlayer, alive: Boolean -> FiguraCompatManager.resetHooks(newPlayer)})
    }

    fun resetHooks(player: ServerPlayer) {
        for (resource in FiguraCompatManager.HOOK_RESOURCES) {
            FiguraCompatManager.setResource(player, resource, 0)
        }
        for (i in 1 .. 5) {
            player.removeTag("andromeda_figura_" + i)
        }
    }

    fun setResource(player: ServerPlayer, resource: ResourceLocation, value: Int) {
        PowerResources.write(PowerContainer.of(player), resource, value.toDouble())
    }
}
