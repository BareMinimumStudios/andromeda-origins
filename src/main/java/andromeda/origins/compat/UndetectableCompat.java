package andromeda.origins.compat;

import net.minecraft.entity.Entity;

/** Shared marker used by the Undetectable power for vanilla AI/render compatibility. */
public final class UndetectableCompat {
    public static final String COMMAND_TAG = "andromeda_undetectable";

    private UndetectableCompat() {}

    public static boolean isUndetectable(Entity entity) {
        return entity != null && entity.getCommandTags().contains(COMMAND_TAG);
    }
}
