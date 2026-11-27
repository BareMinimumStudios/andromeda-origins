package andromeda.origins.config

import andromeda.origins.config.AndromedaConfigPolicy
import andromeda.origins.config.AndromedaConfigState
import kotlin.concurrent.Volatile
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity


 /** Shared immutable snapshot used by server gameplay and the synced client selection UI.  */
object AndromedaConfigRuntime {
    @Volatile
    var state: AndromedaConfigState = AndromedaConfigState.Companion.DEFAULT

    fun get(): AndromedaConfigState {
        return AndromedaConfigRuntime.state
    }

    fun set(next: AndromedaConfigState?) {
        AndromedaConfigRuntime.state = if (next == null) AndromedaConfigState.Companion.DEFAULT else next
    }

    fun originEnabled(id: ResourceLocation): Boolean {
        return AndromedaConfigPolicy.isOriginEnabled(id, AndromedaConfigRuntime.state)
    }

    fun filterPrimaryLayer(ids: MutableList<ResourceLocation>): MutableList<ResourceLocation> {
        return AndromedaConfigPolicy.filterPrimaryLayer(ids, AndromedaConfigRuntime.state)
    }

    fun shouldSuppressPower(owner: Entity, power: ResourceLocation): Boolean {
        return AndromedaConfigPolicy.shouldSuppressPower(owner, power, AndromedaConfigRuntime.state)
    }
}
