# Optional Enhanced Origin FX

**Current state: v1.4.75.** This layer lets Andromeda Origins use Spell Engine and More RPG Library particles/sounds when those mods are installed, while keeping the base mod fully functional without them.

## v1.4.72 Wyverian camera-clear pass

- Standard Pyroclast and Champion Dragon Charge buildup no longer use center-aligned helix batches on the caster; their enhanced charge particles now use the feet-level casting layout so first-person aim is not covered.
- Champion Dragon Breath's enhanced caster burst was likewise moved from a forward center cone to feet-level casting particles.
- Champion Dragon Charge impact smoke now uses a smaller batch with a fast fade and faster playback so the smoke cloud does not hang in the scene.
- Vanilla Ember/Dragon ray and projectile trail cosmetics are offset forward independently of their gameplay raycasts, so damage/hit detection is unchanged.


## Dependency model

- `spell_engine`: optional (`suggests`).
- `more_rpg_classes`: optional (`suggests`).
- Andromeda does **not** bundle either project and does not declare either as a runtime dependency.
- More RPG Library still requires its own dependencies (including Spell Engine and Spell Power) when More RPG itself is installed.

The bridge deliberately uses reflection for Spell Engine's particle API. No Spell Engine class appears in Andromeda's normal linkage path, so a server/client without Spell Engine can load Andromeda normally.

## Fallback behavior

Every original Andromeda custom sound and vanilla particle remains in the power JSON. Enhanced FX are an additive layer. If the libraries are absent, disabled, missing an asset, or the bridge cannot resolve the external API, the extra layer is skipped and the original presentation still runs.

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

Supported particle presets in v1.4.65 are `impact`, `casting`, `travel`, `cloud`, `shockwave`, `ground`, `placed`, `cone`, `helix`, and `pop_up`. `angle` is used by `cone`; `pre_travel` is used by `shockwave`; `degrees_per_tick` and `offset` are used by `helix`. Definitions may also set color, glow, opacity/fade, scale growth, playback speed, lifetime variance, motion, gravity, drag, collision, and attachment flags.

## Current event coverage

The v1.4.65 layer currently defines **109 enhanced FX events** across all 13 Origins. The event set now includes one-shot casts/impacts plus recurring channel/aura pulses, target-side feedback, movement trails, staged charge effects, and state escalations.

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
| Veilborn | 8 |
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

- Apoli powers fire the internal cosmetic event with `andromedaorigins internal_fx <event>` at existing cast/impact points.
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
