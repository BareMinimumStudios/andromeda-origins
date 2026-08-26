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
