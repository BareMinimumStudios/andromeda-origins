package andromeda.origins

import andromeda.origins.AndromedaOrigins


 /**
 * Registry entries for bundled Andromeda Origins ability sounds.
 *
 * The actual OGG files and sound definitions live under assets/andromeda_origins.
 * Registering matching SoundEvents lets Apoli's play_sound action and vanilla
 * playsound/stopsound commands resolve them server-side.
 */
object AndromedaSounds {
    val IDS: kotlin.Array<kotlin.String> = kotlin.arrayOf<kotlin.String>("ability.arachne.silk_snap",
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
    )

    fun register() {
        for (path in AndromedaSounds.IDS) {
            val id: net.minecraft.resources.ResourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, path)
            andromeda.origins.platform.Platform.registerSound(id)
        }
    }
}
