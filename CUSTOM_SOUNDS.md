# Custom Sound Integration

**Current state: v1.4.72**. The 33 bundled Andromeda SoundEvents remain unchanged. When the optional enhanced-FX layer is active, selected Spell Engine / More RPG Library sounds are mixed underneath the existing Origin audio; they do not replace or re-register Andromeda's custom sounds.

Andromeda Origins registers **33 custom SoundEvents**. Bundled ability audio is stored under `assets/andromeda_origins/sounds/abilities/` and registered in `assets/andromeda_origins/sounds.json`.

## Optional external sound layers

Spell Engine / More RPG Library sounds are looked up from Minecraft's SoundEvent registry at runtime. Missing IDs or absent optional mods are skipped safely. The enhanced definitions keep their volumes intentionally below the main Andromeda cues so character-specific custom audio remains dominant.

Examples in v1.4.60 include soul casting/release under Lichling, water release/bubble accents under Nereid/Selkie/Siren, air impacts under Satyr, stealth/arcane accents under Veilborn, and generic fire/wind accents under Wyverian. These mappings are stored with the particle definitions in `data/andromeda_origins/andromeda_fx/`.

## Playback rules

- Custom positional assets are mono 48 kHz OGG.
- Long channel/ambience recordings are marked streamed where appropriate.
- Important moving/long sounds use guaranteed local playback plus positional copies when needed.
- Long channel sounds have explicit stop cleanup so movement/release does not leave stale audio behind.
- Vanilla Minecraft accent layers are not counted among the 33 custom SoundEvents.

## Current behavior notes

- **Faerie:** Allay/Vex accents are vanilla Minecraft sounds layered after the custom fae chime.
- **Fenrkin:** successful Mark of Fenrir gives the marker and target their own audible marked cue.
- **Humanity:** Mortal Resolve heartbeat is a short positional pulse replayed from the Human’s current position rather than one static minute-long bed.
- **Lichling:** Chimes of Necros plays while channeling and is explicitly stopped when the channel ends; the Final Bell Toll now plays at the **end of the ability**, not on initial Doom application.
- **Manticore:** Ravenous Lunge uses one activation roar and one end-of-lunge roar; there is no repeating airborne roar loop. Collision impacts have their own cue. Champion Bloodrift reuses the Manticore roar immediately after reappearance.
- **Nereid:** Convalescing Aura has local/positional playback and end-of-channel stop cleanup.
- **Siren:** Infatuation long audio begins only on successful targeting; the target whisper stops if Infatuation ends early. Shrieking Wail victim impact is intentionally mixed louder than earlier builds.
- **Satyr:** ordinary foot contact uses vanilla horse-step/clop audio; the custom landing sound is reserved for Satyr’s Landing.

## Registered custom sounds

| SoundEvent | Asset | Duration | Mode | Usage |
|---|---|---:|---|---|
| `andromeda_origins:ability.arachne.silk_snap` | `abilities/arachne/silk_snap.ogg` | 2.33s | one-shot | Weaver's Nest/web-shot connection |
| `andromeda_origins:ability.arachne.web_pull` | `abilities/arachne/web_pull.ogg` | 1.75s | one-shot | Web pull applied to a target |
| `andromeda_origins:ability.faerie.fae_chime` | `abilities/faerie/fae_chime.ogg` | 3.80s | one-shot | Flutter start and successful Fae Illusions cast |
| `andromeda_origins:ability.faerie.dust_depleted` | `abilities/faerie/dust_depleted.ogg` | 2.40s | one-shot | Faerie Dust resource reaches zero |
| `andromeda_origins:ability.fenrkin.on_the_hunt_howl` | `abilities/fenrkin/on_the_hunt_howl.ogg` | 3.80s | one-shot | On the Hunt activation |
| `andromeda_origins:ability.gorgon.hiss` | `abilities/gorgon/hiss.ogg` | 1.60s | one-shot | Ophidian Gaze wind-up |
| `andromeda_origins:ability.gorgon.stone_form` | `abilities/gorgon/stone_form.ogg` | 1.92s | one-shot | Ophidian Gaze petrification impact |
| `andromeda_origins:ability.humanity.mortal_resolve_ignite` | `abilities/humanity/mortal_resolve_ignite.ogg` | 3.08s | one-shot | Mortal Resolve activation |
| `andromeda_origins:ability.humanity.mortal_resolve_heartbeat` | `abilities/humanity/mortal_resolve_heartbeat.ogg` | 1.50s | one-shot | 1.5-second heartbeat pulse replayed during Mortal Resolve from the Human's current position |
| `andromeda_origins:ability.lichling.chimes_of_necros` | `abilities/lichling/chimes_of_necros.ogg` | 10.92s | streamed | Chimes of Necros channel bed; local + positional copies with explicit stop cleanup |
| `andromeda_origins:ability.manticore.ravenous_lunge` | `abilities/manticore/ravenous_lunge.ogg` | 1.92s | one-shot | Ravenous Lunge activation roar |
| `andromeda_origins:ability.manticore.beast_of_blood` | `abilities/manticore/beast_of_blood.ogg` | 2.60s | one-shot | Beast of Blood activation |
| `andromeda_origins:ability.manticore.lunge_impact` | `abilities/manticore/lunge_impact.ogg` | 2.20s | one-shot | Ravenous Lunge target collision impact |
| `andromeda_origins:ability.nereid.convalescing_aura` | `abilities/nereid/convalescing_aura.ogg` | 15.70s | streamed | Convalescing Aura channel bed; caster-local + positional nearby copy with stop cleanup |
| `andromeda_origins:ability.satyr.rush_wind` | `abilities/satyr/rush_wind.ogg` | 2.00s | one-shot | Rush activation |
| `andromeda_origins:ability.satyr.hoof_launch` | `abilities/satyr/hoof_launch.ogg` | 1.00s | one-shot | Rush / Swift Leap launch |
| `andromeda_origins:ability.satyr.satyrs_landing` | `abilities/satyr/satyrs_landing.ogg` | 1.80s | one-shot | Satyr's Landing ground-pound impact |
| `andromeda_origins:ability.selkie.surging_tides` | `abilities/selkie/surging_tides.ogg` | 1.10s | one-shot | Surging Tides activation |
| `andromeda_origins:ability.siren.infatuation_charm` | `abilities/siren/infatuation_charm.ogg` | 15.00s | streamed | Successful Infatuation caster-side charm layer |
| `andromeda_origins:ability.siren.infatuation_whisper` | `abilities/siren/infatuation_whisper.ogg` | 15.00s | streamed | Infatuated target-local whisper; stopped if Infatuation ends early |
| `andromeda_origins:ability.siren.shrieking_wail` | `abilities/siren/shrieking_wail.ogg` | 4.00s | one-shot | Shrieking Wail activation |
| `andromeda_origins:ability.siren.wail_impact` | `abilities/siren/wail_impact.ogg` | 2.50s | one-shot | Shrieking Wail victim impact; current impact mix is intentionally louder |
| `andromeda_origins:ability.veilborn.veil_open` | `abilities/veilborn/veil_open.ogg` | 2.10s | one-shot | Curtain Step veil opening |
| `andromeda_origins:ability.veilborn.veil_close` | `abilities/veilborn/veil_close.ogg` | 2.10s | one-shot | Auroral Mirage manual dismissal/veil close |
| `andromeda_origins:ability.veilborn.transposition` | `abilities/veilborn/transposition.ogg` | 3.00s | one-shot | Veil Transposition successful swap |
| `andromeda_origins:ability.veilborn.auroral_mirage_activate` | `abilities/veilborn/auroral_mirage_activate.ogg` | 2.50s | one-shot | Auroral Mirage activation |
| `andromeda_origins:ability.veilborn.reality_fracture` | `abilities/veilborn/reality_fracture.ogg` | 2.60s | one-shot | Reality Shatter application cue |
| `andromeda_origins:ability.veilborn.auroral_mirage_break` | `abilities/veilborn/auroral_mirage_break.ogg` | 3.60s | one-shot | Auroral Mirage broken by attacking |
| `andromeda_origins:ability.wyverian.dragon_wing_rush` | `abilities/wyverian/dragon_wing_rush.ogg` | 1.00s | one-shot | Gusts of Freedom flight pulse |
| `andromeda_origins:ability.wyverian.roar` | `abilities/wyverian/roar.ogg` | 3.10s | one-shot | Ember Flames initial roar cue |
| `andromeda_origins:ability.lichling.final_bell_toll` | `abilities/lichling/final_bell_toll.ogg` | 5.25s | one-shot | Plays when Chimes of Necros channeling ends |
| `andromeda_origins:ability.manticore.ravenous_lunge_airborne` | `abilities/manticore/ravenous_lunge_airborne.ogg` | 0.80s | one-shot | End-of-lunge roar (historical asset/event name retained) |
| `andromeda_origins:ability.fenrkin.marked` | `abilities/fenrkin/marked.ogg` | 2.71s | one-shot | Successful Mark of Fenrir completion; local cue for marker and target |

## Validation expectations

The release audit checks that every registered custom SoundEvent has a matching OGG asset and that the custom OGG files are readable mono 48 kHz Vorbis.

- **Lichling screech:** `andromeda_origins:ability.lichling.screech` plays as the opening cue when Chimes of Necros begins; the supplied `lichlingscreech.ogg` remains bundled as its source asset.
