package andromeda.origins;

import andromeda.origins.command.AndromedaOriginsCommands;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class AndromedaOrigins implements ModInitializer {
    public static final String MOD_ID = "andromeda_origins";

    // Datapack, asset, registry, power, tag, and Figura-facing content uses the mod namespace.
    public static final String CONTENT_NAMESPACE = MOD_ID;

    private static Item registerIcon(String path) {
        return Registry.register(Registries.ITEM, Identifier.of(CONTENT_NAMESPACE, path), new Item(new Settings()));
    }

    @Override
    public void onInitialize() {
        registerIcon("arachne_icon");
        registerIcon("faerie_icon");
        registerIcon("fenrkin_icon");
        registerIcon("gorgon_icon");
        registerIcon("humanity_icon");
        registerIcon("lichling_icon");
        registerIcon("manticore_icon");
        registerIcon("nereid_icon");
        registerIcon("satyr_icon");
        registerIcon("selkie_icon");
        registerIcon("siren_icon");
        registerIcon("veilborn_icon");
        registerIcon("wyverian_icon");

        AndromedaOriginsCommands.register();

    }
}
