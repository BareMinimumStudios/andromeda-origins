package andromeda.origins.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small, dependency-free configuration for the optional enhanced audiovisual layer.
 *
 * <p>The file is intentionally separate from gameplay balancing. Removing the optional
 * libraries or setting {@code enabled} to false returns Andromeda Origins to its bundled
 * sounds and vanilla particles without touching any Origin power JSON.</p>
 */
public final class EnhancedFxConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("andromeda_origins_fx.json");

    private static Data data = new Data();

    private EnhancedFxConfig() {}

    public static void load() {
        if (Files.isRegularFile(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                Data loaded = GSON.fromJson(reader, Data.class);
                if (loaded != null) {
                    data = loaded;
                }
            } catch (Exception ignored) {
                data = new Data();
            }
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
            // Cosmetic compatibility must never stop the base mod from loading.
        }
    }

    public static boolean enabled() {
        return data.enabled;
    }

    public static boolean particles() {
        return data.particles;
    }

    public static boolean sounds() {
        return data.sounds;
    }

    public static void setEnabled(boolean enabled) {
        data.enabled = enabled;
        save();
    }

    public static void setParticles(boolean enabled) {
        data.particles = enabled;
        save();
    }

    public static void setSounds(boolean enabled) {
        data.sounds = enabled;
        save();
    }

    private static final class Data {
        boolean enabled = true;
        boolean particles = true;
        boolean sounds = true;
    }
}
