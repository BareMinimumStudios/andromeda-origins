package andromeda.origins.compat

import net.minecraft.server.level.ServerPlayer

/** PAL is optional. Cache its public API without linking Fabric-only classes on NeoForge. */
object VeilFlightCompat {
    private class Api {
        private val pal = Class.forName("io.github.ladysnake.pal.Pal")
        private val sourceClass = Class.forName("io.github.ladysnake.pal.AbilitySource")
        private val vanilla = Class.forName("io.github.ladysnake.pal.VanillaAbilities")
        private val source = pal.getMethod("getAbilitySource", String::class.java, String::class.java)
            .invoke(null, "andromeda_origins", "veil_step")
        private val allow = vanilla.getField("ALLOW_FLYING").get(null)
        private val flying = vanilla.getField("FLYING").get(null)
        private val grant = sourceClass.methods.single { it.name == "grantTo" && it.parameterCount == 2 }
        private val revoke = sourceClass.methods.single { it.name == "revokeFrom" && it.parameterCount == 2 }
        private val grants = sourceClass.methods.single { it.name == "grants" && it.parameterCount == 2 }

        fun enable(player: ServerPlayer) {
            for (ability in arrayOf(allow, flying))
                if (grants.invoke(source, player, ability) != true) grant.invoke(source, player, ability)
        }

        fun disable(player: ServerPlayer) {
            for (ability in arrayOf(flying, allow))
                if (grants.invoke(source, player, ability) == true) revoke.invoke(source, player, ability)
        }
    }

    private val api: Api? by lazy {
        try { Class.forName("io.github.ladysnake.pal.Pal", false, VeilFlightCompat::class.java.classLoader) }
        catch (_: ClassNotFoundException) { return@lazy null }
        Api()
    }

    fun grant(player: ServerPlayer): Boolean {
        val current = api ?: return false
        current.enable(player)
        return true
    }

    fun release(player: ServerPlayer): Boolean {
        val current = api ?: return false
        current.disable(player)
        return true
    }
}
