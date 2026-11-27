package andromeda.origins.compat

import andromeda.origins.platform.Platform
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import org.slf4j.Logger
import org.slf4j.LoggerFactory


 /**
 * Small, dependency-free configuration for the optional enhanced audiovisual layer.
 *
 *
 * The file is intentionally separate from gameplay balancing. Removing the optional
 * libraries or setting `enabled` to false returns Andromeda Origins to its bundled
 * sounds and vanilla particles without touching any Origin power JSON.
 */
object EnhancedFxConfig {
    val LOGGER: Logger = LoggerFactory.getLogger("Andromeda Origins/Enhanced FX")
    val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    val PATH: Path = Platform.configDir().resolve("andromeda_origins_fx.json")

    private var data: EnhancedFxConfig.Data = EnhancedFxConfig.Data()

    fun load() {
        if (Files.isRegularFile(EnhancedFxConfig.PATH)) {
            try {
                Files.newBufferedReader(EnhancedFxConfig.PATH).use {
                reader -> val loaded: EnhancedFxConfig.Data? = EnhancedFxConfig.GSON.fromJson<EnhancedFxConfig.Data>(reader, EnhancedFxConfig.Data::class.java)
                if (loaded != null) {
                    EnhancedFxConfig.data = loaded
                }

                } }catch (exception: Exception) {
                EnhancedFxConfig.LOGGER.warn("Could not read {}; using default Enhanced FX settings.", EnhancedFxConfig.PATH, exception)
                EnhancedFxConfig.data = EnhancedFxConfig.Data()
            }
        }
        EnhancedFxConfig.save()
    }

    fun save() {
        try {
            Files.createDirectories(EnhancedFxConfig.PATH.getParent())
            Files.newBufferedWriter(EnhancedFxConfig.PATH).use {writer -> EnhancedFxConfig.GSON.toJson(EnhancedFxConfig.data, writer)
            } }catch (exception: IOException) {
            // Cosmetic compatibility must never stop the base mod from loading.
            EnhancedFxConfig.LOGGER.warn("Could not save optional Enhanced FX settings to {}.", EnhancedFxConfig.PATH, exception)
        }
    }

    fun enabled(): Boolean {
        return EnhancedFxConfig.data.enabled
    }

    fun particles(): Boolean {
        return EnhancedFxConfig.data.particles
    }

    fun sounds(): Boolean {
        return EnhancedFxConfig.data.sounds
    }

    fun setEnabled(enabled: Boolean) {
        EnhancedFxConfig.data.enabled = enabled
        EnhancedFxConfig.save()
    }

    fun setParticles(enabled: Boolean) {
        EnhancedFxConfig.data.particles = enabled
        EnhancedFxConfig.save()
    }

    fun setSounds(enabled: Boolean) {
        EnhancedFxConfig.data.sounds = enabled
        EnhancedFxConfig.save()
    }

    private class Data {
        var enabled: Boolean = true
        var particles: Boolean = true
        var sounds: Boolean = true
    }
}
