package andromeda.origins.compat;

import andromeda.origins.AndromedaOrigins;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Datapack-backed Enhanced FX definitions.
 */
public final class EnhancedFxDefinitions {
    private static final Identifier RELOAD_ID = Identifier.of(AndromedaOrigins.MOD_ID, "enhanced_fx");
    private static volatile Map<String, EventDefinition> events = Map.of();
    private static boolean registered;

    private EnhancedFxDefinitions() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return RELOAD_ID;
            }

            @Override
            public void reload(ResourceManager manager) {
                Map<String, EventDefinition> loaded = new HashMap<>();
                Map<Identifier, Resource> resources = manager.findResources(
                    "andromeda_fx",
                    id -> id.getNamespace().equals(AndromedaOrigins.MOD_ID) && id.getPath().endsWith(".json")
                );

                List<Identifier> ids = new ArrayList<>(resources.keySet());
                ids.sort(java.util.Comparator.comparing(Identifier::toString));
                for (Identifier id : ids) {
                    Resource resource = resources.get(id);
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                            if (!entry.getValue().isJsonObject()) {
                                continue;
                            }
                            EventDefinition definition = parseEvent(entry.getValue().getAsJsonObject());
                            loaded.put(entry.getKey(), definition);
                        }
                    } catch (Exception ignored) {
                        // A malformed cosmetic definition must not invalidate Origin gameplay data.
                    }
                }
                events = Map.copyOf(loaded);
            }
        });
    }

    public static EventDefinition get(String event) {
        return events.get(event);
    }

    public static int count() {
        return events.size();
    }

    private static EventDefinition parseEvent(JsonObject object) {
        List<ParticleDefinition> particles = new ArrayList<>();
        List<SoundDefinition> sounds = new ArrayList<>();

        JsonArray particleArray = object.has("particles") && object.get("particles").isJsonArray()
            ? object.getAsJsonArray("particles") : new JsonArray();
        for (JsonElement element : particleArray) {
            if (!element.isJsonObject()) continue;
            JsonObject p = element.getAsJsonObject();
            String id = string(p, "id", "");
            if (id.isBlank()) continue;
            particles.add(new ParticleDefinition(
                id,
                string(p, "preset", "impact"),
                number(p, "count", 1F),
                number(p, "speed", 0F),
                number(p, "scale", 1F),
                number(p, "scale_variance", 0F),
                number(p, "scale_to", 1F),
                number(p, "angle", 40F),
                number(p, "pre_travel", 0.15F),
                number(p, "degrees_per_tick", 16F),
                number(p, "offset", 0F),
                string(p, "motion", ""),
                string(p, "color", ""),
                number(p, "color_variance", 0F),
                number(p, "opacity", 1F),
                number(p, "fade_out_hold", -1F),
                number(p, "fade_in_out_hold", -1F),
                number(p, "playback_speed", 1F),
                number(p, "lifetime_variance", 0F),
                nullableNumber(p, "gravity"),
                nullableNumber(p, "drag"),
                bool(p, "glow", false),
                bool(p, "collides", false),
                bool(p, "attached", false),
                bool(p, "attached_to_ground", false),
                string(p, "requires_mod", "")
            ));
        }

        JsonArray soundArray = object.has("sounds") && object.get("sounds").isJsonArray()
            ? object.getAsJsonArray("sounds") : new JsonArray();
        for (JsonElement element : soundArray) {
            if (!element.isJsonObject()) continue;
            JsonObject s = element.getAsJsonObject();
            String id = string(s, "id", "");
            if (id.isBlank()) continue;
            sounds.add(new SoundDefinition(
                id,
                number(s, "volume", 0.35F),
                number(s, "pitch", 1F),
                string(s, "requires_mod", "")
            ));
        }

        return new EventDefinition(List.copyOf(particles), List.copyOf(sounds));
    }

    private static String string(JsonObject object, String key, String fallback) {
        try {
            return object.has(key) ? object.get(key).getAsString() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float number(JsonObject object, String key, float fallback) {
        try {
            return object.has(key) ? object.get(key).getAsFloat() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Float nullableNumber(JsonObject object, String key) {
        try {
            return object.has(key) ? object.get(key).getAsFloat() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        try {
            return object.has(key) ? object.get(key).getAsBoolean() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record EventDefinition(List<ParticleDefinition> particles, List<SoundDefinition> sounds) {}

    public record ParticleDefinition(
        String id,
        String preset,
        float count,
        float speed,
        float scale,
        float scaleVariance,
        float scaleTo,
        float angle,
        float preTravel,
        float degreesPerTick,
        float offset,
        String motion,
        String color,
        float colorVariance,
        float opacity,
        float fadeOutHold,
        float fadeInOutHold,
        float playbackSpeed,
        float lifetimeVariance,
        Float gravity,
        Float drag,
        boolean glow,
        boolean collides,
        boolean attached,
        boolean attachedToGround,
        String requiresMod
    ) {}

    public record SoundDefinition(String id, float volume, float pitch, String requiresMod) {}
}
