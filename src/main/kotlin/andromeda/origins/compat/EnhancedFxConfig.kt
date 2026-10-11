package andromeda.origins.compat

import java.io.IOException


 /**
 * Small, dependency-free configuration for the optional enhanced audiovisual layer.
 *
 *
 * The file is intentionally separate from gameplay balancing. Removing the optional
 * libraries or setting `enabled` to false returns Andromeda Origins to its bundled
 * sounds and vanilla particles without touching any Origin power JSON.
 */
object EnhancedFxConfig {
    val LOGGER: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("Andromeda Origins/Enhanced FX")
    val GSON: com.google.gson.Gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
    val PATH: java.nio.file.Path = andromeda.origins.platform.Platform.configDir().resolve("andromeda_origins_fx.json")

    private var data: EnhancedFxConfig.Data = EnhancedFxConfig.Data()

    fun load() {
        if (java.nio.file.Files.isRegularFile(EnhancedFxConfig.PATH)) {
            try {
                java.nio.file.Files.newBufferedReader(EnhancedFxConfig.PATH).use {
                reader -> val loaded: EnhancedFxConfig.Data? = EnhancedFxConfig.GSON.fromJson<EnhancedFxConfig.Data>(reader, EnhancedFxConfig.Data::class.java)
                if (loaded != null) {
                    EnhancedFxConfig.data = loaded
                }

                } }catch (exception: java.lang.Exception) {
                EnhancedFxConfig.LOGGER.warn("Could not read {}; using default Enhanced FX settings.", EnhancedFxConfig.PATH, exception)
                EnhancedFxConfig.data = EnhancedFxConfig.Data()
            }
        }
        EnhancedFxConfig.save()
    }

    fun save() {
        try {
            java.nio.file.Files.createDirectories(EnhancedFxConfig.PATH.getParent())
            java.nio.file.Files.newBufferedWriter(EnhancedFxConfig.PATH).use {writer -> EnhancedFxConfig.GSON.toJson(EnhancedFxConfig.data, writer)
            } }catch (exception: IOException) {
            // Cosmetic compatibility must never stop the base mod from loading.
            EnhancedFxConfig.LOGGER.warn("Could not save optional Enhanced FX settings to {}.", EnhancedFxConfig.PATH, exception)
        }
    }

    fun enabled(): kotlin.Boolean {
        return EnhancedFxConfig.data.enabled
    }

    fun particles(): kotlin.Boolean {
        return EnhancedFxConfig.data.particles
    }

    fun sounds(): kotlin.Boolean {
        return EnhancedFxConfig.data.sounds
    }

    fun setEnabled(enabled: kotlin.Boolean) {
        EnhancedFxConfig.data.enabled = enabled
        EnhancedFxConfig.save()
    }

    fun setParticles(enabled: kotlin.Boolean) {
        EnhancedFxConfig.data.particles = enabled
        EnhancedFxConfig.save()
    }

    fun setSounds(enabled: kotlin.Boolean) {
        EnhancedFxConfig.data.sounds = enabled
        EnhancedFxConfig.save()
    }

    private class Data {
        var enabled: kotlin.Boolean = true
        var particles: kotlin.Boolean = true
        var sounds: kotlin.Boolean = true
    }
}
