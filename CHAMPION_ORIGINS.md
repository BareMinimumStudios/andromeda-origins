# Champion Origins

**Current release: v1.4.70.** Champion variants retain the normal Origin abilities and strengths while removing racial weaknesses/self-debuffs. Player-facing ability cooldowns recover at twice the standard rate while charge times, resource costs, and internal mechanical cadence remain unchanged. Champion Veilborn Auroral Mirage is a deliberate exception with a fixed 5-second cooldown.

Champion Origins are administrative variants of all 13 standard Andromeda Origins. They keep the Origin's identity-defining strengths and active abilities while removing racial weaknesses and clear self-debuffs.

## Selection behavior

Every Champion entry is registered with:

```json
"unchoosable": true
```

They therefore do **not** appear in normal Origins selection, but remain assignable through `/origin set`.

## Assignment

```mcfunction
/origin set <player> origins:origin andromeda_origins:champion_<origin>
```

## Design rules

Champions generally retain:

- active abilities,
- passive strengths,
- identity-defining size/attribute bonuses,
- charge times,
- resource consumption,
- a 2× recovery rate for player-facing ability cooldowns (50% shorter effective cooldown), except explicit Champion-specific overrides such as Veilborn Auroral Mirage at 5 seconds,
- effects/debuffs intentionally applied to enemies,
- target-facing counterplay.

Positive protections are not treated as Champion drawbacks. In particular, the shared 50% projectile-damage reduction used by movement abilities is preserved for Champions.

Champions generally remove:

- negative base stats,
- environmental/racial weakness states,
- diet restrictions,
- iron sensitivity,
- equipment restrictions,
- clear ability-side penalties applied to the Champion themselves.

## Champion roster

| Champion | Origin ID | Removed drawbacks / Champion-specific behavior |
|---|---|---|
| Champion Arachne | `andromeda_origins:champion_arachne` | No health/damage-taken, sunlight, prolonged-fire, exhaustion, or diet penalties. Cobweb Fatigue is not applied; Eightfold Swiftness retains the normal projectile-damage reduction. |
| Champion Faerie | `andromeda_origins:champion_faerie` | No health/damage, iron, or inventory-Wither penalties. |
| Champion Fenrkin | `andromeda_origins:champion_fenrkin` | No Wet/cramped, armor-Wither, diet, or stalking-slowdown penalties. Stamina Surge retains the normal projectile-damage reduction. |
| Champion Gorgon | `andromeda_origins:champion_gorgon` | No iron sensitivity, falling slowdown, or diet restriction. Gaze target counterplay remains. |
| Champion Humanity | `andromeda_origins:champion_humanity` | Normal health/hunger efficiency. Mortal Resolve becomes **Mortal Triumph**, which does not force death when the minute ends. |
| Champion Lichling | `andromeda_origins:champion_lichling` | No sunlight, prolonged-fire, iron, diet, or Chimes movement-slowdown penalties. |
| Champion Manticore | `andromeda_origins:champion_manticore` | No Wet/cramped, food-efficiency, diet, or Beast of Blood Famished penalties. Ravenous Lunge retains the normal projectile-damage reduction. Also gains **Bloodrift** on Sneak + Secondary: an Undetectable reality-tear stealth ability with a dedicated 10-second cooldown; cosmetic lightning strikes on entry/exit, and attacking or taking damage forces Bloodrift to end. |
| Champion Nereid | `andromeda_origins:champion_nereid` | No land movement/damage/vulnerability penalties or diet restriction. |
| Champion Satyr | `andromeda_origins:champion_satyr` | No health/swim, boot, max-Momentum hunger, or diet penalties. Rush retains the normal projectile-damage reduction. |
| Champion Selkie | `andromeda_origins:champion_selkie` | No dry-out/Silenced cycle, land-speed, damage, diet, or Sealskin Bastion slowdown penalties. Surging Tides retains the normal projectile-damage reduction. |
| Champion Siren | `andromeda_origins:champion_siren` | No damage, prolonged-fire, iron, or diet penalties. |
| Champion Veilborn | `andromeda_origins:champion_veilborn` | No health, armor-weight, shield, water damage/Silence, or Curtain Step self-Reality-Shatter penalties. Entering/leaving Auroral Mirage applies a 5-second Darkness burst to nearby entities within 8 blocks; Auroral Mirage itself has a fixed 5-second cooldown. |
| Champion Wyverian | `andromeda_origins:champion_wyverian` | No movement, Wet/cramped, propulsion, diet, or underwater breath lockout penalties. Gusts of Freedom retains the normal projectile-damage reduction. Champion-only exception: Ember Ray / Ember Pyroclast are replaced by **Dragon's Breath / Dragon Charge**, using draconic presentation and magic damage while retaining the same core range, charge, resource, and impact structure. |

## v1.4.45 Veilborn note

The standard Veilborn rebalance does not reduce Champion Veilborn. Champion Veilborn already removes the standard health penalty, armor-weight slowdown, water damage/Silence penalty, shield restriction, and Curtain Step self-Reality-Shatter. Shared Undetectable improvements still apply to Auroral Mirage: mob AI cannot target the Champion while Undetectable and normal held-item rendering is hidden.

## AoE self-hit protection

Damaging shared ability implementations that could include their own caster explicitly exclude distance `0` targets. The current safeguards cover:

- Satyr's Landing,
- Manticore Ravenous Lunge collision damage,
- Wyverian Ember Pyroclast explosion damage.
- Champion Wyverian Dragon Charge magic burst (caster is temporarily tagged out of its own AoE).

This is primarily important for Champion variants because they are intended to remove self-inflicted ability drawbacks rather than introduce new ones.

## Exact commands

```mcfunction
/origin set <player> origins:origin andromeda_origins:champion_arachne
/origin set <player> origins:origin andromeda_origins:champion_faerie
/origin set <player> origins:origin andromeda_origins:champion_fenrkin
/origin set <player> origins:origin andromeda_origins:champion_gorgon
/origin set <player> origins:origin andromeda_origins:champion_humanity
/origin set <player> origins:origin andromeda_origins:champion_lichling
/origin set <player> origins:origin andromeda_origins:champion_manticore
/origin set <player> origins:origin andromeda_origins:champion_nereid
/origin set <player> origins:origin andromeda_origins:champion_satyr
/origin set <player> origins:origin andromeda_origins:champion_selkie
/origin set <player> origins:origin andromeda_origins:champion_siren
/origin set <player> origins:origin andromeda_origins:champion_veilborn
/origin set <player> origins:origin andromeda_origins:champion_wyverian
```

## Returning to a standard Origin

Use the same layer with the standard ID, for example:

```mcfunction
/origin set <player> origins:origin andromeda_origins:fenrkin
```

Changing Origins runs the corresponding selection initialization so owned cooldown/resources return to their configured starting values.

- **Champion Fenrkin Adrenaline:** 6-minute base cooldown; Champion 2× cooldown recovery makes the effective cooldown 3 minutes.
