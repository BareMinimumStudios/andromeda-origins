package andromeda.origins.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Optional audiovisual compatibility for Spell Engine and More RPG Library.
 */
public final class EnhancedFxCompat {
    public static final String SPELL_ENGINE_ID = "spell_engine";
    public static final String MORE_RPG_ID = "more_rpg_classes";

    private static boolean spellEngineLoaded;
    private static boolean moreRpgLoaded;
    private static boolean initialized;

    private EnhancedFxCompat() {}

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        EnhancedFxConfig.load();
        EnhancedFxDefinitions.register();
        FabricLoader loader = FabricLoader.getInstance();
        spellEngineLoaded = loader.isModLoaded(SPELL_ENGINE_ID);
        moreRpgLoaded = loader.isModLoaded(MORE_RPG_ID);
    }

    public static boolean active() {
        initialize();
        return EnhancedFxConfig.enabled() && spellEngineLoaded;
    }

    public static String statusLine() {
        initialize();
        return "Enhanced FX=" + EnhancedFxConfig.enabled()
            + ", particles=" + EnhancedFxConfig.particles()
            + ", sounds=" + EnhancedFxConfig.sounds()
            + ", Spell Engine=" + spellEngineLoaded
            + ", More RPG Library=" + moreRpgLoaded
            + ", definitions=" + EnhancedFxDefinitions.count();
    }

    /** Called by the internal Origins execute_command hook. */
    public static boolean play(Entity entity, String event) {
        return playInternal(entity, event, null);
    }

    public static boolean playAt(Entity entity, String event, Vec3d position) {
        return playInternal(entity, event, position);
    }

    private static boolean playInternal(Entity entity, String event, Vec3d position) {
        initialize();
        if (!(entity.getWorld() instanceof ServerWorld) || !EnhancedFxConfig.enabled() || !spellEngineLoaded) {
            return false;
        }

        EnhancedFxDefinitions.EventDefinition definition = EnhancedFxDefinitions.get(event);
        if (definition == null) {
            return false;
        }

        if (EnhancedFxConfig.particles()) {
            List<EnhancedFxDefinitions.ParticleDefinition> particles = definition.particles().stream()
                .filter(p -> requirementPresent(p.requiresMod()))
                .toList();
            if (!particles.isEmpty()) {
                ParticleBridge.spawn(entity, position, particles);
            }
        }

        if (EnhancedFxConfig.sounds()) {
            double x = position != null ? position.x : entity.getX();
            double y = position != null ? position.y : entity.getY();
            double z = position != null ? position.z : entity.getZ();
            for (EnhancedFxDefinitions.SoundDefinition sound : definition.sounds()) {
                if (requirementPresent(sound.requiresMod())) {
                    playSound(entity, x, y, z, sound);
                }
            }
        }
        return true;
    }

    private static boolean requirementPresent(String modId) {
        if (modId == null || modId.isBlank()) {
            return true;
        }
        if (MORE_RPG_ID.equals(modId)) {
            return moreRpgLoaded;
        }
        if (SPELL_ENGINE_ID.equals(modId)) {
            return spellEngineLoaded;
        }
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    private static void playSound(Entity entity, double x, double y, double z, EnhancedFxDefinitions.SoundDefinition definition) {
        Identifier id = Identifier.tryParse(definition.id());
        if (id == null || !Registries.SOUND_EVENT.containsId(id)) {
            return;
        }
        SoundEvent sound = Registries.SOUND_EVENT.get(id);
        if (sound == null) {
            return;
        }
        entity.getWorld().playSound(
            null,
            x, y, z,
            sound,
            SoundCategory.PLAYERS,
            definition.volume(),
            definition.pitch()
        );
    }

    /** Reflection-only adapter so Spell Engine is never a JVM linkage requirement. */
    private static final class ParticleBridge {
        private static boolean attempted;
        private static boolean available;
        private static Class<? extends Enum> easingClass;
        private static Class<? extends Enum> motionClass;
        private static Method builderOf;
        private static Method builderScale;
        private static Method builderScaleVariance;
        private static Method builderScaleTo;
        private static Method builderColor;
        private static Method builderColorVariance;
        private static Method builderOpacity;
        private static Method builderFadeOut;
        private static Method builderFadeInOut;
        private static Method builderPlaybackSpeed;
        private static Method builderLifetimeVariance;
        private static Method builderGlow;
        private static Method builderMotion;
        private static Method builderGravity;
        private static Method builderDrag;
        private static Method builderCollides;
        private static Method builderAttached;
        private static Method builderAttachedToGround;
        private static Method builderBatch;
        private static Method sendBatches;
        private static Method sendBatchesAt;
        private static Method batchesImpact;
        private static Method batchesCasting;
        private static Method batchesTravel;
        private static Method batchesCloud;
        private static Method batchesShockwave;
        private static Method batchesGround;
        private static Method batchesPlaced;
        private static Method batchesCone;
        private static Method batchesPopUp;
        private static Method batchesHelix;

        private ParticleBridge() {}

        @SuppressWarnings("unchecked")
        private static boolean init() {
            if (attempted) {
                return available;
            }
            attempted = true;
            try {
                Class<?> builderClass = Class.forName("net.spell_engine.api.spell.fx.ParticleGroupBuilder");
                Class<?> batchesClass = Class.forName("net.spell_engine.api.spell.fx.ParticleGroupBuilder$Batches");
                Class<?> helperClass = Class.forName("net.spell_engine.fx.ParticleHelper");
                easingClass = (Class<? extends Enum>) Class.forName("net.spell_engine.api.spell.fx.Easing");
                motionClass = (Class<? extends Enum>) Class.forName("net.spell_engine.api.spell.fx.ParticleGroup$Motion");

                builderOf = builderClass.getMethod("of", Identifier.class);
                builderScale = builderClass.getMethod("scale", float.class);
                builderScaleVariance = builderClass.getMethod("scale", float.class, float.class);
                builderScaleTo = builderClass.getMethod("scaleTo", float.class, easingClass);
                builderColor = builderClass.getMethod("color", long.class);
                builderColorVariance = builderClass.getMethod("colorVariance", float.class);
                builderOpacity = builderClass.getMethod("opacity", float.class);
                builderFadeOut = builderClass.getMethod("fadeOut", float.class, easingClass);
                builderFadeInOut = builderClass.getMethod("fadeInOut", float.class, easingClass);
                builderPlaybackSpeed = builderClass.getMethod("playbackSpeed", float.class);
                builderLifetimeVariance = builderClass.getMethod("lifetimeVariance", float.class);
                builderGlow = builderClass.getMethod("glow", boolean.class);
                builderMotion = builderClass.getMethod("motion", motionClass);
                builderGravity = builderClass.getMethod("gravity", float.class);
                builderDrag = builderClass.getMethod("drag", float.class);
                builderCollides = builderClass.getMethod("collides");
                builderAttached = builderClass.getMethod("attached");
                builderAttachedToGround = builderClass.getMethod("attachedToGround");
                builderBatch = builderClass.getMethod("batch", Consumer.class);
                sendBatches = helperClass.getMethod("sendBatches", Entity.class, List.class);
                sendBatchesAt = helperClass.getMethod("sendBatches", Vec3d.class, LivingEntity.class, List.class);

                batchesImpact = batchesClass.getMethod("impact", float.class, float.class);
                batchesCasting = batchesClass.getMethod("casting", float.class, float.class);
                batchesTravel = batchesClass.getMethod("travel", float.class, float.class);
                batchesCloud = batchesClass.getMethod("cloud", float.class, float.class);
                batchesShockwave = batchesClass.getMethod("shockwave", float.class, float.class, float.class);
                batchesGround = batchesClass.getMethod("ground", float.class);
                batchesPlaced = batchesClass.getMethod("placed", float.class);
                batchesCone = batchesClass.getMethod("cone", float.class, float.class, float.class);
                batchesPopUp = batchesClass.getMethod("popUp");
                batchesHelix = batchesClass.getMethod("helix", float.class, float.class, float.class, float.class);
                available = true;
            } catch (ReflectiveOperationException ignored) {
                available = false;
            }
            return available;
        }

        private static void spawn(Entity entity, Vec3d position, List<EnhancedFxDefinitions.ParticleDefinition> specs) {
            if (!init()) {
                return;
            }
            try {
                List<Object> effects = new ArrayList<>(specs.size());
                for (EnhancedFxDefinitions.ParticleDefinition spec : specs) {
                    Identifier id = Identifier.tryParse(spec.id());
                    if (id == null || !Registries.PARTICLE_TYPE.containsId(id)) {
                        continue;
                    }
                    Object builder = builderOf.invoke(null, id);
                    applyAppearance(builder, spec);
                    Object effect = builderBatch.invoke(builder, batchPreset(spec));
                    effects.add(effect);
                }
                if (!effects.isEmpty()) {
                    if (position != null && entity instanceof LivingEntity living) {
                        sendBatchesAt.invoke(null, position, living, effects);
                    } else {
                        sendBatches.invoke(null, entity, effects);
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                available = false;
            }
        }

        private static void applyAppearance(Object builder, EnhancedFxDefinitions.ParticleDefinition spec) throws ReflectiveOperationException {
            if (spec.scaleVariance() > 0F) {
                builder = builderScaleVariance.invoke(builder, spec.scale(), spec.scaleVariance());
            } else if (spec.scale() > 0F && spec.scale() != 1F) {
                builder = builderScale.invoke(builder, spec.scale());
            }
            if (spec.scaleTo() != 1F) {
                builder = builderScaleTo.invoke(builder, spec.scaleTo(), enumValue(easingClass, "LINEAR"));
            }
            if (!spec.color().isBlank()) {
                long color = parseColor(spec.color());
                if (color != -1L) {
                    builder = builderColor.invoke(builder, color);
                }
            }
            if (spec.colorVariance() > 0F) {
                builder = builderColorVariance.invoke(builder, spec.colorVariance());
            }
            if (spec.opacity() != 1F) {
                builder = builderOpacity.invoke(builder, spec.opacity());
            }
            if (spec.fadeOutHold() >= 0F) {
                builder = builderFadeOut.invoke(builder, spec.fadeOutHold(), enumValue(easingClass, "LINEAR"));
            }
            if (spec.fadeInOutHold() >= 0F) {
                builder = builderFadeInOut.invoke(builder, spec.fadeInOutHold(), enumValue(easingClass, "LINEAR"));
            }
            if (spec.playbackSpeed() != 1F) {
                builder = builderPlaybackSpeed.invoke(builder, spec.playbackSpeed());
            }
            if (spec.lifetimeVariance() > 0F) {
                builder = builderLifetimeVariance.invoke(builder, spec.lifetimeVariance());
            }
            if (spec.glow()) {
                builder = builderGlow.invoke(builder, true);
            }
            if (!spec.motion().isBlank()) {
                Object motion = enumValue(motionClass, spec.motion());
                if (motion != null) {
                    builder = builderMotion.invoke(builder, motion);
                }
            }
            if (spec.gravity() != null) {
                builder = builderGravity.invoke(builder, spec.gravity());
            }
            if (spec.drag() != null) {
                builder = builderDrag.invoke(builder, spec.drag());
            }
            if (spec.collides()) {
                builder = builderCollides.invoke(builder);
            }
            if (spec.attachedToGround()) {
                builder = builderAttachedToGround.invoke(builder);
            } else if (spec.attached()) {
                builder = builderAttached.invoke(builder);
            }
        }

        private static Object batchPreset(EnhancedFxDefinitions.ParticleDefinition spec) throws ReflectiveOperationException {
            String preset = spec.preset() == null ? "impact" : spec.preset().toLowerCase(Locale.ROOT);
            return switch (preset) {
                case "casting" -> batchesCasting.invoke(null, spec.count(), spec.speed());
                case "travel" -> batchesTravel.invoke(null, spec.count(), spec.speed());
                case "cloud" -> batchesCloud.invoke(null, spec.count(), spec.speed());
                case "shockwave" -> batchesShockwave.invoke(null, spec.count(), spec.speed(), spec.preTravel());
                case "ground" -> batchesGround.invoke(null, spec.count());
                case "placed" -> batchesPlaced.invoke(null, spec.count());
                case "cone" -> batchesCone.invoke(null, spec.count(), spec.speed(), spec.angle());
                case "helix" -> batchesHelix.invoke(null, spec.count(), spec.speed(), spec.degreesPerTick(), spec.offset());
                case "pop_up", "popup" -> batchesPopUp.invoke(null);
                default -> batchesImpact.invoke(null, spec.count(), spec.speed());
            };
        }

        private static long parseColor(String input) {
            String s = input.trim();
            if (s.startsWith("#")) {
                s = s.substring(1);
            }
            try {
                if (s.length() == 6) {
                    // Spell Engine packs colors as RGBA, not ARGB. Append opaque alpha.
                    return Long.parseLong(s + "FF", 16);
                }
                if (s.length() == 8) {
                    return Long.parseLong(s, 16);
                }
            } catch (NumberFormatException ignored) {
            }
            return -1L;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Object enumValue(Class<? extends Enum> enumClass, String name) {
            if (enumClass == null || name == null || name.isBlank()) {
                return null;
            }
            try {
                return Enum.valueOf((Class) enumClass, name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
