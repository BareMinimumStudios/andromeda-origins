# Andromeda Origins — Compatibility & State Safety

This document describes the compatibility and state-safety behavior in **v1.4.46**. Compatibility behavior is unchanged from v1.4.45; v1.4.46 only corrects HUD resource-bar sheet/index routing.

## Incapacitated

Incapacitated is optional. Andromeda Origins uses a reflection bridge so the mod still loads when Incapacitated is absent.

### Lichling revival

Lichling revival does not blindly force another mod's downed flag. Self/target revive routes through the internal safe-revive bridge, which:

1. checks Incapacitated's own player-data state,
2. only revives when that player is actually downed,
3. clears transient `lastDmgTaken` / `lastHealthBeforeDamage` tracking after an Andromeda-triggered revive.

Lichling self-healing uses Origins' native heal action rather than intentionally routing through the damage/death pipeline.

### Humanity — Mortal Resolve

Mortal Resolve can activate only while Incapacitated reports the Human as downed. During the one-minute final stand, Andromeda prepares Incapacitated's down counter before a real death so the player does not simply enter another downed state. When Mortal Resolve expires, the ordinary Human follows the forced-death path; Champion Humanity's Mortal Triumph does not.

## Champion Origins

Champion Origins are real entries in the normal `origins:origin` layer with `unchoosable: true`. This keeps them command-assignable while hiding them from normal selection.

A hidden `andromeda_origins:common/champion` marker lets standard/Champion variants share active powers while conditionally suppressing standard-origin drawbacks.

### Self-hit safeguards

Shared damaging AoE abilities explicitly exclude the acting player where self-hit was possible. This currently covers the Champion-accessible implementations of:

- Satyr's Landing stomp damage,
- Manticore Ravenous Lunge collision damage,
- Wyverian Ember Pyroclast explosion damage.

Champion variants are still damageable normally by other entities, and enemy-facing counterplay remains intact.

See [CHAMPION_ORIGINS.md](CHAMPION_ORIGINS.md).

## Figura

Figura is optional and is listed only as a suggested integration. Andromeda Origins does not import Figura classes at runtime.

- Legacy `common/figura_1` through `figura_5` resources remain for older avatars.
- Legacy writes use explicit boolean `set 0` / `set 1` behavior.
- Figura states are reset on Origin selection and respawn to reduce stuck animation states.
- Mortal Resolve has a dedicated synced semantic resource.
- A bundled avatar-side Lua helper exposes named states.

Figura ExtraBone is also optional and is treated strictly as a skeleton/blending aid for Figura + PlayerAnimator/Emotecraft workflows.

### Armor Model API custom armor

Armor Model API renders registered geo armor through a custom rendering path that normally bypasses Figura's vanilla armor visibility handling. v1.4.44 adds an optional client mixin at Armor Model API's shared dispatcher so that custom armor follows the same Figura slot visibility state. This bridge was authored against **Armor Model API 1.1.0**, **Figura 0.1.6**, and the **Rogues & Warriors 3.1.1** 1.21.1 source supplied for testing.

- `vanilla_model.ARMOR:setVisible(false)` hides all Armor Model API armor slots for that avatar.
- Individual Figura slot groups (`HELMET`, `CHESTPLATE`, `LEGGINGS`, `BOOTS`) are also honored.
- Figura's `VANILLA_MODEL_EDIT` permission must allow the visibility edit.
- Figura and Armor Model API are both optional; the bridge uses no hard Figura linkage.
- If Figura is missing or its expected 0.1.6-era visibility internals cannot be resolved, custom armor renders normally rather than failing the player render.
- The compatibility is generic to Armor Model API, so content mods using that renderer (including Rogues & Warriors armor sets) do not need item-ID-specific handling.

See [FIGURA_COMPAT.md](FIGURA_COMPAT.md).

## Detection / Undetectable

`andromeda_origins:common/undetectable` is the shared stealth state used by abilities such as Faerie concealment and Veilborn Auroral Mirage. In v1.4.45 it covers both Andromeda detection logic and vanilla mob AI:

- Andromeda tracking/glow powers continue to exclude Undetectable targets.
- Vanilla `TargetPredicate` checks reject an Undetectable target when the observer is a mob, including predicates that normally ignore visibility.
- Brain `Sensor` target checks reject Undetectable targets.
- Direct `MobEntity` target assignment refuses an Undetectable target, and reads of an already-held target return no target while the state remains active.
- The shared power hides armor/outline as before and additionally suppresses the standard held-item feature. Client mixins also suppress first-person and vanilla third-person held-item rendering while the synchronized `andromeda_undetectable` command tag is present.
- The optional Armor Model API dispatcher bridge also treats Undetectable as hidden, so registered custom geo armor is cancelled instead of floating around an invisible player.

The marker tag is added/removed by the shared power and refreshed once per second while active. Origins/Apoli 1.13.0-pre.3 includes client synchronization for command tags, allowing the same state to drive client rendering without a hard Apoli Java dependency.

This is target immunity, not general invulnerability: area damage, projectiles already in flight, traps, commands, and environmental damage are not cancelled merely because the player is Undetectable. Arbitrary custom renderers that do not use vanilla held-item rendering may require their own compatibility hook.

## Veilborn balance

Standard Veilborn v1.4.45 values:

- maximum health penalty: `-2` HP, for **18 HP / 9 hearts** total;
- each armor/armor-toughness threshold applies **-10% movement and swim speed**;
- Wet/Unstable damage dealt modifier: **-60%** for melee and projectiles;
- self-applied Reality Shatter maximum resource duration: **90 seconds**.

Water still applies Silence and keeps the existing lingering-duration behavior. Champion Veilborn continue to omit the standard health, armor-weight, water, shield, and Curtain Step self-Reality-Shatter drawbacks.

## Crowd-control state counters

Shared restraint/control counters use a single owner for decrement-on-loss behavior. In particular, Arachne Webbed and the historical Gorgon constriction path were cleaned so a normal expiry does not decrement shared restraint sources twice.

## Siren Shrieking Wail

Shrieking Wail uses a direct fan of Apoli raycasts. It does **not** spawn temporary armor stands for hit detection. A legacy cleanup command remains to remove old `shriek`-tagged armor stands left behind by pre-fix builds.

## Gorgon carry removal

Gorgon's old entity/player pickup/carry mechanic was removed. The associated pickup helpers, carry logic, grab audio, and dismount-prevention mixin are not part of the current implementation.

## Native scaling

Origin size and step-height behavior use Minecraft's native attributes. **Pehkui is not required.**

## Dietary compatibility

Andromeda provides common diet powers/tags for:

- Carnivore
- Vegetarian
- Pescatarian
- Raw-food restrictions/bonuses

The data also includes compatibility-oriented food tags used with the server's food/mod setup, including Dietary Delights integration.

## Iron weakness

Origins with iron sensitivity use shared Andromeda iron tags so both vanilla and supported modded iron-containing items can trigger the appropriate weakness behavior. Inventory-based withering iron weakness and iron-weapon vulnerability are implemented separately where required by the Origin.

## Attribute modifier safety

Explicit persistent modifier IDs use the `andromeda_origins:` namespace. Context-relative Apoli `*:*` modifier IDs remain context-relative rather than being treated as one shared literal runtime identifier.

## Origin-selection initialization

When a standard or Champion Andromeda Origin is selected/reselected/command-assigned, its owned cooldowns/resources are restored to configured starting values. Figura compatibility resources are also reset. This is selection-time initialization, not an intentional reconnect refresh.

## Admin recovery command

```mcfunction
/andromedaorigins repair <player>
```

The command is intended for interrupted/broken current-state recovery. It clears known temporary Andromeda control states/source counters, removes a stale `andromeda_undetectable` marker if present, invokes Incapacitated repair behavior when available, resets Incapacitated transient damage tracking, and restores the player's current maximum health. A legitimately active Undetectable power reasserts its marker on the next one-second sync.

## Namespace / migration

All current Origins, powers, resources, functions, tags, registered icon items, models, textures, sounds, and internal references use:

```text
andromeda_origins:
```

The project does not ship a migration layer for unsupported prototype namespaces or arbitrary old player NBT.
