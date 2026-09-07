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
