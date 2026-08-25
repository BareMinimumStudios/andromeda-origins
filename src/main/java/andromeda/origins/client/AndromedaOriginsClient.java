package andromeda.origins.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class AndromedaOriginsClient implements ClientModInitializer {
    public static final String TOGGLE_KEY_ID = "key.andromeda_origins.toggle";
    public static final String KEY_CATEGORY_ID = "category.andromeda_origins";

    @Override
    public void onInitializeClient() {
        // C mirrors the vanilla Save Hotbar Activator default, but this is a separate
        // keybinding so players can rebind the Andromeda toggle independently.
        KeyBindingHelper.registerKeyBinding(new KeyBinding(
            TOGGLE_KEY_ID,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            KEY_CATEGORY_ID
        ));
    }
}
