package andromeda.origins.compat

import andromeda.origins.platform.ReloadCallback


 /**
 * Warns when Overgrown Origins' per-world registry filter is also disabling Andromeda content.
 *
 *
 * Andromeda's Fzzy Config uses non-destructive power suppression. The upstream world config runs
 * earlier and can remove Origins or powers from the registry entirely, so Fzzy cannot re-enable
 * entries that were disabled there. We intentionally do not overwrite the upstream file.
 */
object OriginsWorldConfigWarning {
    val LOGGER: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("Andromeda Origins/Origins World Config")

    fun register() {
        andromeda.origins.platform.Platform.serverStarted(java.util.function.Consumer callback@ { obj: net.minecraft.server.MinecraftServer -> OriginsWorldConfigWarning.check(obj)})
        andromeda.origins.platform.Platform.reload(ReloadCallback callback@ { server: net.minecraft.server.MinecraftServer, resourceManager: net.minecraft.server.packs.resources.ResourceManager, success: kotlin.Boolean -> if (success) OriginsWorldConfigWarning.check(server)
        })
    }

    private fun check(server: net.minecraft.server.MinecraftServer) {
        val path: java.nio.file.Path = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("origins/config.json")
        if (!java.nio.file.Files.isRegularFile(path)) return

        try {
            val parsed: com.google.gson.JsonElement = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(path))
            if (!parsed.isJsonObject()) return
            val root: com.google.gson.JsonObject = parsed.getAsJsonObject()
            val disabledOrigins: kotlin.Int = OriginsWorldConfigWarning.disabledAndromedaOrigins(root)
            val disabledPowers: kotlin.Int = OriginsWorldConfigWarning.disabledAndromedaPowers(root)
            val layerBlocks: kotlin.Int = OriginsWorldConfigWarning.disabledAndromedaLayerEntries(root)
            if (disabledOrigins == 0 && disabledPowers == 0 && layerBlocks == 0) return

            OriginsWorldConfigWarning.LOGGER.warn(
            ("Overgrown Origins world config {} also disables Andromeda content "
            + "(Origins={}, powers={}, layer entries={}). These registry-level settings run "
            + "before Andromeda's Fzzy Config and cannot be overridden there. Prefer Fzzy "
            + "Config for Andromeda Origins unless this double-filtering is intentional."),
            path, disabledOrigins, disabledPowers, layerBlocks
            )
        }catch (exception: java.lang.Exception) {
            OriginsWorldConfigWarning.LOGGER.warn("Could not inspect Overgrown Origins world config {} for Andromeda conflicts.", path, exception)
        }
    }

    private fun disabledAndromedaOrigins(root: com.google.gson.JsonObject): kotlin.Int {
        val origins: com.google.gson.JsonObject? = OriginsWorldConfigWarning.`object`(root, "origins")
        if (origins == null) return 0
        var count: kotlin.Int = 0
        for (entry in origins.entrySet()) {
            if (!OriginsWorldConfigWarning.isAndromeda(entry.key) || !entry.value.isJsonObject()) continue
            val toggle: com.google.gson.JsonObject = entry.value.getAsJsonObject()
            val enabled: com.google.gson.JsonElement? = toggle.get("enabled")
            if (enabled != null && enabled.isJsonPrimitive() && !enabled.getAsBoolean()) count++
        }
        return count
    }

    private fun disabledAndromedaPowers(root: com.google.gson.JsonObject): kotlin.Int {
        val origins: com.google.gson.JsonObject? = OriginsWorldConfigWarning.`object`(root, "origins")
        if (origins == null) return 0
        var count: kotlin.Int = 0
        for (entry in origins.entrySet()) {
            if (!OriginsWorldConfigWarning.isAndromeda(entry.key) || !entry.value.isJsonObject()) continue
            val powers: com.google.gson.JsonObject? = OriginsWorldConfigWarning.`object`(entry.value.getAsJsonObject(), "powers")
            if (powers == null) continue
            for (power in powers.entrySet()) {
                if (power.value.isJsonPrimitive() && !power.value.getAsBoolean()) count++
            }
        }
        return count
    }

    private fun disabledAndromedaLayerEntries(root: com.google.gson.JsonObject): kotlin.Int {
        val layers: com.google.gson.JsonObject? = OriginsWorldConfigWarning.`object`(root, "layers")
        if (layers == null) return 0
        var count: kotlin.Int = 0
        for (layer in layers.entrySet()) {
            if (!layer.value.isJsonObject()) continue
            val origins: com.google.gson.JsonObject? = OriginsWorldConfigWarning.`object`(layer.value.getAsJsonObject(), "origins")
            if (origins == null) continue
            for (origin in origins.entrySet()) {
                if (OriginsWorldConfigWarning.isAndromeda(origin.key) && origin.value.isJsonPrimitive()
                && !origin.value.getAsBoolean()) count++
            }
        }
        return count
    }

    private fun `object`(parent: com.google.gson.JsonObject, key: kotlin.String?): com.google.gson.JsonObject? {
        val value: com.google.gson.JsonElement? = parent.get(key)
        return if (value != null && value.isJsonObject()) value.getAsJsonObject() else null
    }

    private fun isAndromeda(id: kotlin.String?): kotlin.Boolean {
        return id != null && id.startsWith("andromeda_origins:")
    }
}
