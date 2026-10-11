package andromeda.origins.config

import andromeda.origins.config.AndromedaConfigPolicy
import andromeda.origins.config.AndromedaConfigState


 /** Shared immutable snapshot used by server gameplay and the synced client selection UI.  */
object AndromedaConfigRuntime {
    @kotlin.concurrent.Volatile
    var state: AndromedaConfigState = AndromedaConfigState.Companion.DEFAULT

    fun get(): AndromedaConfigState {
        return AndromedaConfigRuntime.state
    }

    fun set(next: AndromedaConfigState?) {
        AndromedaConfigRuntime.state = if (next == null) AndromedaConfigState.Companion.DEFAULT else next
    }

    fun originEnabled(id: net.minecraft.resources.ResourceLocation): kotlin.Boolean {
        return AndromedaConfigPolicy.isOriginEnabled(id, AndromedaConfigRuntime.state)
    }

    fun filterPrimaryLayer(ids: kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation>): kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation> {
        return AndromedaConfigPolicy.filterPrimaryLayer(ids, AndromedaConfigRuntime.state)
    }

    fun shouldSuppressPower(owner: net.minecraft.world.entity.Entity, power: net.minecraft.resources.ResourceLocation): kotlin.Boolean {
        return AndromedaConfigPolicy.shouldSuppressPower(owner, power, AndromedaConfigRuntime.state)
    }
}
