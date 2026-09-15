# v1.4.70

## Migration-safe iron weakness
- Existing standard **Faerie, Gorgon, and Lichling** players are now reconciled automatically when they join. If their current selected Origin is missing any current child power from `andromeda_origins:common/witheringironweakness`, Andromeda grants only the missing pieces under that Origin's existing power source.
- This specifically fixes legacy players whose saved Apoli power membership predates the newer iron sampler/resource/damage children and previously required an Origin reselect before the updated weakness worked.
- The migration does **not** re-select the Origin, revoke/regrant already-correct powers, reset ability cooldowns/resources, or rebuild every player power. If nothing is missing, it performs no mutation and no Apoli sync.
- `/andromedaorigins repair <player>` now runs the same narrow iron-power reconciliation before the existing Origin-aware attribute rebuild, allowing staff to repair an already-online affected player without forcing a reconnect or Origin reselect.

## Performance / compatibility
- The automatic migration is a **one-shot join check**, not a tick callback. It inspects only the player's selected Origin and the single iron-weakness multiple power; no world/entity scan or inventory scan is added.
- Kept Andromeda's reflection-based Apoli/Origins compatibility boundary and fail-closed behavior. Origins/Apoli remain pinned to **Origins 1.13.0-pre.2+mc.1.21.1 / Apoli 2.12.0-pre.2+mc.1.21.1**.
- Existing v1.4.69 iron damage remains unchanged: 1 wither damage every 20 ticks while sampled iron is present, with a 5-tick inventory sampler and fast Wither removal after the final iron item is dropped.

# v1.4.69

## Iron weakness passive damage + sampler cleanup
- Standard **Faerie, Gorgon, and Lichling** inventory iron weakness now deals **1 wither damage (half a heart) every 20 ticks / 1 second** while an iron-tagged item remains anywhere in the inventory, restoring a more noticeable passive health drain.
- The short Wither I marker is still refreshed for only **10 ticks**, so the inventory-caused Wither icon/effect clears within roughly **0.5 seconds** after the final iron-tagged item is removed. The damage state is cleared by the same 5-tick sampler, so passive iron damage also stops within at most about 0.25 seconds of the next sample.
- Moved the expensive `origins:inventory` condition inside the 5-tick periodic action instead of leaving it as the `action_over_time` power condition. Apoli checks a power condition every server tick, so this change makes the full inventory scan genuinely run only **4 times per second per affected player**, while the per-tick active check remains a cheap resource/state check.
- Iron-weapon hit Wither and Champion iron exemptions are unchanged. Origins/Apoli remain pinned to pre.2.

# v1.4.68

## Siren carnivore enforcement
- Standard **Siren** now uses a dedicated strict carnivore rule instead of the shared diet-exemption set. Non-meat foods such as golden apples and golden carrots are no longer valid Siren food.
- Actual beverages and potion-style consumables remain usable through a Siren-specific drink exemption tag, so the diet change does not block normal drinks.
- Champion Siren remains unchanged and retains the established Champion exemption from racial diet restrictions.

## Fenrkin Origin Toggle isolation
- Replaced standard and Champion Fenrkin's Apoli `TogglePowerType` dependency with a dedicated persisted 0/1 toggle resource controlled only by the **Origin Toggle** key.
- Fenrkin darkvision and step assistance now read that dedicated resource directly. Using **Primary / On the Hunt** can no longer re-enable step assistance while the Origin Toggle is off.
- The toggle still defaults to ON when Fenrkin is selected/re-selected, matching the previous behavior, and the dedicated Origin Toggle binding remains independently rebindable.

## Validation
- Revalidated all modified power/origin/tag JSON after the diet and toggle changes.
- Origins/Apoli remain pinned to **Origins 1.13.0-pre.2+mc.1.21.1 / Apoli 2.12.0-pre.2+mc.1.21.1**.

# v1.4.67

## Responsive iron weakness
- Reworked the inventory-based iron weakness used by standard **Faerie, Gorgon, and Lichling**. Iron inventory checks now run every **5 ticks** and refresh a short Wither I marker, so inventory-caused Wither disappears within roughly **0.5 seconds** after the final iron-tagged item is removed instead of lingering for up to 10 seconds.
- Preserved normal Wither I damage cadence with a lightweight 40-tick damage pulse while the short iron-caused Wither marker is active. This avoids the short refresh duration accidentally suppressing Wither's normal damage.
- The damage pulse deliberately yields to any longer or stronger external Wither effect, so ordinary Wither from other sources is not replaced or cleared when iron is removed.
- Fixed **standard Siren** so iron-weapon hits now actually inflict the Wither I described by its existing ability text. Siren still does **not** gain the inventory-carrying iron weakness.
- Champion variants remain unchanged and continue to have their iron weaknesses removed.

## Performance / safety
- The responsive inventory check applies only to Origins that actually own the inventory iron-weakness power and runs four times per second; no global entity scan or per-tick inventory scan was added.
- Revalidated all data JSON and iron-weakness references after the change.

# v1.4.66

## Origins / Apoli pre.2 compatibility
- Restored the supported runtime to **Origins 1.13.0-pre.2+mc.1.21.1 / Apoli 2.12.0-pre.2+mc.1.21.1** and pinned both exact versions so the mod will not require or silently accept the pre.3 line.
- Kept Andromeda's server-side Apoli hot-path optimizations from v1.4.65. These now act as a targeted performance backport for pre.2 rather than depending on pre.3's upstream lookup changes.
- Verified the compatibility layer against the actual upstream **Apoli 2.12.0-pre.2** source shape: `PowerHolderComponentImpl#getPowerTypes(Class, boolean)` still performs the full power scan, and `EntitySetPowerType#integrateUnloadCallback` still scans every loaded entity in every server world when a non-player entity is destroyed.
- No Origin balance, powers, Champion behavior, FX, repair logic, or Andromeda version-handshake behavior changed in this release.

## Performance compatibility notes
- `ApoliPowerHolderCacheMixin` continues to cache only **power-type class membership**. Dynamic `isActive()` conditions are still evaluated for each lookup so conditioned powers keep their normal behavior.
- `ApoliEntitySetUnloadMixin` continues to replace the pre.2 world-wide unload scan with an indexed holder path. If the expected pre.2 internals cannot be resolved, it falls back to Apoli's stock behavior instead of hard-failing.

## Validation
- Revalidated all JSON resources after the dependency rollback.
- Confirmed the mixin target names and reflected pre.2 fields/methods against upstream commit `c88fac3` (Apoli 2.12.0-pre.2).
- Static validation only in this environment; the Gradle wrapper still cannot download Gradle 8.8 here.

# v1.4.65

## Server-side Apoli performance
- Added a compatibility cache for Apoli power-type lookups. The cache stores only class membership and still evaluates each power's active condition on every query, preserving conditioned-power behavior while avoiding repeated scans across every power on an entity from hot attribute, collision, invisibility, fire, tag, and movement hooks.
- Replaced Apoli's EntitySet unload cleanup scan with an indexed holder path. Instead of walking every loaded entity in every dimension whenever a non-player entity is destroyed, the compatibility layer visits only living entities that currently own an `EntitySetPowerType`. If the upstream API cannot be resolved, Andromeda falls back to Apoli's original callback.
- Tightened the required Apoli version to **2.12.0-pre.3+mc.1.21.1 or newer**. Apoli pre.3 itself includes upstream power-lookup optimizations; servers still running pre.2 must update Apoli on both server and client installations.
- These optimizations do not alter Origin balance, power conditions, cooldowns, FX, Champion behavior, or the v1.4.64 Origin-aware repair/version-sync systems.

## Validation
- Verified the new compatibility mixins against the method shapes used by Apoli 2.12.0-pre.2 and the supplied 2.12.0-pre.3 source.
- The new Java compatibility classes pass a standalone syntax compile against minimal Minecraft/Mixin API stubs.
- Revalidated all JSON resources and mixin configuration after the performance changes.
- Static validation only in this environment; the Gradle wrapper still cannot download Gradle 8.8 here.

# v1.4.64

## Origin-aware attribute repair
- Rebuilt `/andromedaorigins repair <player>` so it no longer treats the player's current effective stats as trustworthy. The command now resets the raw vanilla bases for the attributes Andromeda actively owns, then re-applies the player's currently granted Apoli attribute powers.
- The final repaired values therefore match the **currently selected standard or Champion Origin** instead of leaving the player at vanilla-player stats. Example: a repaired Arachne receives the clean player base plus Arachne's max-health/scale/movement modifiers; a repaired Lichling receives the clean base plus Lichling's health modifier.
- The rebuild uses Minecraft's PLAYER default-attribute container rather than a hand-maintained per-Origin stat table, so future balance changes in the Origin power files remain authoritative.
- The repair does **not** re-select the Origin and therefore does not intentionally reset selection callbacks, player-facing cooldowns, charge resources, or active timers.
- Added `/andromedaorigins repair_attributes <player>` for attribute-only recovery/debugging. The normal `repair` command still performs the existing crowd-control, Undetectable-marker, Incapacitated, and health recovery after the attribute rebuild.
- Equipment/status modifiers are not blanket-wiped. Apoli-owned attribute modifiers are removed/re-applied through Apoli's own runtime modifier methods, while unrelated non-Apoli modifiers remain in place.

## Exact client/server version sync
- Added a mandatory Fabric login-query handshake on `andromeda_origins:version_check`.
- A client without Andromeda Origins is disconnected before joining and told which server version is required.
- A client with a different Andromeda Origins version is disconnected before joining with the server/client versions shown.
- Version comparison uses each side's runtime Fabric Loader mod metadata, so the check follows the built jar's real `mod_version` instead of a duplicated hard-coded string.

## Validation
- Updated the packaged mod version to **1.4.64** and refreshed current-version documentation.
- Revalidated all JSON resources; gameplay data/Enhanced FX definitions are otherwise unchanged from v1.4.63.
- Static/source validation only in this environment; the Gradle wrapper still cannot fetch Gradle 8.8 here.

# v1.4.63

## Champion Wyverian — draconic primary
- Champion Wyverian now uses a dedicated Primary ability instead of sharing standard Ember Flames. Standard Wyverian is unchanged.
- **Dragon's Breath** replaces Ember Ray for Champion Wyverian: the 32-block stream uses dragon-breath/arcane presentation and deals magic damage instead of fire damage or ignition.
- **Dragon Charge** replaces Ember Pyroclast: the 2-second charge, 64-block maximum range, 4-block impact radius, 10-resource cost, and interruption behavior are retained, but the impact deals **10 magic damage** with draconic/arcane effects instead of the standard fire/explosion presentation.
- Added separate Champion-only charge helpers and ray functions so the standard Wyverian fire implementation is not altered. Caster tagging prevents the new magic breath/charge AoE from striking the Champion that fired it.
- Added six Champion Wyverian Enhanced FX definitions for Dragon's Breath, four escalating Dragon Charge stages, and the Dragon Charge impact.

## Validation
- Revalidated all JSON resources and Champion Wyverian resource references after splitting the primary power.
- Enhanced FX definitions now total **109** events.
- Static validation only in this environment; the Gradle wrapper still cannot download Gradle 8.8 here.

# v1.4.62

## Champion Manticore — Bloodrift tuning
- Reduced Bloodrift's fixed cooldown from **30 seconds to 10 seconds** and changed its HUD style to use the same resource-bar style as Beast of Blood.
- Added **cosmetic lightning** on both Bloodrift entry and reappearance; the bolt is visual/audio presentation only and does not deal lightning damage or ignite blocks.
- Repositioned Bloodrift's claw-tear FX **in front of the player** instead of centered on the player model so the rupture reads as a forward slicing motion.
- Reappearance now plays a dedicated Manticore roar shortly after visibility returns, followed by the existing delayed aftershock.
- Bloodrift still ends on manual toggle, attack, or incoming damage, and still applies the existing 3-second Darkness burst within 6 blocks when it ends.

## Validation
- Revalidated all modified JSON resources.
- Kept Bloodrift outside the generic Champion 2× cooldown helper; its 10-second cooldown is the actual fixed cooldown.
- Static validation only in this environment; Gradle 8.8 is still unavailable locally.

# v1.4.61

## Champion Manticore — Bloodrift
- Added **Bloodrift** as a Champion Manticore-only secondary variant on **Sneak + Secondary Active**. While sneaking, the Champion tears into reality, becomes **Undetectable**, and can press the same input again to rupture back out.
- Bloodrift ends immediately if the Champion attacks or takes damage. When it ends, nearby entities within **6 blocks** receive **Darkness for 3 seconds**, and the cooldown begins.
- Bloodrift uses its own dedicated **30-second cooldown** and does **not** consume Beast of Blood or alter Beast of Blood's cadence. Beast of Blood remains on normal Secondary while not sneaking.

## Cinematic FX pass
- Expanded the presentation of several existing Enhanced FX events for readability and impact without changing gameplay values.
- **Manticore:** added a dedicated four-stage Bloodrift presentation (`enter`, `tear`, `exit`, `aftershock`) and slightly strengthened Beast of Blood / Lunge impact layering.
- **Fenrkin / Humanity / Lichling / Siren / Veilborn / Wyverian:** strengthened selected signature events with extra rings, helixes, impact layers, and follow-through bursts for a more dramatic presentation.

## Validation
- Revalidated all modified JSON resources after the Champion Manticore and Enhanced FX updates.
- Champion parity remains otherwise unchanged from the prior audit: outside of established Champion exceptions and the new Champion Manticore Bloodrift, shared origin abilities remain 1:1.
- Enhanced FX definitions now total **103** events.
- Static validation only in this environment; the Gradle wrapper still cannot download Gradle 8.8 here.

# v1.4.60

## Nereid / Selkie hydration integration
- Fixed Convalescing Aura creating a second Nereid Wet timer/HUD bar on standard Selkies instead of feeding the Selkie's native hydration system.
- A hydrated standard Selkie now gains **3 seconds of native stored wetness per Aura pulse**, matching the Aura's normal 3-seconds-of-Wet-per-second behavior.
- A fully dried-out standard Selkie now gains **1 second per Aura pulse** toward its existing 30-second dry-state recovery meter rather than being instantly cured.
- Marked aquatic targets can now receive both the Aura heal and aquatic Wet/hydration processing in the same pulse; the old `if_else_list` layout stopped after the first matching branch.
- Any stale generic Nereid Wet timer already present on a standard Selkie is cleanly expired when the Aura reaches them, and the generic Nereid Wet HUD is suppressed on standard Selkies so only their native hydration bar is shown. Champion Selkie keeps generic Wet handling because it intentionally has no standard hydration weakness/bar.

## Arachne Eightfold Swiftness
- Reduced the airborne silk-latch raycast from **32 blocks to 16 blocks**.
- Reduced the airborne pull velocity so a successful latch no longer carries Arachne far beyond the intended 16-block movement scale.
- Removed the forced downward velocity from the ground dash (`y -2` -> `y 0`).
- Arachne step assistance is now automatically active during the ground dash for both standard and Champion Arachne, while the normal Origin Toggle behavior remains unchanged.

## Siren Infatuation Tempo
- Reduced Infatuation duration from **15 seconds to 10 seconds** for standard and Champion Siren.
- Updated the target timer, actionbar message, and player-facing ability description together.

## Veilborn Auroral Mirage
- Removed the repeating attached Enhanced FX pulse that followed Veilborn while Auroral Mirage was active.
- Moved all one-shot activation particle bursts (including Champion Mirage's Darkness presentation) before Undetectable is granted. Once the Veilborn becomes invisible, Auroral Mirage itself emits no tracking particles; exit/break FX still play when stealth ends.

## Incapacitated diagnosis
- Confirmed the reported large-hit instant death is an **upstream Incapacitated configuration mechanic**, not an Andromeda Origins damage/downing rule. Incapacitated 2.0.x exposes `ShouldDieOnOverkillDamage` / `shouldDieOnOverkillDamage`, enabled by default; when enabled, a lethal hit whose recorded damage is greater than **max health + current health** hard-kills instead of downing.
- Andromeda Origins does not override that setting. Public Incapacitated documentation does not specify whether the compared damage value is captured before or after armor/enchantment mitigation, so this release does not claim or alter that detail.

## Validation
- Revalidated all JSON resources after the gameplay/HUD changes.
- Re-audited the new standard/Champion shared paths for Arachne, Nereid, Siren, and Veilborn so Champion parity remains intact aside from established Champion exceptions.
- Enhanced FX definitions now total **99** after removing the persistent Auroral Mirage pulse.
- Static validation only in this environment; the Gradle wrapper still cannot download Gradle 8.8 here.

# v1.4.59

## Fenrkin Adrenaline selection reset
- Fixed both normal and Champion Fenrkin selection initialization after the v1.4.57 cooldown increase. Adrenaline now resets to the full **7200-tick / 6-minute** base cooldown when the Origin is selected instead of being initialized at the old 3600-tick value.
- Champion Fenrkin still recovers that cooldown at the normal Champion 2× rate, so the effective wait remains **3 minutes**.

## Champion parity audit
- Re-audited all 13 Champion Origins against their standard counterparts. Shared active powers remain shared 1:1; Champion-only passive copies differ only where a standard mechanic is a racial weakness/self-debuff or where an explicit Champion exception already exists.
- Fixed a real parity regression in `common/dash_projectile_penalty`: Champion movement abilities now retain the same **50% projectile-damage reduction during the movement ability and its 3-second tail** as the corresponding standard Origin. This affects Arachne, Fenrkin, Manticore, Satyr, Selkie, and Wyverian movement abilities that use the shared helper.
- Restored the missing Enhanced FX parity for **Champion Fenrkin Adrenaline** and **Champion Satyr Swift Leap**. These cosmetic hooks had been added to the standard duplicated passive files but not their Champion copies during the v1.4.58 FX expansion.
- Retained the established intentional Champion exceptions: removed weaknesses/self-debuffs, 2× player-facing cooldown recovery, Champion Humanity Mortal Triumph, and Champion Veilborn's dedicated Mirage/Darkness behavior and fixed 5-second Mirage cooldown.

## Validation
- Verified every Champion origin file contains exactly one Champion marker and one Champion cooldown-recovery power.
- Verified standard/Champion active ability files are shared wherever no explicit Champion override is required.
- Verified duplicated Champion passive strengths match their standard counterparts after excluding documented weakness/self-debuff removals and explicit Champion exceptions.
- Revalidated all JSON resources after the parity fixes.

# v1.4.58

## Major Enhanced FX expansion
- Expanded the optional Spell Engine / More RPG Library presentation layer from one-shot cast bursts into more persistent, staged, target-side, and state-aware visuals.
- **Nereid:** Convalescing Aura now pulses while channeling, shows healing FX on healed targets, has a larger release burst, and Submersion hits produce target-side water effects.
- **Selkie:** Coastal Phalanx now has matching caster/target water-shield effects and refresh pulses; Surging Tides leaves short trails; Sealskin Bastion pulses while active.
- **Veilborn:** added dedicated purple/magenta FX for target-side Veil Transposition, Unstable, Reality Shatter, active Auroral Mirage, and Champion Mirage Darkness bursts.
- **Lichling:** Chimes of Necros now has ten escalating visual pulse stages as Energy is consumed; every Doom application marks the target and the 10-stack detonation has its own large necromantic burst.
- **Wyverian:** Ember Pyroclast now has four visible charging stages, a dedicated impact explosion, and hover produces a wind-vortex effect.
- **Humanity:** Indomitable's prevented-death trigger now has a unique holy/shield burst. Mortal Resolve gains three escalating late-stage FX cues plus an enhanced expiry effect.
- **Faerie:** Trickster prank families now have item-specific enhanced particle signatures without changing any gameplay effects.
- **Fenrkin / Manticore / Satyr / Siren / Gorgon:** added On the Hunt and Beast of Blood pulses, movement trails/bounce cues, staggered Wail waves and target Infatuation FX, plus stronger repeated Gaze and target-side Transference FX.
- Renamed the internal Enhanced FX status wording from `recipes` to `definitions`; these files are FX definitions, not Minecraft crafting recipes.

## Safety / behavior
- This pass is cosmetic-only: no cooldowns, damage, status durations, resource costs, or Origin balance values were changed from v1.4.57.
- Repeating cosmetic hooks are intentionally low-frequency (generally once per second or a few short delayed pulses) to avoid per-tick particle spam.
- Base Andromeda particles and custom sounds remain the fallback when optional FX libraries are unavailable or disabled.

# v1.4.57

## Fenrkin Adrenaline
- Increased Adrenaline's base cooldown from **3 minutes to 6 minutes** for both normal and Champion Fenrkin.
- Champion cooldown recovery still applies at 2× rate, making Champion Adrenaline effectively **3 minutes**.
- Recovery amount and the existing Crowd Control break, brief Unstoppable state, and 35-second resistance buff are unchanged.

## Lichling — Chimes of Necros
- Increased Chimes of Necros range from **10 blocks to 15 blocks**.
- Each one-second Energy consumption now applies **Wither I for 2 seconds** on the first pulse; the duration increases by 1 second on each subsequent Energy consumption, capping at **10 seconds**.
- Chimes continues to add **1 Doom stack per pulse** to visible non-Undead targets.
- The 10th Doom stack still deals **20 magic damage**, applies **Wither II for 5 seconds**, and clears all Doom stacks.
- The existing Chimes visual sphere was scaled from 10 to 15 blocks without increasing its particle-command count.
- The Lichling screech opener and enhanced Chimes FX remain unchanged.

## Cleanup / validation
- Removed the obsolete `V2_0_CROSS_REFERENCE.md` file from the packaged source.
- Revalidated JSON syntax and the modified cooldown / Chimes resource flow.

# v1.4.56

## Veilborn Wet HUD / internal HUD fix
- Fixed Veilborn Wet/Unstable duration accumulation. While Veilborn is Wet, physically in water, or exposed to rain, `veilborn/helper/watered_remove` now gains **3 seconds every second** up to the existing 90-second cap instead of only receiving one +3-second increment when the state first started.
- The Wet Status bar now therefore fills upward while exposure continues and counts down after exposure ends. The resource starts at 0 so the first second of exposure represents exactly 3 seconds.
- Hid the shared `common/silenced_sources` internal source counter. It was the stray default HUD bar (the gunpowder-looking icon/bar) shown while Veilborn Wet granted Silenced; the counter remains fully functional but is no longer player-facing.

## v2.0.0 design-document cross-reference
- Cross-referenced the current runtime against the supplied **Andromeda Origins v2.0.0** design document across all 13 standard Origins.
- Corrected Chimes of Necros' normal pulse from **Wither II** to the documented **Wither I**. The 10th Doom-stack payoff remains 20 magic damage + Wither II for 5 seconds.

## Validation
- Revalidated all JSON resources after the changes.
- Audited all rendered HUD resources: no visible HUD entry now falls back to an unspecified/default sprite, and all custom resource-bar indices remain within their sheet ranges.
- No Java changes were required for this pass.

# v1.4.55

## Veilborn Enhanced FX color correction
- Fixed the Enhanced FX hex-color bridge to pack colors in Spell Engine's expected **RGBA** order instead of incorrectly treating six-digit colors like ARGB. This was why Veilborn's authored purple/violet tints were rendering as the wrong hue in game.
- Retuned Veilborn Enhanced FX around a saturated arcane palette using Spell Engine's own arcane magenta (`#FF66FF`) plus violet/purple accents, and added an extra `magic_spell` layer to Curtain Step, Veil Transposition, and Auroral Mirage enter/exit.
- The RGBA packing correction also makes the authored tint colors on other Origins render as originally specified.

## Lichling Chimes of Necros
- Added the supplied `lichlingscreech.ogg` as a registered bundled sound: `andromeda_origins:ability.lichling.screech`.
- Chimes of Necros now plays the screech immediately when the channel starts, with a local caster copy plus a positional copy for nearby players. The existing Chimes audio bed and Enhanced FX remain unchanged.

## Validation
- Revalidated JSON resources after the sound registration and Veilborn FX edits.
- Confirmed the bundled OGG is stereo, 44.1 kHz, approximately 7.06 seconds long, and is referenced by `sounds.json`.
- Static only in this environment; please run the normal local/server Gradle build before deployment.

# v1.4.54

## Enhanced FX flashy pass II
- Expanded the optional Enhanced FX layer again with a much flashier presentation pass focused on the previously subtler Origins.
- Added new enhanced events for **Faerie Flutter**, **Fenrkin Adrenaline**, and **Lichling Chimes pulse** so those abilities now produce more readable and expressive audiovisual feedback instead of only relying on their original vanilla/custom presentation.
- Reworked the Faerie, Fenrkin, Gorgon, Lichling, Siren, and Wyverian Enhanced FX definition files with larger bursts, more layered particles, brighter glow/color treatment, stronger cone/travel usage, extra sound layering, and more persistent-looking channel visuals.
- The new pass intentionally remains cosmetic-only and optional. Gameplay logic, cooldowns, and damage values are unchanged.

## Validation
- Revalidated all Enhanced FX JSON definitions after the flashy pass.
- Confirmed the newly referenced internal FX events are wired into the relevant Origin powers.
- Static only in this environment; please run your normal local/server Gradle build before deployment.

# v1.4.53

## Enhanced FX flash pass
- Expanded the optional Spell Engine / More RPG Library presentation layer from a restrained prototype into a more visibly dramatic pass across the Origin roster. Particle counts, scale, glow, layering, and motion were increased for most events while still remaining cosmetic-only.
- Enhanced FX definitions now support richer data fields including color tinting, glow, opacity envelopes, scale variance/growth, playback speed, lifetime variance, motion presets, gravity/drag overrides, collision, attachment, `travel` and `helix` presets, and attached-to-ground decals.
- Added a new internal `/andromedaorigins internal_fx_at <event> <pos>` hook so effects can be spawned at a world position instead of only on the caster.
- Arachne received the largest pass: Weaver's Nest now erupts at the raycast impact point through a dedicated function hook, Webspinner now pulses while weaving and bursts more strongly on completion, and Eightfold Swiftness now has dedicated enhanced FX for both the grounded dash and the airborne grapple.
- Rebalanced the recipe coverage count upward with the new Arachne-specific events and updated the datapack JSON definitions accordingly.

## Validation
- Revalidated JSON syntax after the recipe rewrite and new Arachne helper function.
- Cross-checked the newly used Spell Engine / More RPG Library particle and sound identifiers against the uploaded library sources.
- Static only in this environment; please run a normal local/server Gradle build before deployment.

# v1.4.52

## Optional Spell Engine / More RPG Library enhanced FX
- Added an optional enhanced audiovisual layer for Origin abilities. Spell Engine and More RPG Library are listed as `suggests`, not hard dependencies; the base mod still loads and behaves normally without either library.
- The Spell Engine particle bridge is reflection-only, so no Spell Engine classes are linked from Andromeda's normal startup path. If the external API is absent or changes, the compatibility layer fails closed and the existing Andromeda/vanilla effects continue to work.
- Added **30 datapack-defined FX events** across all 13 Origins, including web/illusion/hunt/stone/holy/soul/blood/water/wind/music/veil/fire themes. Existing custom Andromeda sounds remain the primary identity layer; library sounds are mixed underneath at restrained volume.
- Added `data/andromeda_origins/andromeda_fx/*.json` recipes so particle/sound selection is data-driven instead of being hardcoded per Origin in Java.
- Added a persistent `config/andromeda_origins_fx.json` with independent `enabled`, `particles`, and `sounds` switches.
- Added operator test controls: `/andromedaorigins enhanced_fx`, plus `enabled`, `particles`, and `sounds` boolean subcommands.
- Added an internal `andromedaorigins internal_fx <event>` hook used by the Apoli powers; it only invokes cosmetic compatibility and does not change gameplay.
- Mortal Resolve invokes its enhanced activation directly from the existing Java presentation manager; all other recipes are attached at their existing Apoli cast/impact points.

## FX coverage in this prototype
- Arachne: Weaver's Nest and Webspinner completion.
- Faerie: Fae Illusions.
- Fenrkin: On the Hunt and Mark of Fenrir.
- Gorgon: Ophidian Gaze and Transference.
- Humanity: Indomitable and Mortal Resolve.
- Lichling: Death's Defiance plus Chimes of Necros start/end.
- Manticore: Ravenous Lunge start/impact and Beast of Blood.
- Nereid: Convalescing Aura start and Submersion.
- Satyr: Rush and Satyr's Landing.
- Selkie: Surging Tides and Sealskin Bastion.
- Siren: Infatuation Tempo and Shrieking Wail cast/impact.
- Veilborn: Curtain Step, Veil Transposition, and Auroral Mirage enter/exit.
- Wyverian: Ember Flames opening cue and Gusts of Freedom pulse.

## Validation
- Parsed all JSON resources after the FX additions.
- Confirmed every `internal_fx` event used by powers has a matching datapack recipe and every recipe is referenced.
- Cross-checked the external sound/particle identifiers against the uploaded Spell Engine 1.10.5 and More RPG Library 2.7.2 sources.

# v1.4.51

## Champion Veilborn
- Champion Veilborn Auroral Mirage now uses a fixed **5-second cooldown** after the Mirage ends, whether it is dismissed manually or broken by attacking.
- Standard Veilborn remains unchanged at the normal **2-minute** Auroral Mirage cooldown.
- Auroral Mirage is excluded from the generic Champion 2× cooldown accelerator so its Champion cooldown remains exactly 100 ticks / 5 seconds rather than being shortened again.
- The existing 8-block, 5-second Darkness burst on Champion Auroral Mirage entry and exit is unchanged.
- Updated Champion-facing descriptions and current-release documentation to disclose the cooldown exception.

## Validation
- Revalidated JSON syntax and confirmed both Auroral Mirage exit paths trigger the base cooldown and then reduce the Champion cooldown to 100 remaining ticks / 5 seconds.
- Confirmed the generic Champion cooldown accelerator no longer modifies `veilborn/secondary_cooldown`.

# v1.4.50

## Champion cooldowns
- Champion variants now recover player-facing ability cooldowns at **2× the normal rate**, producing a 50% shorter effective cooldown.
- The reduction is implemented as a shared Champion cooldown-recovery power so Champions continue using the same normal active-ability JSON instead of maintaining divergent copies.
- Charge times, resource costs, channel cadence, input-debounce cooldowns, and other internal timing gates are intentionally unchanged.
- Named passive cooldowns that function as real gameplay cooldowns are included, including Fenrkin Adrenaline, Arachne's periodic web-on-hit proc, Faerie concealment recovery, and Satyr's Landing reset gate.
- Champion Origin and passive descriptions now disclose the 2× cooldown recovery rule. Shared descriptions with explicit numbers label those values as **base cooldowns**.

## Arachne Webspinner
- Fixed the Webspinner progress resource remaining at its maximum after a successful weave. The progress now resets to 0 after awarding 5 cobwebs, allowing both normal and Champion Arachne to complete Webspinner repeatedly.
- Champion Arachne still skips Cobweb Fatigue as intended.

## Fenrkin Adrenaline
- Reworked Adrenaline's recovery so both normal and Champion Fenrkin are set to **exactly 8 health** one tick after lethal damage is prevented.
- Apoli's `prevent_death` first places the holder at 1 health; the previous additive heal could be overwritten by later death/downed-state hooks. The delayed internal exact-health command removes that race while preserving the documented 8-health result.
- Normal Adrenaline retains its 3-minute base cooldown; Champion cooldown recovery reduces the Champion effective cooldown to 90 seconds.

## Champion Veilborn
- Champion Auroral Mirage now emits an **8-block spherical Darkness burst** both when entering and when leaving the state.
- Nearby entities receive Darkness for **5 seconds**; the Champion themselves is excluded.
- The exit burst runs for both manual dismissal and attack-triggered uncloaking.
- Standard Veilborn Auroral Mirage is unchanged.

## Validation
- Revalidated Champion origin power grants, new cooldown-resource references, JSON syntax, and modified descriptions after the v1.4.50 changes.

# v1.4.49

## Design decisions resolved against v1.3.6
- Step assistance for Arachne and Fenrkin remains tied to the Origin Toggle behavior in the current implementation; the older document will be revised rather than moving step assistance back into their actives.
- Faerie does not have passive Levitation immunity. Its mid-air jump clears Levitation and Slow Falling when the jump is used; normal and Champion descriptions now state this directly.
- Expanded Trickster's in-game description to list the mechanical effect currently implemented for every dye/item prank instead of treating the newer effects as undocumented drift.
- Fenrkin Adrenaline now resolves to exactly 8 health after preventing lethal damage. The redundant Instant Health II application was removed; normal and Champion Fenrkin use the same recovery amount.
- Veilborn Wet/Silenced duration now caps at 90 seconds (1.5 minutes) instead of 180 seconds. The hidden `weaknesstate` boolean comparison was also normalized from a stale `< 120` check to `< 1` without changing its role as an internal state flag.
- Lichling's +8 Max HP, current Gorgon resistance exceptions, current Siren redesign, current Wyverian stats, and Selkie rain interaction remain authoritative in code; the v1.3.6 document is expected to be revised for those items.

## Champion parity fixes
- Fixed `fenrkin/helper/stalked` so both normal and Champion Fenrkin can see the Mark of Fenrir target glow and receive the intended +25% damage interaction.
- Audited `fenrkin/helper/onhunt`: Champion Fenrkin already uses the exact same On the Hunt helper, overlay, buffs, and 64-block entity glow as normal Fenrkin. No Champion-only divergence exists in that helper; the confirmed origin-gating defect was in Mark of Fenrir.
- Fixed Fae Illusions target glow so Champion Faerie is recognized as a valid viewer.
- Fixed Nereid's Mark ally glow so Champion Nereid is recognized as a valid viewer.
- Fixed Weaver's Nest filtering so Champion Arachne receives the same Arachne self-exemption as normal Arachne instead of being pulled/webbed by its own shared ability.

## Runtime corrections confirmed by both documentation and UI
- Restored Arachne Cobweb Fatigue to the documented/current-tooltip values: +25% hunger drain, -25% movement speed, and -25% mining speed. The implementation had drifted to 20% in all three modifiers.
- Restored Gorgon Ophidian Gaze and Transference targeting to 32 blocks. Both the supplied v1.3.6 document and current player-facing descriptions specify 32 blocks, while the runtime raycasts had drifted to 64.

## Description and documentation accuracy
- Removed Fae Illusions' bare natural-expiry `origins:clear_effect`; expiration now only removes the illusion helper itself, while Brown Dye remains the intentional full status-effect purge prank.
- Re-audited every player-facing Origin/power description and tooltip against the v1.4.49 runtime data after the design decisions were resolved.
- Corrected Nereid hydration text/tooltips to state that water, rain, or the shared Wet state grants the 25% resistance, while the dry state carries the land penalties.
- Corrected Veilborn's passive text to include Wet/rain exposure and the resolved 90-second cap.
- Clarified Champion Arachne, Manticore, Satyr, Veilborn, and Wyverian passive descriptions where shared active descriptions still mention drawbacks that Champions intentionally suppress.
- Replaced the stale Fenrkin passive text that had accidentally described Manticore mechanics. Fenrkin now documents its actual movement bonus, global damage resistance, Wet/cramped penalty, armor threshold, carnivore diet, and Adrenaline behavior.
- Removed the obsolete Hunter's Caution badge from On the Hunt; the associated power was not granted anywhere.
- Corrected Nereid Convalescing Aura from 10 blocks to the implemented 20-block radius.
- Corrected Selkie Sealskin Bastion from 99% to the implemented 75% incoming-damage reduction.
- Corrected Manticore venom text to state that Poison I is applied by melee attacks while falling.
- Restored Veilborn Curtain Step wording to the implemented two-stage Unstable -> Reality Shatter flow.
- Expanded Arachne, Faerie, Satyr, Siren, Lichling, and Wyverian passive descriptions to expose mechanics already present in code.
- Removed Faerie's unsupported Levitation-immunity tooltip claim; the current code clears Levitation when performing an air jump but does not provide continuous Levitation immunity.
- Corrected Gorgon/Wyverian resistance tooltips to include the damage types excluded by their current damage-condition filters.

## Internal cleanup
- Removed dead `common/hunter_caution` and legacy `humanity/helper/doom` powers after confirming they have no live references.
- Removed `UndetectableHeldItemFeatureRendererMixin`. Apoli already suppresses third-person `held_item` feature rendering through `origins:prevent_feature_render`; Andromeda's first-person held-item mixin remains because it covers a separate renderer path.
- Kept the Undetectable AI-target mixins and Armor Model API/Figura bridge; the uploaded Origins/Apoli, Figura 0.1.6, Armor Model API, and Rogues sources confirm those still cover behavior not replaced by the removed mixin.

## Audit scope
- Compared v1.4.48 runtime data/code and player-facing descriptions against the supplied v1.3.6 design document. Mechanics with unresolved design intent were documented for decision rather than silently rebalanced.
- Gradle compilation still requires the Gradle 8.8 distribution; the audit environment cannot download it, so this release is statically validated here but should receive a normal local/server build before deployment.

# v1.4.48

## Veilborn HUD cleanup
- Hid the internal `veilborn/passives_weaknesstate` boolean HUD resource. It is state bookkeeping, not a duration timer, and was causing a second leftover bar whenever Veilborn became wet.
- Veilborn Wet / Unstable duration is now represented only by `veilborn/helper/watered_remove` using the intended **Wet Status Bar (03)** on sheet 1 at `bar_index: 2`.
- Removed the obsolete Veilborn assignment from the Weakness Bar (18) documentation.

## Selection descriptions
- Removed all Champion comparisons/references from player-facing descriptions and badges used by the 13 standard selectable Origins.
- Standard descriptions now state only the behavior that actually applies to the selected Origin. Champion mechanics, internal conditions, command-only origin files, and administrative documentation are unchanged.
- Also removed Champion wording from Humanity's hidden Figura compatibility strings for consistency.

## Documentation
- Updated all current-release Markdown headers/notes to v1.4.48 without rewriting historical changelog entries.
- The authoritative HUD sheet mapping remains 25 styles on sheet 1 and 6 on sheet 2.

# v1.4.47

## Manticore Beast of Blood
- Replaced the blanket `origins:clear_effect` activation cleanse with a Java harmful-effect filter.
- Beast of Blood now removes only status effects whose Minecraft category is `HARMFUL`, including modded harmful effects that use the normal category system.
- Beneficial and neutral effects such as Resistance, Strength, Absorption, Regeneration, Night Vision, and custom non-harmful effects are preserved.
- Existing Crowd Control cleanup remains intact through the `minecraft:ccontrol` power source, and the 30-second Unstoppable state is unchanged.

## Incapacitated reliability
- Fixed a real Mortal Resolve compatibility leak: its temporary `downsUntilDeath = -1` final-death sentinel is now tracked and restored to Incapacitated's configured post-death counter after respawn.
- Added a join/respawn self-repair for legacy negative counters when Incapacitated has `UnlimitedDowns=true`. This fixes players already affected by the pre-v1.4.47 stale-state bug.
- Reset of Incapacitated transient damage tracking is retained after the repair.
- Andromeda does **not** override Incapacitated's own instant-kill, overkill, timeout, give-up, or full-party-death configuration; those remain controlled by Incapacitated.

## Documentation
- Updated all current-release Markdown documentation to v1.4.47 while preserving the historical changelog and v1.4.46 HUD mapping.

# v1.4.46

## HUD resource-bar mapping correction

- Corrected the 31-style custom HUD catalog to **25 styles on `resource_bars_1.png` and 6 styles on `resource_bars_2.png`**.
- Moved **Wyverian Gusts of Freedom / Wings Bar (25)** from sheet 2 index 0 to **sheet 1 `bar_index: 24`**.
- Reindexed sheet 2 so it now begins at zero with the final six styles: Halo 0, Bubble 1, Heart 2, Gold Seal 3, Infatuation 4, Waves 5.
- Updated every affected Nereid, Selkie, Siren, and Wyverian HUD reference to the corrected sheet/index pair.
- Updated README and all current-release Markdown metadata; gameplay, balance, sounds, Figura hooks, and compatibility behavior are unchanged from v1.4.45.

# v1.4.45

## Veilborn balance
- Increased standard Veilborn maximum health from 16 HP / 8 hearts to **18 HP / 9 hearts** (`-4 Max HP` -> `-2 Max HP`).
- Reduced each standard Veilborn armor-weight movement/swim penalty from **-15%** to **-10%**. Thresholds remain unchanged (13+ Armor, 16+ Armor, 9+ Armor Toughness).
- Reduced the Wet/Unstable damage-dealt penalty from **-80%** to **-60%** for melee and projectile damage. Wet Silence and lingering-duration behavior are unchanged.
- Reduced the maximum self-stacked Reality Shatter duration from **120 seconds** to **90 seconds**. Initial and repeated Curtain Step increments remain unchanged.
- Champion Veilborn remains free of the standard health, armor-weight, water, shield, and Curtain Step self-Reality-Shatter drawbacks.

## Undetectable stealth / rendering
- Upgraded `andromeda_origins:common/undetectable` into a true mob-target stealth state shared by every Andromeda power that grants Undetectable.
- Added mob `TargetPredicate`, Brain `Sensor`, and direct `MobEntity` target guards so mobs cannot acquire or retain an Undetectable player as an AI target, including visibility-ignoring target checks.
- Added a synchronized `andromeda_undetectable` command tag managed by the shared power for vanilla Java/client compatibility.
- The `/andromedaorigins repair <player>` recovery command now clears a stale Undetectable marker; an actually active stealth power reasserts it on the next sync.
- Added `origins:prevent_feature_render` for `held_item` plus first-person and vanilla third-person held-item render guards. Armor and glowing-outline hiding remain in place.
- Extended the Armor Model API bridge so registered custom geo armor is also suppressed while the wearer is Undetectable, preventing floating custom armor around an invisible player.
- Updated Faerie and Veilborn Undetectable tooltips/descriptions to describe the stronger stealth behavior.
- This does not grant damage invulnerability and does not cancel arbitrary custom renderer geometry or already-created projectiles/area effects.

## Documentation
- Updated README, compatibility, Figura, Champion, custom-audio, and resource-bar documentation for v1.4.45.

# v1.4.44

## Figura / Armor Model API compatibility
- Added a client-side compatibility bridge for Armor Model API custom geo armor. Figura avatars that hide `vanilla_model.ARMOR` or an individual armor slot now also hide Armor Model API-rendered armor instead of only vanilla armor geometry. The bridge is authored against Armor Model API 1.1.0 / Figura 0.1.6, with Rogues & Warriors 3.1.1 used as the 1.21.1 test case.
- The bridge is implemented inside Andromeda Origins; Armor Model API, Rogues & Warriors, and Figura do not need to be forked or modified.
- Compatibility is optional and soft-linked: Andromeda Origins still has no hard Figura or Armor Model API dependency. The Armor Model API mixin is pseudo/optional and the Figura visibility lookup uses cached reflection against the Figura 0.1.6-era API.
- Figura's `VANILLA_MODEL_EDIT` permission is respected before suppressing custom armor. If Figura is absent or its expected internals cannot be resolved, Armor Model API rendering falls back to normal behavior.
- Added Armor Model API to optional `suggests` metadata and documented the custom-armor behavior in `README.md`, `COMPATIBILITY.md`, and `FIGURA_COMPAT.md`.
- No Origin ability, balance, audio, HUD, semantic Figura-state, legacy Figura-hook, Champion, or server-side gameplay behavior changed from v1.4.43.

# v1.4.43

## Documentation and metadata cleanup
- Removed the personal-name command example from the README and replaced it with the generic `<player>` placeholder.
- Rebuilt README, compatibility, Champion, sound, and Figura documentation around the actual current v1.4.42 behavior.
- Added `RESOURCE_BARS.md` documenting the two custom HUD sheets and all 31 global style mappings.
- Corrected the README build output/version information and documented the current Origins/Apoli development targets.
- Refreshed `fabric.mod.json` description text and added optional `figuraextrabone` suggestion metadata alongside Figura/Incapacitated.
- Removed the unused legacy `origin_icon/gorgon.png` asset after confirming the selection-screen item model exclusively uses `gorgon_eye_2.png`.
- Repaired changelog headings for v1.4.28 through v1.4.42 that had been corrupted by repeated global version-string replacements.
- Updated the custom-audio documentation to reflect the current Lichling end-of-channel Final Bell behavior and Siren impact mix.
- No gameplay, ability, balance, sound, HUD mapping, Figura-state, or compatibility runtime behavior changed.

# v1.4.42

- Forced the Gorgon selection icon to use a new texture path (`item/origin_icon/gorgon_eye_2`) so the Origins selection-screen item model resolves the new 64x64 artwork instead of potentially reusing the old cached/overridden `gorgon` texture path.

# v1.4.41

- Fixed the Figura compatibility manager compile error by using the correct 1.21.1/Yarn `net.minecraft.server.command.CommandOutput` mapping.
- No gameplay or Figura-hook behavior changed from v1.4.40.

# v1.4.40

- Reworked Figura compatibility without adding a hard Figura dependency.
- Preserved `common/figura_1` through `figura_5` for existing avatars and normalized every legacy hook write to explicit boolean `set 1` / `set 0` behavior.
- Added automatic legacy-hook reset on origin selection/power addition and after respawn.
- Added a semantic Figura Lua helper and `FIGURA_COMPAT.md` so avatar authors can use named states instead of parsing Apoli NBT or memorizing numbered hooks.
- Mortal Resolve now exposes a dedicated synced semantic resource (`humanity/figura_mortal_resolve`), with the existing active tags retained as helper fallbacks.
- Added an Auroral Mirage duration hook on Veilborn `figura_2` and fixed stale/missing Gorgon, Humanity, Selkie and Veilborn Figura documentation.
- All 13 standard and 13 Champion origin definitions now include their matching origin-specific Figura compatibility documentation power plus the shared legacy backend.
- Removed two unreferenced legacy Figura helper files (`faerie/helper/unstoppable_anim` and `humanity/helper/selfrevive`).
- Added Figura to optional `suggests` metadata. Figura ExtraBone remains optional and is documented only as a PlayerAnimator/Emotecraft blend aid.

# v1.4.39

- Replaced `resource_bars_2.png` with the latest re-aligned sheet supplied by the user.
- Retained the v1.4.38 Gorgon icon update, Champion self-hit safeguards for damaging AoE abilities, louder Siren Wail impact, and end-of-channel Lichling Final Bell behavior.
- Revalidated all JSON resources and confirmed both HUD sheets are 256x256.

# v1.4.38

- Replaced the 64x64 Gorgon icon with the newly provided artwork.
- Audited Champion-origin combat interactions and added explicit self-exclusion (`distance > 0`) to damaging area-of-effect abilities reused by Champions: Satyr's Landing stomp, Manticore Ravenous Lunge impact, and Wyverian Ember Pyroclast explosion. This prevents Champions from damaging themselves with those abilities.
- Increased Siren Shrieking Wail impact volume again for better presence.
- Preserved the Lichling Final Bell Toll behavior so it always plays when Chimes of Necros ends.
- Revalidated JSON resources and champion HUD/selection files.

# v1.4.37

- Replaced `resource_bars_2.png` with the latest user-provided second sheet exactly as supplied to correct the persistent sheet-2 vertical alignment issue.
- Increased Siren Shrieking Wail impact sound volume from 0.8 to 1.15.
- Moved Lichling Final Bell Toll from first Doom application to the end of Chimes of Necros channeling so it always plays when the ability ends.

# v1.4.36

- Replaced `resource_bars_2.png` with the latest user-provided sheet and rebuilt it into exact Origins row slots.
- Top-aligned each visible bar within its 10-pixel slot to correct the appearance of bars looking shifted downward by a pixel.
- Preserved the existing sheet-2 mappings for global bars 25–31.

# v1.4.35

- Cleaned `resource_bars_2.png` to remove faint semi-transparent pink placeholder pixels that were still visible in-game as an underlay.
- Preserved the v1.4.34 row alignment and the existing 25–31 bar mappings.

# v1.4.34

- Rebuilt `resource_bars_2.png` from the user-provided sheet into exact Origins 10-pixel row slots to fix misshapen/clipped bars in-game.
- Preserved the same bar mappings from v1.4.33; only the texture row alignment was corrected.

# v1.4.33

- Replaced the preview HUD sheet with two final custom resource-bar sheets: `resource_bars_1.png` (global styles 01–24) and `resource_bars_2.png` (global styles 25–31).
- Remapped the Andromeda HUD resources to the user-provided 01–31 catalog. Sheet 1 uses zero-based indices 0–23; sheet 2 restarts at zero for global style 25.
- Champion mirror resources for Faerie, Fenrkin, Satyr, and Selkie use the same matching custom styles.
- Nereid Aura/Submersion Wet currently share one `nereidwet` duration resource, so that shared timer uses Wetness Bar 02; there is not a second independent Convalescing-only Wet timer to assign to Wet Status Bar 03 without splitting the mechanic.

# v1.4.32

- Replaced the preview HUD sprite sheet with the newly provided sheet.
- Renamed the custom HUD texture path to `andromeda_origins:textures/gui/andromeda_resource_bars_preview.png`.
- Preserved the existing bar mappings from v1.4.31.

# v1.4.31

- Replaced the preview HUD sprite sheet with the newly provided user sheet at `andromeda_origins:textures/gui/resource_bar_test.png`.
- Applied the requested mappings: bar 0 for Stamina Surge/Eightfold/Manticore Lunge/Satyr Momentum/Surging Tides; bar 1 for water-origin wetness meters; bar 2 for wet status; bar 3 for Weaver's Nest; bar 4 for Cobweb Fatigue; and bar 5 for Faerie Flutter.

# v1.4.30

- Fixed the test HUD bar mapping offset for `resource_bar_test.png` after in-game verification.
- Bars previously mapped to `bar_index: 1` now use `bar_index: 0`.
- Wet-status bars previously mapped to `bar_index: 2` now use `bar_index: 1`.

# v1.4.29

- Added a test custom HUD sprite sheet at `andromeda_origins:textures/gui/resource_bar_test.png` using the user-provided bar art.
- Remapped Fenrkin Stamina Surge, Arachne Eightfold Swiftness, Manticore Ravenous Lunge, Satyr Momentum, and Selkie Surging Tides to `bar_index: 1` on that custom sheet.
- Remapped Manticore wet weakness, Wyverian wet weakness, and Veilborn wet/unstable bars to `bar_index: 2` on that custom sheet for preview testing.

# v1.4.28

## Satyr — Swift Leap VFX + regression audit
- Replaced Swift Leap / double-jump's one-shot `minecraft:explosion` particle with `minecraft:gust_emitter_small` for both standard and Champion Satyr.
- Kept the existing cloud puff, Wind Charge sound, goat accent, launch velocity, jump charge usage, and all gameplay values unchanged.
- Re-audited all 13 Champion variants for command-only visibility, shared-resource routing, weakness/debuff suppression, helper references, and selection reset behavior.
- Corrected the stale build-output filename in the README.
- No other origin gameplay behavior was intentionally changed.

# v1.4.27

## Satyr — Satyr's Landing VFX
- Replaced the Shift/Stomp creature-impact `minecraft:explosion` particle with `minecraft:gust_emitter_large`.
- Added a Wind Charge-style landing burst using `minecraft:gust_emitter_large` plus a small ring of `minecraft:small_gust` particles when Satyr's Landing hits the ground.
- Kept the existing cloud/dust accent, damage, radius, knockback, bounce behavior, cooldowns, and all other gameplay values unchanged.
- Swift Leap's separate launch VFX was not changed.

# v1.4.26

## Ravenous Lunge trajectory refinement

- Reworked Manticore Ravenous Lunge into a grounded/airborne pitch-aware trajectory instead of one fixed horizontal vector.
- A downward block raycast distinguishes being within roughly 3 blocks of terrain from being genuinely airborne.
- Near the ground, upward aim is softly capped at the steep end: looking above ~50 degrees still lunges forward and upward instead of becoming a vertical rocket or refusing activation.
- Once sufficiently airborne, the lunge responds much more strongly to look pitch, including deliberate upward and downward lunges.
- The strongest airborne upward tier is capped so the lunge itself stays below the intended ~16-block vertical ceiling without relying on Jump Boost.
- Replaced the previous two sequential `set: true` velocity actions with one combined Y/Z velocity action per trajectory tier. This prevents the horizontal set from overwriting the intended vertical component.
- The one-second active-state cap, start/end roar behavior, collision handling, cooldown, damage, and Champion behavior are unchanged.

# v1.4.25

### Bug-fix pass
- Replaced Siren Shrieking Wail's 15 invisible armor-stand helper entities with a 15-ray fan. The ability now spawns zero helper entities; each cast also removes legacy `shriek` armor stands within 64 blocks.
- Removed Gorgon Grab/carry entirely from standard and Champion Gorgon, including its carry helpers and dismount-prevention mixin.
- Ravenous Lunge is now a horizontal forward lunge: `local_horizontal_normalized`, 2.5 forward velocity, 0.35 upward lift, and a 20-tick maximum active state. Looking upward/downward can no longer convert the forward launch into extreme vertical velocity.
- Ravenous Lunge audio now plays once at launch and once when the lunge ends. The repeating airborne roar loop was removed.
- Satyr ordinary landing feedback now uses the vanilla horse step sound instead of the custom Hoof Land recording.
- Siren/Champion Siren now have Infatuation Guard: damage from an attacker currently carrying the Infatuated power is fully invulnerable, including damage generated by an ability that began before Infatuation was applied.

# 1.4.24

## Fenrkin mark audio + command-only Champion Origins

### Fenrkin — Mark of Fenrir
- Added the supplied `markedfenriken.ogg` cue as `andromeda_origins:ability.fenrkin.marked`.
- The cue plays only after the five-second mark completes successfully.
- Both the Fenrkin and the newly marked target receive guaranteed local Master-channel playback.
- The asset is bundled as a mono 48 kHz positional-safe OGG.

### Champion Origins
- Added command-only Champion variants for all 13 Andromeda Origins.
- Every Champion origin is registered in the normal `origins:origin` layer with `unchoosable: true`, keeping it out of normal Origin selection while allowing staff assignment through `/origin set`.
- Champion variants retain their origin's abilities, identity-defining size/boons, cooldowns, charge times and resource costs.
- Racial weaknesses are removed: negative health/damage/movement stats, diet restrictions, iron sensitivity, environmental weakness states, equipment restrictions, food-efficiency penalties and similar passive drawbacks.
- Clear self-debuffs attached to abilities are also suppressed for Champions, including Cobweb Fatigue, Fenrkin stalking slowdown, Chimes slowdown, Famished, maximum-Momentum hunger drain, Sealskin Bastion slowdown, Curtain Step's self-applied Reality Shatter and dash projectile penalties.
- Champion Humanity uses **Mortal Triumph**: it preserves Mortal Resolve's revive, one-minute Resistance IV/Strength IV, activation effects and moving heartbeat, but does not force death or make deaths final when the minute expires.
- Target-facing counterplay and debuffs remain. For example, Gorgon's pumpkin protection still blocks Ophidian Gaze, and Champion abilities still apply their intended effects to enemies.

### Safety
- Added a hidden Champion marker power used only to condition shared powers without duplicating every active ability.
- Corrected Champion Satyr's Rush so its Momentum refill writes to the Champion passive resource rather than the ordinary Satyr resource path.
- Corrected Champion Satyr's landing helper so its cooldown trigger and Swift Leap reset also write to Champion-owned passive resources.
- Updated shared ability descriptions to clearly distinguish standard-origin drawbacks from Champion exemptions, and corrected Wyverian backend key tooltips.
- No ordinary origin weakness, ability balance or selection behavior was intentionally changed beyond the new Fenrkin mark audio and Champion-aware conditions.

# 1.4.23

## Audio mix reliability + roster-wide VFX polish

### Audio
- **Nereid — Convalescing Aura:** moved the long custom aura to a guaranteed caster-local Master copy plus a positional nearby-listener copy, and added explicit stop cleanup when channeling ends.
- **Siren — Infatuation Tempo:** the long custom charm now starts only after a successful target is acquired instead of playing on a miss. The caster receives a guaranteed local charm layer, the afflicted target receives a guaranteed local whisper layer, nearby players receive quieter positional copies, and the whisper is stopped if Infatuation is cleansed early. The simultaneous Illusioner accents are staggered and reduced so they no longer mask the custom recordings.
- **Satyr:** Rush / Swift Leap hoof layers are stronger, goat/wind accents are slightly reduced and staggered, and normal Hoof Land is more audible.
- **Veilborn:** custom ability cues remain the focal layer while chorus-fruit, Illusioner, amethyst, and enchantment-table sounds are delayed and softened into secondary accents.
- **Gorgon:** Ophidian Gaze hiss and Stone Form are stronger; stone/Illusioner layers are staggered and reduced to keep the custom cues readable.
- **Lichling — Death's Defiance:** restored sculk, totem, trial-spawner, and Illusioner layers are retained but staggered so each cue can be heard instead of beginning simultaneously.
- **Fenrkin — Stamina Surge:** wolf growls/panting are more audible while the very loud leather-equip accent is reduced and delayed.
- **Manticore — Beast of Blood:** the low Warden heartbeat was raised from 0.10 to 0.35 and paired with a light visual pulse.

### Visual effects
- Added or strengthened thematic activation/state particles across **all 13 origins** while keeping particle counts deliberately modest.
- **Arachne:** sharper silk/enchanted-hit snaps on movement and Weaver's Nest.
- **Faerie:** extra waxing-style fairy sparkles layered into Flutter and successful Fae Illusions.
- **Fenrkin:** sweep/smoke predatory accents on On the Hunt and Stamina Surge leaps.
- **Gorgon:** witch-smoke arcane/serpentine haze around gaze abilities.
- **Humanity:** Indomitable now has a restrained totem/end-rod shield flare; Mortal Resolve keeps its existing custom burst and afterglow.
- **Lichling:** stronger sculk-soul/reverse-portal necrotic channel accents.
- **Manticore:** extra damage/sweep launch accents and a subtle heartbeat pulse during Beast of Blood.
- **Nereid:** brighter glow/nautilus/splash aura feedback and a clearer channel-ending burst.
- **Satyr:** visible gust/crit movement accents and additional Vigil Perception sparkle.
- **Selkie:** stronger splash/glow water-crown feedback on Coastal Phalanx, Surging Tides, and Sealskin Bastion.
- **Siren:** stronger heart/glow Infatuation feedback and a glow echo around Shrieking Wail.
- **Veilborn:** additional dragon-breath veil distortion layered into transposition/mirage effects.
- **Wyverian:** extra ember sparks during Ember Flames and a visible gust accent during Gusts of Freedom.

### Safety / balance
- Cosmetic/audio pass only. After removing presentation-only sound/particle actions, the power data is structurally identical to v1.4.22.
- No intended changes to damage, cooldowns, movement, resource costs, durations, targeting, status effects, or ability conditions.

# 1.4.22

## Faerie vanilla-audio audibility correction

- Replaced the low-volume simultaneous Allay/Vex `origins:play_sound` layers with explicit, server-issued `playsound` commands.
- Flutter now plays its custom chime first, then a clearly audible Allay cue after 3 ticks and a high-pitched Vex ambience after 8 ticks.
- Successful Fae Illusions casts now use the same staggered mix: custom chime, Allay `item_given`, then Vex ambience.
- The Faerie receives a guaranteed local Master-channel copy; nearby players receive a quieter positional copy.
- No cooldown, resource, targeting, movement, status-effect, or damage behavior was changed.

# 1.4.21

### Lichling channel reliability and restored character audio
- Restored the complete vanilla sound design for **Lichling Death's Defiance** from the pre-v1.4.20 implementation: sculk-shrieker, totem, trial-spawner, and low-pitched illusioner cues again accompany self-revival and targeted revival.
- Replaced the prior cropped Chimes of Necros asset with the supplied full **`lichlingdoom(3).ogg`** recording, converted to mono 48 kHz OGG while preserving its complete 10.92-second duration.
- Moved Chimes of Necros playback into the ability's guaranteed continuous channel path. The sound now begins on the first valid held-key tick as a guaranteed caster-local copy plus a positional nearby-listener copy. Both are stopped through the channel helper when the key is released, Energy is depleted, or the channel otherwise ends.
- Reworked the **Final Bell Toll** trigger so it plays directly from a target only when that target is first granted Doom. This replaces the unreliable dynamic-power callback and avoids replaying the five-second bell on every added Doom stack.
- Preserved Faerie's existing Allay cue and restored a subtle high-pitched Vex layer to **Flutter** and successful **Fae Illusions** casts, alongside the supplied custom Faerie chime.
- Audio/control-path correction only; no intended damage, cooldown, movement strength, Energy cost, Doom-stack timing, or other balance changes.

# 1.4.20

### Audio follow-up and Manticore lunge helper fix
- Fixed **Manticore Ravenous Lunge** revoking `manticore/helper/chargelogic` at activation instead of granting it. The helper now actually runs during the lunge, restoring repeated midair contact checks, target dragging/contact impacts, and the moving airborne roar pulse.
- The airborne Manticore roar now begins from the moving lunge state immediately and refreshes every 0.5 seconds while airborne, so it follows the Manticore instead of remaining at the launch/contact point.
- Hardened **Humanity Mortal Resolve** heartbeat playback with a guaranteed local pulse for the Human plus a positional nearby-listener pulse every 1.5 seconds. The 1.5-second custom heartbeat asset was also raised again for stronger perceived presence.
- Removed the remaining legacy vanilla Lichling sound actions from **Death's Defiance**. Lichling ability audio is now limited to the supplied custom **Chimes of Necros** channel sound and **Final Bell Toll** Doom cue.
- Audio/helper correction only; no intended damage, cooldown, movement strength, resource cost, or other balance changes.

# 1.4.19

### Positional custom-audio fixes
- Reworked **Humanity Mortal Resolve** heartbeat playback from one 60-second positional bed into a louder 1.5-second custom heartbeat pulse that is replayed from the Human's current position throughout the final stand. This prevents the heartbeat from being left behind and fading out as the player moves.
- Removed the old vanilla bell, amethyst-chime, and trident-thunder layers from **Lichling Chimes of Necros / Doom**, leaving the supplied `lichlingdoom` chime as the channel sound and the supplied Final Bell Toll as the Doom-application cue.
- Raised the supplied **Chimes of Necros** custom audio presence.
- Added a short derived **Manticore Ravenous Lunge airborne roar pulse** that plays from the Manticore's current position while the lunge is in flight, so the roar travels with the action instead of remaining only at the launch point. The full supplied roar still plays on activation, and the separate heavy impact remains on collision.
- Audio/presentation only; no damage, cooldown, movement, resource, or other balance values were intentionally changed.

# 1.4.18

### Custom audio mix and cue corrections
- Raised the custom sound mix for **Veilborn**, Humanity's **Mortal Resolve heartbeat**, **Arachne**, Siren's **Infatuation whisper**, **Manticore**, and **Wyverian** so their supplied audio is easier to hear over combat and vanilla layers.
- Reprocessed the Mortal Resolve heartbeat with additional gain and raised its playback level while preserving the full 60-second bed and existing stop logic.
- Fixed **Manticore Ravenous Lunge** impact audio so the heavy slam now plays on actual targets contacted during the lunge, not only targets already beside the Manticore at activation.
- Moved the Ravenous Lunge custom roar earlier in the activation sequence and raised its playback level. The latest supplied `manticoreprimary` source is bundled.
- Restored **Lichling Chimes of Necros** to the supplied `lichlingdoom` chime bed. The **Final Bell Toll** now plays when Doom is actually granted to a target instead of when the Lichling's Energy reaches zero.
- Restored the latest supplied **Nereid Convalescing Aura** audio at its full ~15.7-second length, raised its presence, and removed the recurring vanilla dolphin/conduit sound layer that was masking it.
- Audio/presentation only; no cooldown, damage, resource-cost, movement, or other balance values were intentionally changed.

# 1.4.17

### README cleanup
- Rebuilt the README as a concise project overview instead of duplicating the full release history.
- Added clear sections for features, the 13 Origins, requirements, controls, compatibility, custom audio, namespace, origin-selection initialization, building, authors, and license.
- Detailed release notes remain in `CHANGELOG.md`, and custom sound assignments remain in `CUSTOM_SOUNDS.md`.
- No gameplay, ability, balance, particle, or audio behavior was changed in this release.

# 1.4.16

### Lichling Final Bell Toll correction
- Corrected the supplied `finalbelltool` audio assignment: it belongs to **Lichling**, not Humanity.
- Renamed and registered it as `andromeda_origins:ability.lichling.final_bell_toll`.
- **Chimes of Necros** now plays the Final Bell Toll when its Energy resource reaches zero at the end of a full channel.
- Mortal Resolve no longer plays the custom bell on natural expiry; its ending uses the low heartbeat/smoke cue instead.

# 1.4.15

### Custom ability audio
- Added 33 custom ability sounds supplied for the project and converted them to mono 48 kHz OGG assets for positional Minecraft playback.
- Registered all custom SoundEvents under the `andromeda_origins:ability.*` namespace.
- Humanity Mortal Resolve now uses a custom ignition and a full-minute heartbeat bed; the heartbeat stops on early death, respawn cleanup, or natural expiry.
- Added custom cues for Arachne, Faerie, Fenrkin, Gorgon, Lichling, Manticore, Nereid, Satyr, Selkie, Siren, Veilborn, and Wyverian active abilities.
- Nereid's 10-second aura and Lichling's channel audio are stopped when their channel helper state ends, avoiding long sounds continuing after cancellation.
- Siren's 15-second charm/whisper assets are trimmed to the fixed Infatuation duration; Wyverian's wing-rush cue is trimmed to the one-second flight pulse cadence.
- This pass is presentation-only and does not intentionally alter ability balance, cooldowns, costs, or gameplay rules.

# 1.4.14

### Origin presentation pass
- Added a lightweight vanilla particles/sounds polish pass to major active abilities across the roster to give each origin more visual identity and aura.
- Humanity now has a stronger heroic activation on Indomitable.
- Faerie, Siren, and Veilborn now lean further into glittering, melodic, and veil-distortion visuals.
- Fenrkin, Manticore, and Gorgon now have stronger predatory / feral / stone cues on activation.
- Lichling now uses additional soul and sculk-flavored feedback on necrotic abilities.
- Nereid and Selkie now have more water-themed bursts, while Wyverian breath and flight have extra fire/wind feedback.
- This is a cosmetic-only pass; no intended balance or rules changes were made.

# 1.4.13

### Humanity — Mortal Resolve presentation
- Added a distinct activation sequence to **Mortal Resolve**: a low heartbeat, beacon/heroic rise sounds, a white flash, and a burst of flame, end-rod, and electric-spark particles.
- Added a sparse flame/end-rod afterglow for the first 1.5 seconds after the Human rises, rather than continuously spawning heavy particles for the full final stand.
- When Mortal Resolve's 60-second timer expires naturally, a final low heartbeat and smoke burst play immediately before the forced final death.
- These effects are cosmetic only; Mortal Resolve's revive, Resistance IV, Strength IV, final-death behavior, and one-minute duration are unchanged.

# 1.4.12

### Humanity — Mortal Resolve
- Added **Mortal Resolve** as Humanity's Secondary Ability. It can only successfully activate while the Human is genuinely downed by Incapacitated.
- Mortal Resolve self-revives the Human and grants **Resistance IV** and **Strength IV** for 1 minute.
- The one-minute final stand is tracked server-side and continues counting while the player is disconnected.
- Any death during Mortal Resolve is final: Andromeda marks the death as having no Incapacitated downs remaining before Incapacitated processes it.
- When the minute expires, the Human dies outright instead of entering the downed state again.
- Added the player-facing lines: *“Trust in yourself right now. Let your heart burn it up. You can change the world.”*

# 1.4.11

### Fenrkin Stamina Surge leap height
- Increased the upward impulse of the Jump-triggered **Stamina Surge** leap from `0.2` to `0.3` world-relative velocity.
- Horizontal launch strength, stamina costs, chained-leap rules, and all other Stamina Surge behavior are unchanged.

# 1.4.10

### Fenrkin chained-leap stamina fix
- Fixed the second **Stamina Surge** leap being rejected when the continuously held sprint consumed a small amount of Stamina after the first leap.
- A first leap started at 18/20 Stamina or higher now opens a 5-second follow-up window.
- During that window, a second Jump-triggered leap may consume the remaining Stamina even if it has fallen below the normal 10-Stamina threshold.
- This preserves the shared sprint/leap Stamina pool without allowing low-Stamina players to chain a cheap second leap.

# 1.4.9

### Fenrkin Stamina Surge leap input
- Fenrkin **Stamina Surge** now uses the normal **Jump** key for its high-cost leap while Secondary Ability is being held.
- The player no longer has to release/re-press Secondary Ability to leap, so the sprint portion of Stamina Surge remains continuously held.
- The leap still costs 50% of maximum Stamina, retains its horizontal-facing/world-up velocity fix, and cannot activate in water or lava.

# 1.4.8

### Movement, rain cooldown, and Gorgon Grab fixes
- Selkie **Surging Tides** now shortens to a 5-second cooldown while physically in water or exposed to rain; dry land remains 30 seconds.
- Fenrkin **Stamina Surge** no longer automatically triggers the 50%-stamina airborne leap when briefly leaving the ground while running over block edges.
- Fenrkin's airborne leap is now a discrete Secondary Ability press while airborne and uses horizontal-facing velocity so looking downward cannot push the Fenrkin into the ground.
- Satyr **Swift Leap** now preserves existing momentum instead of replacing it when the double jump fires.
- Gorgon **Grab** now marks carried players and prevents self-dismount while the grab is active. Intentional throw, Sneak + Jump placement, and the 10-second timeout remove the grab lock before dismounting.

# 1.4.7

### Wyverian flight and Selkie water-state fixes
- Fixed Wyverian **Gusts of Freedom** continuing to apply propulsion after its 20-second flight-time resource reached 0.
- Gusts of Freedom now requires active fall-flying, at least 1 remaining charge, no water submersion, and no Restrained state before it can fire.
- Fixed Selkie land-weakness recovery decrementing while the player was actively swimming. Swimming/full submersion now counts as valid recovery alongside the existing deep-water check.
- Fixed Selkie **Surging Tides** incorrectly receiving its 5-second cooldown on land while stored Wet status was active.
- Surging Tides now uses one 30-second base cooldown and shortens that same cooldown to 5 seconds only when the dash is activated while physically in water.
- Removed the obsolete auxiliary `primary_dashcooldown` sub-power.

# 1.4.6

### Compatibility tag and Nereid fixes
- Merged the misspelled `alpinerwhispers` iron compatibility file into the canonical `alpinewhispers` tag.
- Corrected `arolla_pine_bathtub` and `arolla_pine_privy` to the `alpinewhispers:` namespace.
- Removed the duplicate/misspelled `andromeda_origins:iron/alpinerwhispers` child tag from `alliron`.
- Fixed Nereid accidentally referencing `andromeda_origins:lichling/figura`; it now uses `andromeda_origins:nereid/figura`.

# 1.4.5

### Rebindable origin toggle
- Replaced every Andromeda power that piggybacked on the vanilla Player List key with a dedicated **Origin Toggle** keybinding.
- The new keybinding appears under **Controls → Andromeda Origins** and can be rebound independently.
- Default key is **C**, matching the vanilla Save Hotbar Activator default suggested for the toggle, while remaining a separate binding.
- Updated Wyverian Ember Flames firing-mode tooltip to display the dedicated Origin Toggle binding.

# 1.4.4

### License and Siren passive correction
- Replaced the MIT License with the **Bare Minimum License (BML) v1.0**.
- Kept **jaselumena**, **pokesmells**, and **aulatris** as the project copyright holders/authors.
- Corrected the intended Siren passive: **Luck** has been replaced with permanent **Haste I**.
- Removed the mistakenly assigned permanent **Haste I** from Gorgon.
- Updated the Siren and Gorgon player-facing passive descriptions to match.

# 1.4.3

### Repository metadata
- Added the initial repository license (superseded by BML v1.0 in 1.4.4).
- Added **jaselumena**, **pokesmells**, and **aulatris** as project authors.
- Added author and license metadata to `fabric.mod.json`.

# 1.4.2

### New-mod namespace cleanup
- Removed the pre-release Origins/Apoli save-data namespace migration and both migration Mixins.
- Removed join-time legacy attribute cleanup and the `/andromedaorigins cleanup` command; this mod is treated as a new project and does not support importing prototype save IDs.
- Kept the native `andromeda_origins:` namespace for all live content and current persistent modifier IDs.
- Kept `/andromedaorigins repair <player>` for current transient crowd-control/Incapacitated recovery, without any old-save cleanup step.
- `fabric.mod.json` no longer declares a Mixin configuration because the migration Mixins were the only Mixins in the project.

# Andromeda Origins 1.3.9

## 1.3.9

### Weaver's Nest pull fix
- Fixed Weaver's Nest launching or flinging targets through/past the web center instead of cleanly trapping them.
- Reduced the center pull from an additive `2.0` velocity impulse to a controlled `0.35` pull.
- The pull now **sets** the victim's velocity instead of adding to existing sprint/jump/fall momentum, preventing accumulated momentum from turning the web pull into a launch.
- Restrained/Webbed behavior and duration are otherwise unchanged.

# Andromeda Origins 1.3.8

## 1.3.8

### Lore terminology polish
- Refined the player-facing origin descriptions for Faerie, Gorgon, Lichling, Selkie, Siren, and Veilborn to better align with the Andromeda lore master terminology.
- Lichling now describes its power as **necrotic energy** rather than generic spellcasting.
- **Chimes of Necros** uses **Energy** instead of Mana in its GUI description and depletion/recovery actionbar messages.
- Internal power/resource identifiers were intentionally left unchanged for save and datapack compatibility.

## 1.3.7

- Fixed a Fenrkin stale stalking-speed modifier that could leave the player heavily slowed after cooldowns ended or after changing/reselecting origins.
- Fenrkin now explicitly removes `andromeda_origins:fenrkin_primary_stalking_slow` when the origin is selected, added, lost, and whenever the stalking state returns to zero.

### Origin-selection reset pass
- All 13 origins now refresh their ability cooldowns when the origin is actually chosen or re-chosen.
- Ability cooldowns no longer begin partially spent simply because a player selected an origin.
- Origin-owned temporary resources (charges, stamina, mode/state counters, and similar ability state) reset to their configured starting values on selection.
- Cooldowns are **not** reset on normal login/reconnect, so reconnecting cannot be used to bypass cooldowns.
- Fixed Human **Indomitable** selection initialization: removed the old `+35980` cooldown manipulation that could place the ability on cooldown immediately after choosing Human.
- Fenrkin now begins with On the Hunt, Mark of Fenrir, Adrenaline, Stamina Surge, and internal stalking timers in a clean ready/default state when selected.

# Andromeda Origins 1.3.5

### Fenrkin weakness-state fix
- Fixed Fenrkin spawning/being selected with its weakness resource already active.
- Removed the stray nighttime/exposed-to-sky logic from Fenrkin weakness tracking.
- Fenrkin weakness now follows its intended Wet/cramped-space behavior: Wet time builds a lingering timer up to 120 seconds, the timer drains while dry, and cramped overhead spaces apply only while cramped.
- Normal Fenrkin movement bonus is now available immediately when the origin is selected under neutral conditions.

# Andromeda Origins 1.3.4

### GUI description overhaul
- Rewrote all 13 origin lore descriptions for clearer, more consistent in-game presentation.
- Rewrote all visible passive and active ability descriptions while preserving existing mechanics.
- Updated descriptions to reflect the current ability names introduced in 1.3.3.
- Updated descriptions for the latest balance/behavior changes, including Fenrkin stalking, Adrenaline, Selkie wetness and Pescatarian diet, Siren buffs, Curtain Step cooldown, Undetectable detection immunity, inventory-wide iron weakness, and dash projectile penalties.
- Fixed malformed GUI formatting in Arachne Webspinner, Satyr movement abilities, Selkie primary abilities, and Veilborn primary abilities.
- Corrected stale iron vulnerability tooltip values from flat `+2` wording to the current `+20%` wording.
- Corrected Human passive text to match the actual configured `-2 Max HP` modifier.
- Standardized Wyverian **Gusts of Freedom** to the plural display name.

# Andromeda Origins 1.3.3

### Ability naming pass
- Fenrkin: **Rushdown** → **Stamina Surge**. **On the Hunt** and **Mark of Fenrir** remain unchanged.
- Wyverian: **Ember Beam** → **Ember Ray**; **Ember Sphere** → **Ember Pyroclast**.
- Manticore: **Lunge** → **Ravenous Lunge**.
- Arachne: **Eightfold Footwork** → **Eightfold Swiftness**.
- Faerie: **Misty Visions** → **Fae Illusions**; **Prankster** → **Trickster**.
- Satyr: **Mighty Leap** → **Swift Leap**; **Mighty Stomp** → **Satyr's Landing**; **Ram** → **Rush**; **Foresight** → **Vigil Perception**.
- Gorgon: **Intimidating Speed** → **Transference**.
- Veilborn: **Veil Pearl** → **Curtain Step**.
- Lichling: **Undead Resilience** → **Death's Defiance**; **Antilife Vortex** → **Chimes of Necros**.
- Selkie: **Tide of Life** → **Coastal Phalanx**; **Tidal Surge** → **Surging Tides**; **Tidal Defense** → **Sealskin Bastion**.
- Siren: **Sonar Shriek** → **Shrieking Wail**.
- Nereid: marking effect is now labeled **Nereid's Mark**; **Convalescing Halo** → **Convalescing Aura**; **Whirlpool Strikes** → **Submersion**.
- Human **Indomitable** remains unchanged.
- Updated player-facing descriptions and actionbar messages to use the new names.

# Andromeda Origins 1.3.2

- Fixed Fenrkin **On the Hunt** stalking slowdown being applied after a target was marked instead of while stalking.
- The 75% movement penalty now applies only while sneaking, holding Primary Ability, and actively tracking a valid target.
- The stalking slowdown clears immediately when the key is released, the target is lost, or the mark completes.
- The marked-target helper no longer carries any movement-speed penalty.

# Andromeda Origins 1.3.1

- Human **Indomitable** cooldown reduced from 30 minutes to **15 minutes** (18,000 ticks).
- Updated the Indomitable description to state its 15-minute cooldown.

# Andromeda Origins 1.3.0

- Fenrkin On the Hunt no longer applies Hunter's Caution after the hunt; the movement penalty is confined to stalking.
- Fenrkin Adrenaline resistance lasts 35 seconds and can trigger once every 3 minutes.
- Selkie now has the pescatarian diet restriction.
- Converted all remaining flat incoming/outgoing/projectile damage modifiers to percentage modifiers, including Lichling’s Weakness-style hit debuff.
- Veil Pearl now has a 3 second cooldown.
- Added a 50% projectile-damage penalty for 3 seconds after high-momentum dash abilities (Arachne, Fenrkin, Manticore, Satyr, Selkie, Wyverian).
- Reworked Selkie wetness: 3 minutes to dry out, wetness rises in water/rain and falls while dry; land weakness recovery requires 30 seconds of deep/full-body water and counts backward when leaving it.
- Fixed Selkie ground weakness decrementing the wrong shared source counter.
- Gorgon now receives Haste I as a passive effect.
- Siren Sonar Shriek now reaches about 10 blocks and deals 6 sonic damage.
- Siren Infatuation now lasts a fixed 15 seconds and cannot be broken by self-damage or movement.
- Carrying iron-tagged items anywhere in the inventory now triggers the persistent withering iron weakness for Gorgon, Faerie, and Lichling.

# Changelog

## 1.2.2

### Undetectable / detection compatibility
- Fixed Fenrkin **On the Hunt** revealing targets that currently have `andromeda_origins:common/undetectable`.
- Fenrkin target-marking raycasts now reject Undetectable targets.
- Existing Fenrkin stalk marks no longer render an outline while the target is Undetectable.
- All Andromeda Origins `origins:entity_glow` and `origins:self_glow` renderers now respect the target holder's Undetectable state, covering Arachne sense, Manticore health auras, Nereid friend outlines, and similar custom outlines.
- Vanilla `minecraft:glowing` immunity remains in `common/undetectable`.

## 1.2.0

### Incapacitated compatibility
- Lichling self/target revive no longer blindly calls `incapacitated setDowned false`.
- Added an optional reflection bridge that checks Incapacitated's own player state before reviving.
- Reset Incapacitated's transient last-damage/pre-hit-health tracking after Andromeda-triggered revives.
- Replaced Lichling's undead Instant Damage healing trick with `origins:heal`.

### Player-data / attribute safety
- Replaced 31 explicitly named attribute/modifier IDs with unique `andromeda_origins:*` IDs.
- Added `/andromedaorigins repair <player>` for emergency health/downed/control-state recovery.

### Crowd-control state fixes
- Fixed Arachne Webbed decrementing the shared restraint source counter twice on normal expiry.
- Fixed Gorgon Constricted doing the same.
- Repair command clears stale ccontrol powers and resets shared control resource counters.

### Existing content retained
- 64x64 origin selection icons remain included.
- Andromeda Origins branding and mod icon remain included.
- Existing `andromeda_origins:` content namespace remains intact for save/Figura compatibility.
