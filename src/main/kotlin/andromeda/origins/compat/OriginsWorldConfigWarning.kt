package andromeda.origins.compat

import andromeda.origins.platform.Platform
import andromeda.origins.platform.ReloadCallback
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import net.minecraft.server.MinecraftServer
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory


 /**
 * Warns when Overgrown Origins' per-world registry filter is also disabling Andromeda content.
 *
 *
 * Andromeda's Fzzy Config uses non-destructive power suppression. The upstream world config runs
 * earlier and can remove Origins or powers from the registry entirely, so Fzzy cannot re-enable
 * entries that were disabled there. We intentionally do not overwrite the upstream file.
 */
object OriginsWorldConfigWarning {
    val LOGGER: Logger = LoggerFactory.getLogger("Andromeda Origins/Origins World Config")

    fun register() {
        Platform.serverStarted(Consumer callback@ { obj: MinecraftServer -> OriginsWorldConfigWarning.check(obj)})
        Platform.reload(ReloadCallback callback@ { server: MinecraftServer, resourceManager: ResourceManager, success: Boolean -> if (success) OriginsWorldConfigWarning.check(server)
        })
    }

    private fun check(server: MinecraftServer) {
        val path: Path = server.getWorldPath(LevelResource.ROOT).resolve("origins/config.json")
        if (!Files.isRegularFile(path)) return

        try {
            val parsed: JsonElement = JsonParser.parseString(Files.readString(path))
            if (!parsed.isJsonObject()) return
            val root: JsonObject = parsed.getAsJsonObject()
            val disabledOrigins: Int = OriginsWorldConfigWarning.disabledAndromedaOrigins(root)
            val disabledPowers: Int = OriginsWorldConfigWarning.disabledAndromedaPowers(root)
            val layerBlocks: Int = OriginsWorldConfigWarning.disabledAndromedaLayerEntries(root)
            if (disabledOrigins == 0 && disabledPowers == 0 && layerBlocks == 0) return

            OriginsWorldConfigWarning.LOGGER.warn(
            ("Overgrown Origins world config {} also disables Andromeda content "
            + "(Origins={}, powers={}, layer entries={}). These registry-level settings run "
            + "before Andromeda's Fzzy Config and cannot be overridden there. Prefer Fzzy "
            + "Config for Andromeda Origins unless this double-filtering is intentional."),
            path, disabledOrigins, disabledPowers, layerBlocks
            )
        }catch (exception: Exception) {
            OriginsWorldConfigWarning.LOGGER.warn("Could not inspect Overgrown Origins world config {} for Andromeda conflicts.", path, exception)
        }
    }

    private fun disabledAndromedaOrigins(root: JsonObject): Int {
        val origins: JsonObject? = OriginsWorldConfigWarning.`object`(root, "origins")
        if (origins == null) return 0
        var count: Int = 0
        for (entry in origins.entrySet()) {
            if (!OriginsWorldConfigWarning.isAndromeda(entry.key) || !entry.value.isJsonObject()) continue
            val toggle: JsonObject = entry.value.getAsJsonObject()
            val enabled: JsonElement? = toggle.get("enabled")
            if (enabled != null && enabled.isJsonPrimitive() && !enabled.getAsBoolean()) count++
        }
        return count
    }

    private fun disabledAndromedaPowers(root: JsonObject): Int {
        val origins: JsonObject? = OriginsWorldConfigWarning.`object`(root, "origins")
        if (origins == null) return 0
        var count: Int = 0
        for (entry in origins.entrySet()) {
            if (!OriginsWorldConfigWarning.isAndromeda(entry.key) || !entry.value.isJsonObject()) continue
            val powers: JsonObject? = OriginsWorldConfigWarning.`object`(entry.value.getAsJsonObject(), "powers")
            if (powers == null) continue
            for (power in powers.entrySet()) {
                if (power.value.isJsonPrimitive() && !power.value.getAsBoolean()) count++
            }
        }
        return count
    }

    private fun disabledAndromedaLayerEntries(root: JsonObject): Int {
        val layers: JsonObject? = OriginsWorldConfigWarning.`object`(root, "layers")
        if (layers == null) return 0
        var count: Int = 0
        for (layer in layers.entrySet()) {
            if (!layer.value.isJsonObject()) continue
            val origins: JsonObject? = OriginsWorldConfigWarning.`object`(layer.value.getAsJsonObject(), "origins")
            if (origins == null) continue
            for (origin in origins.entrySet()) {
                if (OriginsWorldConfigWarning.isAndromeda(origin.key) && origin.value.isJsonPrimitive()
                && !origin.value.getAsBoolean()) count++
            }
        }
        return count
    }

    private fun `object`(parent: JsonObject, key: String?): JsonObject? {
        val value: JsonElement? = parent.get(key)
        return if (value != null && value.isJsonObject()) value.getAsJsonObject() else null
    }

    private fun isAndromeda(id: String?): Boolean {
        return id != null && id.startsWith("andromeda_origins:")
    }
}
