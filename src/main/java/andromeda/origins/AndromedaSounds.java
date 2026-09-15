package andromeda.origins;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Registry entries for bundled Andromeda Origins ability sounds.
 *
 * The actual OGG files and sound definitions live under assets/andromeda_origins.
 * Registering matching SoundEvents lets Apoli's play_sound action and vanilla
 * playsound/stopsound commands resolve them server-side.
 */
public final class AndromedaSounds {

    private static final String[] IDS = {
        "ability.arachne.silk_snap",
        "ability.arachne.web_pull",
        "ability.faerie.fae_chime",
        "ability.faerie.dust_depleted",
        "ability.fenrkin.on_the_hunt_howl",
        "ability.fenrkin.marked",
        "ability.gorgon.hiss",
        "ability.gorgon.stone_form",
        "ability.humanity.mortal_resolve_ignite",
        "ability.humanity.mortal_resolve_heartbeat",
        "ability.lichling.final_bell_toll",
        "ability.lichling.chimes_of_necros",
        "ability.lichling.screech",
        "ability.manticore.ravenous_lunge",
        "ability.manticore.ravenous_lunge_airborne",
        "ability.manticore.beast_of_blood",
        "ability.manticore.lunge_impact",
        "ability.nereid.convalescing_aura",
        "ability.satyr.rush_wind",
        "ability.satyr.hoof_launch",
        "ability.satyr.satyrs_landing",
        "ability.selkie.surging_tides",
        "ability.siren.infatuation_charm",
        "ability.siren.infatuation_whisper",
        "ability.siren.shrieking_wail",
        "ability.siren.wail_impact",
        "ability.veilborn.veil_open",
        "ability.veilborn.veil_close",
        "ability.veilborn.transposition",
        "ability.veilborn.auroral_mirage_activate",
        "ability.veilborn.reality_fracture",
        "ability.veilborn.auroral_mirage_break",
        "ability.wyverian.dragon_wing_rush",
        "ability.wyverian.roar"
    };

    private AndromedaSounds() {}

    public static void register() {
        for (String path : IDS) {
            Identifier id = Identifier.of(AndromedaOrigins.MOD_ID, path);
            Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
        }
    }
}
