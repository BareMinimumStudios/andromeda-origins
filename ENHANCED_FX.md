# Optional Enhanced Origin FX

**Current source: `1.4.83`.** Optional particle and sound layers keep bundled Origin effects available when their libraries or assets are absent. Malformed definitions and unsupported optional APIs are logged without blocking abilities.

## v1.4.72 Wyverian camera-clear pass

- Standard Pyroclast and Champion Dragon Charge buildup no longer use center-aligned helix batches on the caster; their enhanced charge particles now use the feet-level casting layout so first-person aim is not covered.
- Champion Dragon Breath's enhanced caster burst was likewise moved from a forward center cone to feet-level casting particles.
- Champion Dragon Charge impact smoke now uses a smaller batch with a fast fade and faster playback so the smoke cloud does not hang in the scene.
- Vanilla Ember/Dragon ray and projectile trail cosmetics are offset forward independently of their gameplay raycasts, so damage/hit detection is unchanged.


## Dependency model

- `spell_engine`: optional (`suggests`).
- `mrpg_lib` (current More RPG Library) or legacy `more_rpg_classes`: optional (`suggests`).
- Andromeda does **not** bundle either project and does not declare either as a runtime dependency.
- More RPG Library still requires its own dependencies (including Spell Engine and Spell Power) when More RPG itself is installed.

The bridge deliberately uses reflection for Spell Engine's particle API. No Spell Engine class appears in Andromeda's normal linkage path, so a server/client without Spell Engine can load Andromeda normally.

## Fallback behavior

Every original Andromeda custom sound and vanilla particle remains in the power JSON. Enhanced FX are an additive layer. Particle layers need Spell Engine; registered or resource-pack-only sound layers can run independently. Missing optional libraries/assets or unsupported particle APIs skip the affected extra layer while original effects still run.

## Data-driven definitions

Definitions live at:

```text
data/andromeda_origins/andromeda_fx/
```

There is one JSON file per Origin. Each event may define particle batches and optional sound layers. Example:

```json
{
  "selkie.surging_tides": {
    "particles": [
      {
        "id": "more_rpg_classes:water_splash",
        "preset": "impact",
        "count": 12,
        "speed": 0.11,
        "scale": 1.0,
        "requires_mod": "more_rpg_classes"
      }
    ],
    "sounds": [
      {
        "id": "more_rpg_classes:water_wave_release_1",
        "volume": 0.38,
        "pitch": 1.0,
        "requires_mod": "more_rpg_classes"
      }
    ]
  }
}
```

Supported particle presets are `impact`, `casting`, `travel`, `cloud`, `shockwave`, `ground`, `placed`, `cone`, `helix`, and `pop_up`. `angle` is used by `cone`; `pre_travel` is used by `shockwave`; `degrees_per_tick` and `offset` are used by `helix`. Definitions may also set color, glow, opacity/fade, scale growth, playback speed, lifetime variance, motion, gravity, drag, collision, and attachment flags.

## Custom resource-pack sounds

Copy the relevant bundled definition, for example `data/andromeda_origins/andromeda_fx/faerie.json`, into the **same path in a server datapack**. Keep the other events/particles and edit the desired event's `sounds` array:

```json
[
  {
    "id": "andromeda_custom:ability.flutter",
    "resource_pack": true,
    "volume": 0.35,
    "pitch": 1.0
  }
]
```

Give clients a resource pack containing `assets/andromeda_custom/sounds/flutter.ogg` and `assets/andromeda_custom/sounds.json`:

```json
{
  "ability.flutter": {
    "sounds": ["andromeda_custom:flutter"]
  }
}
```

Both packs need a normal Minecraft 1.21.1 `pack.mcmeta` (data format 48; resource format 34). Enable the server datapack and client resource pack. Server `/reload` applies definition changes; clients reload resources after changing sound assets. A resource pack alone cannot change server event definitions. The highest-priority pack's file replaces the entire matching definition file, so preserve other entries you want to retain.

`resource_pack: true` permits an unregistered sound ID supplied by clients; it defaults to false. Spell Engine and More RPG Library are unnecessary for this sound-only setup. Registered Minecraft/mod sounds work without that flag. Optional `requires_mod` gates a sound by installed server mod ID. Missing client sounds leave the normal ability and bundled cues working; Minecraft may log an unknown-sound warning.

Sound layers are additive to bundled cues, not replacements for the original power actions. The existing master/sounds switches control these extra layers. Volume is clamped to 0–4 and pitch to 0.5–2. Invalid IDs and non-finite values are skipped with a warning once; missing optional sounds do not stop remaining valid cues or spend additional resources.

## HUD and badge texture fallbacks

Visible custom HUDs use Opoli's built-in resource bar if their primary texture is missing, including safe bar/icon indices. Explicit tooltip badges use Andromeda's bundled informational glyph. The custom Origin menu honors the badge's ordered fallback list before using that glyph; text and hover behavior remain available. Existing artwork keeps priority. These fallbacks cover missing resources, not intentionally transparent or corrupt replacement images.

Pack authors can override these fallback fields using the upstream HUD/badge format. Server datapack changes require `/reload`; client artwork changes require a resource reload. Origin/global-power-set `loading_priority` is now respected by the dependencies across packs; this mod does not raise its priorities to block intentional pack overrides. Origin selection layers retain their existing merge behavior.

## Current event coverage

The layer currently defines **112 enhanced FX events** across all 13 Origins. The event set now includes one-shot casts/impacts plus recurring channel/aura pulses, target-side feedback, movement trails, staged charge effects, and state escalations.

| Origin | Enhanced FX events |
|---|---:|
| Arachne | 5 |
| Faerie | 20 |
| Fenrkin | 4 |
| Gorgon | 4 |
| Humanity | 7 |
| Lichling | 15 |
| Manticore | 9 |
| Nereid | 6 |
| Satyr | 5 |
| Selkie | 7 |
| Siren | 5 |
| Veilborn | 11 |
| Wyverian | 14 |

## Testing / reverting without uninstalling anything

The config is generated at:

```text
config/andromeda_origins_fx.json
```

Default values:

```json
{
  "enabled": true,
  "particles": true,
  "sounds": true
}
```

Operators can change the same switches live:

```mcfunction
/andromedaorigins enhanced_fx
/andromedaorigins enhanced_fx enabled false
/andromedaorigins enhanced_fx particles false
/andromedaorigins enhanced_fx sounds false
```

This makes A/B testing simple. Setting `enabled` to `false` immediately returns Origin presentation to the existing Andromeda/vanilla effects while leaving Spell Engine and More RPG installed for other mods.

## Implementation boundary

- Apoli powers fire typed `andromeda_origins:enhanced_fx` actions at existing cast/impact points. Local-space and raycast-hit variants preserve the old positioned FX behavior without routing through Brigadier.
- `EnhancedFxDefinitions` reloads the JSON definitions from server data.
- `EnhancedFxCompat` checks optional mods and registered sounds/particles.
- The private particle bridge calls Spell Engine's `ParticleGroupBuilder`, batch presets, and `ParticleHelper.sendBatches(...)` reflectively.
- Gameplay logic, cooldowns, damage, conditions, and Champion parity do not depend on this system.


## v1.4.57 flashy-pass highlights
- Added dedicated Enhanced FX hooks for Faerie Flutter, Fenrkin Adrenaline, and the repeating pulse during Lichling Chimes of Necros.
- Rebalanced several previously subtle Origin events upward (Faerie, Fenrkin, Gorgon, Lichling, Siren, Wyverian) with larger particle counts, more layering, and stronger audiovisual readability.


## v1.4.60 FX expansion highlights
- **Nereid:** Convalescing Aura now has repeating water-circle/mist/bubble pulses, target-side healing FX, a larger release burst, and Submersion hit splashes.
- **Selkie:** Coastal Phalanx has caster/target shield-water signatures and refresh pulses; Surging Tides leaves short water trails; Sealskin Bastion visibly pulses while active.
- **Veilborn:** Reality Shatter, Unstable, target-side Transposition, Mirage entry/exit, and Champion Mirage Darkness retain dedicated purple/magenta arcane FX; the repeating active-Mirage pulse was removed in v1.4.60 so Undetectable stealth has no tracking particles.
- **Lichling:** Chimes of Necros now escalates visually on every Energy consumption, Doom applications mark targets, and the 10-stack detonation has a unique large necromantic burst.
- **Wyverian:** Ember Pyroclast has four visible charge stages plus a dedicated impact explosion; hover now produces a subtle downward wind vortex.
- **Humanity:** Indomitable's fatal-save trigger has its own shield/holy burst; Mortal Resolve visually escalates at 40, 20, and 10 seconds remaining and has an enhanced expiry cue.
- **Faerie:** Trickster now has item-specific enhanced particle signatures for its prank families without changing any prank mechanics.
- **Fenrkin, Manticore, Satyr, Siren, Gorgon:** added persistent/target-side/trailing FX for On the Hunt, Beast of Blood, movement abilities, Wail/Infatuation, and repeated petrification/transference.

All of these remain additive optional cosmetics. The normal Andromeda sound/particle fallback still runs when Spell Engine or More RPG Library is absent or Enhanced FX is disabled.


## v1.4.60 stealth / hydration notes
- Auroral Mirage no longer fires a repeating attached FX event while Undetectable. Activation particles happen before stealth is granted; break/exit presentation still occurs when stealth ends.
- Nereid Convalescing Aura now routes standard Selkie targets into their native hydration/recovery resources instead of granting a second Nereid Wet HUD timer.


## v1.4.61 FX highlights
- **Champion Manticore:** added a dedicated four-stage Bloodrift presentation (`manticore.bloodrift_enter`, `tear`, `exit`, `aftershock`) built around crimson rupture, claw tears, disappearance, and a delayed reappearance shockwave.
- **Fenrkin / Humanity / Lichling / Siren / Veilborn / Wyverian:** several marquee events were layered up with extra rings, helixes, shockwaves, and follow-through bursts for a more dramatic but still cosmetic-only presentation.


## v1.4.62 FX highlights
- **Champion Manticore Bloodrift:** entry/exit now pair the existing rupture presentation with cosmetic lightning. The claw/dragon-claw tear is spawned in front of the player rather than centered on the player model, and reappearance now layers a dedicated post-return roar before the delayed aftershock.


## v1.4.63 FX highlights
- **Champion Wyverian:** added a separate draconic presentation for Dragon's Breath and Dragon Charge. The new Champion-only event family uses purple arcane helixes, dragon-claw accents, staged charging, and a large magic impact instead of the standard flame-based Pyroclast presentation.


## Native MRPG presets in 1.4.84

More RPG Library 2.8.2 provides client-built effects. All 112 bundled events opt in. Each retains its existing event palette, including purple Champion draconic effects. Shockwaves, bursts, impacts, inward casts, swirls, auras, columns, clouds and slashes distinguish activations, marks, impacts and repeated feedback. These replace only the event's optional particle layer when playback is supported. Existing bundled cues, sound layers, power conditions, costs, damage and targeting remain unchanged. Spell Engine is not required for this native path.

An event may add this field alongside its existing `particles` and `sounds`:

```json
"mrpg": {
  "preset": "mrpg_lib:preset/shockwave",
  "radius": 2.2,
  "intensity": 0.5,
  "color": "77DDFF",
  "duration": 8,
  "attach": false
}
```

Use matching compatible MRPG builds on the server and clients. Before sending a native effect, Andromeda checks that every nearby recipient supports `mrpg_lib:fx_play`. Missing libraries, unavailable APIs, unknown effects or unsupported clients use the previous optional-particle path instead. Removing the `mrpg` field also selects that path. Each supported recipient receives one effect payload; the client builds its particles and applies MRPG's own density setting.

The bundled presets are short, finite bursts with no persistent server handles. Radius is bounded to 0.1–10, intensity to 0–2 and duration to 1–20 ticks. The existing enhanced-FX master and particles switches control this path; the sounds switch remains independent. Native support does not turn existing command-along-ray visuals into batched trails automatically.


Repeated ticks/trails/pulses use four-tick, low-intensity profiles to limit visual clutter. Other bursts last eight ticks. Auras and selected swirls use `attach: true` to follow their source for this brief lifetime; explicit target-position events always remain at their supplied position. Directional shapes use the source's look direction. No persistent channels or gameplay timers are added. Presets with independent explosion/storm audio are intentionally excluded so the sounds switch continues to control the optional audio layer.

| Origin | Native presentation |
| --- | --- |
| Arachne | Pale web clouds, spinning silk, grapple casts and dash arcs |
| Faerie | Existing trickster palettes, flutter bursts and illusion swirls |
| Fenrkin | Hunt rings, marking casts and light Adrenaline pulses |
| Gorgon | Gaze casts, aura extensions and transference swirls |
| Humanity | Resolve auras, stronger staged swirls/columns and save bursts |
| Lichling | Necrotic columns, chime swirls, doom casts and detonation rings |
| Manticore | Blood-colored lunge impacts, rift arcs and aftershocks |
| Nereid | Water rings, submersion swirls/clouds and healing bursts |
| Satyr | Rush casts, leap bursts and landing rings |
| Selkie | Tidal rings and brief protective auras |
| Siren | Infatuation swirls, wail rings and impact accents |
| Veilborn | Mirage swirls, darkness clouds, rift arcs and shimmer bursts |
| Wyverian | Separate existing flame/draconic palettes, staged casts and impact rings |

This native path replaces the optional event particle layer only. Existing Spell Engine definitions remain as fallback data, sounds remain independently configurable, and bundled base ability particles, ray trajectories and gameplay are unchanged.


## Dawnbreaker — separate command-only Origin

Standard and Champion Gorgon retain their existing kits. Operators can assign the new Origin with:

```mcfunction
origin set <player> origins:origin andromeda_origins:dawnbreaker
```

Under **Champion Origins → Dawnbreaker**, Fzzy Config exposes availability, inherited passives, Sovereign Gaze, Transference, Presence, Begone, Seraph’s Draw, radiant effects and stone appearance independently. Command-only availability is retained; Dawnbreaker is excluded from normal/random selection.

Bind **Dawnbreaker’s Presence**, **Begone** and **Seraph’s Draw (Hold)** separately in Controls under Andromeda Origins. All three start unbound to avoid pack conflicts. **Primary** activates Sovereign Ophidian Gaze; **Secondary** retains Transference. New descriptions list effective cooldowns; the new native resource timers do not receive shared Champion recovery a second time.

The halo and aura are Andromeda effects. **Seraph’s Draw uses the actual Dawnquiver bow renderer and Chorus/draw HUD from the installed Simply Swords mod**, rather than a replacement bow model. A client-only Dawnquiver visual entity follows the synchronized draw state; it never becomes a server entity or an inventory item. The scoped HUD adapter feeds our draw progress to Simply Swords’ own HUD helpers without changing the player's item-use or inventory state. Charge, release and impact sounds use its registered sound events. Compatible Simply Swords is required on both sides for Seraph’s Draw; all other Dawnbreaker abilities remain available without it.

No Simply Swords implementation or assets are copied into this mod. Its library remains separately installed. The adapter covers the installed 1.70.2 renderer/HUD and handles the released component format; newer capacity components are optional. Rendering helpers are implementation details and can change upstream. The real weapon's own HUD keeps priority if an actual Dawnquiver is equipped.

Presence uses sparse, finite MRPG light motes close to the caster instead of repeated cloud shockwaves across the ground. Cast/impact cues retain fine vanilla gold/jade dust where MRPG is unavailable. Bow release and impact use short golden MRPG bursts; golden projectile trails remain available without MRPG. The installed Dawnquiver arrow renderer supplies the flying light-arrow geometry, while the server keeps the existing damage and collision rules. Radiant wings render from both sides. Seraph’s Draw has no cooldown between draws; Begone can target living mobs as well as players. Existing Enhanced FX particle and sound switches remain effective. The separate radiant-effects switch also hides the custom light geometry while retaining gameplay; the stone-appearance switch controls native stone tint independently of petrification.

Stone tint and mossy-stone fragments preserve existing skins/artwork. Arbitrary Figura scripts/materials may control their own mesh colors; local Figura compatibility tests do not guarantee identical rendering for every third-party avatar. Readable petrification feedback and movement-key escape remain.

Light arrows use normal projectile collisions and armor, respect friendly teams/allied tame pets, cannot be picked up, and have a tagged 24-tick cleanup even after entity reload. No permanent light-bow entity or persistent cosmetic handle is created.

Dawnbreaker’s Presence uses separated, double-sided gold/jade halo bands and swept wings with smooth body rotation and body-size scaling. The halo sits behind the upper back and the wings attach near the shoulders on a separate rear plane. Adjacent core/glow bands avoid coplanar overlap; quiet motes replace the old crown cluster. Refreshing an active aura does not replay its cast sound. Begone has no departure/arrival particles or broadcast sound and no target warning text. Only a successfully teleported player receives a fixed, non-positional local heartbeat request through a dedicated ability event sharing the existing bundled heartbeat asset, so vanilla low-health heartbeat muting does not silence it; it respects the existing FX sound switch. Mobs are transported silently. Safe-landing preparation still completes before the abrupt teleport.

The heartbeat is played locally on the recipient’s client, independent of the camera/listener position while distant chunks arrive. It never emits a world sound. Matching updated client/server jars are required for this dedicated cue.
