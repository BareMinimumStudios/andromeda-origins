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
