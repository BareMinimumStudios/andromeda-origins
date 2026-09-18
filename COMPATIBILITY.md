# Andromeda Origins — Compatibility & State Safety

This document describes the compatibility and state-safety behavior in **v1.4.74**. Optional Spell Engine / More RPG Library audiovisual compatibility remains additive. v1.4.74 isolates standard Nereid gills from temporary `minecraft:state` power lifecycles and automatically repairs missing gills on join; the v1.4.70 iron migration, v1.4.71 Arachne/Humanity guards, and v1.4.72 Wyverian VFX cleanup remain intact.



### v1.4.74 state / concurrent-cast safety

- Veil Transposition no longer creates globally tagged `origin` / `destination` marker entities. The successful 32-block raycast now performs an atomic actor/target position exchange, preventing simultaneous Veilborn casts from selecting or deleting each other's temporary markers. Existing warp helpers are retained only for cosmetic teleport feedback.
- The old `common/debug` Origin-loss callback no longer calls broad `revoke_all_powers` operations on generic Apoli sources. It invokes an Andromeda namespace-scoped transient cleanup instead, avoiding collateral state removal from other Origins addons.
- Selkie retaliation now uses the same `minecraft:debuff` source for grant and expiry. A one-shot player migration plus a helper-side legacy cleanup removes old stuck `minecraft:state` ownership.

### v1.4.74 legacy Origin repair hardening

- `/andromedaorigins repair <player>` removes power ownership from registered Origin sources the player no longer has selected and explicitly recognizes `medievalorigins:*` sources even if Medieval Origins is no longer registered.
- Loaded `medievalorigins:*` helper powers and Medieval-owned `origins:carnivore` / `origins:vegetarian` sources are removed when stale, while ownership from a currently selected Origin is preserved.
- Orphaned `medievalorigins:*` attribute modifiers are removed across every instantiated player attribute before Andromeda rebuilds the current Origin's attributes. This covers Medieval size/health/movement/hitbox modifiers instead of resetting unrelated modded attributes wholesale.
- Pehkui `eye_height` is reset only when Medieval Pixie residue is positively detected and Pehkui is present. Legacy cleanup is manual through `/repair`; it does not run as a recurring join/tick scan.
- Nereid ally marks are now timed for 15 seconds and use the dedicated `andromeda_origins:nereid_mark` source; old indefinite player marks from `minecraft:state` are removed on join/repair. Nereid Aura/Submersion hydration is kelp-mark-gated and restricted to standard Selkie/Nereid hydration systems. Marked non-aquatic allies are protected from hostile Submersion; aquatic targets remain immune to forced drowning but are still subject to Submersion's sink/movement suppression. No fake hydration state is applied to Sirens or other aquatic Origins.

### v1.4.71 Arachne collision / Humanity finality guards

- Arachne no longer uses `origins:phasing` to implement cobweb slowdown immunity. A player-only movement hook skips `slowMovement` only for players tagged `arachne` and only for blocks in `origins:cobwebs`, preserving web mobility without exposing Apoli phasing collision hooks to the climbing power. Standard and Champion Arachne share this behavior.
- Standard Humanity's Indomitable `prevent_death` child is inactive while the `andromeda_mortal_resolve` tag is present, and the Primary activation itself is blocked for that same standard final-stand state. Champion Mortal Resolve uses a different tag and remains intentionally non-lethal.
- Mortal Resolve expiry keeps its active tag through `player.kill()` and clears it only after a successful death, closing the previous one-call gap where Indomitable could become eligible again.

### v1.4.70 legacy iron-power migration
Existing standard Faerie, Gorgon, and Lichling players are checked once when they join. If their already-selected Origin is missing any current child of `andromeda_origins:common/witheringironweakness`, Andromeda grants only the missing child powers under the existing Origin source and syncs Apoli once. It does not re-select the Origin, reset cooldowns/resources, or run a recurring tick scan. The same narrow reconciliation is also performed by `/andromedaorigins repair <player>` before the existing attribute repair.

## Spell Engine / More RPG Library (optional enhanced FX)

- `spell_engine` and `more_rpg_classes` are `suggests`, not `depends`.
- No Spell Engine or More RPG Java class is referenced from Andromeda's normal class-loading path. Spell Engine particle calls are resolved reflectively only after Fabric reports the mod as loaded.
- More RPG-specific entries are skipped unless `more_rpg_classes` is loaded; More RPG's own Spell Engine / Spell Power dependencies remain More RPG's responsibility.
- Existing Andromeda custom sounds and vanilla particles are left in place as the fallback. Enhanced FX are additive rather than replacements.
- FX selection is server-data-driven through `data/andromeda_origins/andromeda_fx/*.json`; Java only handles optional-library detection, registry safety, packet emission, and sound playback.
- The Spell Engine helper already checks whether its S2C particle packet can be sent to each tracking client.
- A malformed or missing optional FX definition does not invalidate an Origin power; the internal FX command simply produces no enhanced layer.
- Runtime switches are stored in `config/andromeda_origins_fx.json` and can be changed with `/andromedaorigins enhanced_fx ...`.

This layer does not grant spells, attributes, spell power, or RPG classes. Andromeda uses only the libraries' registered audiovisual assets/particle transport for presentation.

## Apoli server performance

Andromeda v1.4.65 includes two compatibility optimizations for Apoli 2.12.x hot paths observed in server profiling:

- `PowerHolderComponentImpl#getPowerTypes(Class, boolean)` caches only the list of owned power objects matching a requested power-type class. Active/conditioned state is still checked on every query, so a power becoming active or inactive is not cached incorrectly. The membership cache is invalidated whenever powers are added, removed, loaded from NBT, or synchronized.
- `EntitySetPowerType.integrateUnloadCallback` is replaced with an indexed-holder equivalent. Apoli's stock callback walks every loaded entity in every dimension whenever a non-player entity is destroyed; Andromeda keeps a weak index of living entities that actually own an EntitySet power and checks only those holders. If reflection cannot resolve the expected Apoli API, the optimization declines to cancel the original callback.

The server profiler supplied for this issue was running **Apoli 2.12.0-pre.2**. Andromeda intentionally remains on and now pins **Origins 1.13.0-pre.2+mc.1.21.1 / Apoli 2.12.0-pre.2+mc.1.21.1**. The local performance compatibility layer backports targeted hot-path relief for pre.2 instead of requiring the pre.3 release line. Andromeda's own login handshake still enforces an exact Andromeda Origins version match.

### Apoli pre.2 performance backport

Because the server must remain on Origins/Apoli pre.2, Andromeda provides narrow compatibility mixins instead of requiring pre.3:

- class-specific `getPowerTypes(Class, includeInactive)` membership is cached and invalidated when the holder's powers change; active conditions are still checked live;
- `EntitySetPowerType.integrateUnloadCallback` tracks actual EntitySet holders instead of scanning every loaded entity in every world for each destroyed non-player entity;
- every optimization is optional/fail-open: if the expected pre.2 internals are unavailable, Apoli's stock implementation is allowed to run.


## Incapacitated

Incapacitated is optional. Andromeda Origins uses a reflection bridge so the mod still loads when Incapacitated is absent.

### Lichling revival

Lichling revival does not blindly force another mod's downed flag. Self/target revive routes through the internal safe-revive bridge, which:

1. checks Incapacitated's own player-data state,
2. only revives when that player is actually downed,
3. clears transient `lastDmgTaken` / `lastHealthBeforeDamage` tracking after an Andromeda-triggered revive.

Lichling self-healing uses Origins' native heal action rather than intentionally routing through the damage/death pipeline.

### Humanity — Mortal Resolve

Mortal Resolve can activate only while Incapacitated reports the Human as downed. During the one-minute final stand, Andromeda temporarily arms Incapacitated's down counter so a real death cannot simply become another downed state. v1.4.47 tracks that temporary sentinel through the final death and restores Incapacitated's configured counter/timer after respawn. A persistent recovery tag also protects the handoff across a server restart or delayed respawn. Champion Humanity's Mortal Triumph never arms the forced-death sentinel.

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

The marker tag is added/removed by the shared power and refreshed once per second while active. On the pinned pre.2 runtime, server-side mob targeting and the data-driven `prevent_feature_render` power remain the authoritative stealth behavior. Client-only helpers that rely on the command-tag mirror are best-effort compatibility hooks rather than a reason to require pre.3.

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

## Exact Andromeda Origins version handshake

Andromeda Origins uses Fabric's login-query networking stage to require an exact client/server mod-version match before world join. The server sends an `andromeda_origins:version_check` query; a client without Andromeda Origins does not understand the channel and is disconnected, while a client with the mod responds with its runtime Fabric Loader metadata version. Any version/protocol mismatch is disconnected with a message showing the server and client versions. This is separate from `fabric.mod.json` dependency checks, which only validate each installation locally.

## Admin recovery command

```mcfunction
/andromedaorigins repair <player>
```

The command is intended for interrupted/broken current-state recovery. Before the existing state cleanup, it rebuilds the raw player attributes Andromeda currently manages (`max_health`, `movement_speed`, `scale`, `step_height`, `armor`, `armor_toughness`, `knockback_resistance`, and `attack_speed`) from Minecraft's clean player defaults, removes/re-applies the currently granted Apoli `AttributeModifying` powers, and then heals to the newly reconstructed maximum health. The effective result is the clean player base plus the currently selected Origin/Champion modifiers, not a vanilla-player final stat line. It does **not** re-run `/origin set`, so selection callbacks, cooldowns, resources, and active timers are preserved.

Before rebuilding Andromeda attributes, the full repair now also reconciles stale **Origin-owned Apoli sources**. Any registered Origin source that is no longer selected is removed, and the retired `medievalorigins:` namespace is recognized even when Medieval Origins is no longer installed. Because Origins uses the Origin ID as the power source, this also cleans vanilla powers such as `origins:carnivore` or `origins:vegetarian` when an old Medieval Origin originally granted them.

For Medieval Origins specifically, repair also removes orphaned `medievalorigins:` attribute modifiers across the player's custom attribute instances, clears its transient `siren_seduce` command tag, and—when Pehkui is installed—resets the `pehkui:eye_height` scale type that Medieval Pixie's mount power could leave altered. Andromeda's normal scale remains native Minecraft scaling; no other Pehkui scale types are reset. This legacy cleanup is **manual-only** through `/andromedaorigins repair` rather than an automatic join migration.

The full repair then clears known temporary Andromeda control states/source counters, removes Andromeda-owned transient powers from the historical generic `minecraft:state` / `buff` / `debuff` / `ccontrol` sources, removes a stale `andromeda_undetectable` marker if present, invokes Incapacitated repair behavior when available, repairs a stale negative unlimited-down counter, and resets Incapacitated transient damage tracking. The transient cleanup is namespace-scoped and no longer revokes unrelated addon powers merely because they share one of those generic source IDs. A legitimately active Undetectable power reasserts its marker on the next one-second sync.

Attribute-only recovery is also available with:

```mcfunction
/andromedaorigins repair_attributes <player>
```

## Namespace / migration

All current Origins, powers, resources, functions, tags, registered icon items, models, textures, sounds, and internal references use:

```text
andromeda_origins:
```

Automatic join migrations remain narrowly scoped to known Andromeda state (iron weakness, Nereid gills, legacy indefinite Nereid marks, and the pre-fix Selkie retaliation source). The manual repair command additionally understands stale registered Origin sources plus the retired `medievalorigins:` namespace; it does not attempt to guess arbitrary unrelated player NBT.

## Incapacitated hard-death notes (v1.4.47)

- Mortal Resolve temporarily forces Incapacitated's internal down counter negative so the final-stand death cannot become another down. v1.4.47 explicitly restores the configured counter after that death.
- Players carrying a stale negative counter from older Andromeda versions are repaired automatically on join/respawn when `UnlimitedDowns=true`.
- `UnlimitedDowns` only prevents Incapacitated from consuming its down counter. Incapacitated can still intentionally hard-kill through its own instant-kill damage tag/config, overkill-damage rule, bleed-out timeout, give-up behavior, or full-server-kill rule when those options are enabled. Andromeda does not override those settings.

### Incapacitated overkill damage

In Incapacitated 2.0.x, `ShouldDieOnOverkillDamage` / `shouldDieOnOverkillDamage` is an upstream Incapacitated setting and defaults to `true`. Its documented rule hard-kills instead of downing when the lethal hit's recorded damage is greater than **max health + current health**. At full health that threshold is greater than 2× max health; when already injured, the threshold is lower. Andromeda Origins neither implements nor overrides this rule.

The upstream public README/changelog does not state whether the recorded damage used by this comparison is sampled before or after armor/protection mitigation. Andromeda therefore leaves that implementation detail to Incapacitated rather than adding a competing damage hook.
