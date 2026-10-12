package andromeda.origins.compat

import andromeda.origins.config.AndromedaConfigRuntime
import andromeda.origins.config.FoodBalanceState
import andromeda.origins.platform.Platform
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

object FoodBalanceCompat {
    private val wellington = ResourceLocation.parse("candlelight:beef_wellington")
    private val wellingtonBlock = ResourceLocation.parse("candlelight:beef_wellington_block")
    private val farmers = ResourceLocation.parse("farm_and_charm:farmers_blessing")
    private val grandmas = ResourceLocation.parse("farm_and_charm:grandmas_blessing")
    private val stoutheart = ResourceLocation.parse("brewery:stoutheart")
    private val blessingIds = listOf(farmers, grandmas, stoutheart)
    private val currentBlock = ThreadLocal<ResourceLocation>()

    fun register() {
        Platform.serverTick { server ->
            if (server.tickCount % 20 == 0) clearDisabledEffects(server.playerList.players)
        }
    }

    fun clearDisabledEffects(players: List<ServerPlayer>) {
        val policy = AndromedaConfigRuntime.get().foodBalance
        for (id in blessingIds) {
            if (!blocked(id, policy)) continue
            val effect = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null) ?: continue
            for (player in players) player.removeEffect(effect)
        }
    }

    private fun blocked(id: ResourceLocation?, policy: FoodBalanceState): Boolean = when (id) {
        farmers -> policy.disableFarmersBlessing
        grandmas -> policy.disableGrandmasBlessing
        stoutheart -> policy.disableStoutheart
        else -> false
    }

    fun blocks(effect: MobEffectInstance) = blocked(BuiltInRegistries.MOB_EFFECT.getKey(effect.effect.value()),
        AndromedaConfigRuntime.get().foodBalance)

    fun applies(stack: ItemStack): Boolean = !stack.isEmpty &&
        BuiltInRegistries.ITEM.getKey(stack.item) == wellington

    /** Immutable overlay; preserves effects, eating time, containers, and unrelated/custom components. */
    fun components(original: DataComponentMap, policy: FoodBalanceState): DataComponentMap {
        if (!policy.balanceBeefWellington) return original
        val originalFood = original.get(DataComponents.FOOD) ?: return original
        val overrides = DataComponentMap.builder().set(DataComponents.FOOD, food(originalFood, policy)).build()
        return DataComponentMap.composite(original, overrides)
    }

    fun food(original: FoodProperties, policy: FoodBalanceState) = FoodProperties(
        policy.beefWellingtonNutrition, policy.beefWellingtonNutrition * policy.beefWellingtonSaturationModifier * 2,
        original.canAlwaysEat(), original.eatSeconds(), original.usingConvertsTo(), original.effects())

    fun <T> withBlock(id: ResourceLocation, action: () -> T): T {
        val previous = currentBlock.get()
        currentBlock.set(id)
        try { return action() } finally {
            if (previous == null) currentBlock.remove() else currentBlock.set(previous)
        }
    }

    fun balancingPlacedWellington(): Boolean = currentBlock.get() == wellingtonBlock &&
        AndromedaConfigRuntime.get().foodBalance.balanceBeefWellington
}
