# Andromeda Origins

Fabric 1.21.1 custom Origins content for Andromeda MC.

## Authors

- jaselumena
- pokesmells
- aulatris

## License

Licensed under the Bare Minimum License (BML) v1.0. See [LICENSE](LICENSE).

## Requirements

- Minecraft 1.21.1
- Java 21
- Fabric Loader 0.16.5+
- Fabric API
- Origins
- Apoli 2.11.11+
- Incapacitated is optional; compatibility is enabled automatically when it is installed.

## Controls

- **Origin Toggle** — defaults to **C** and is independently rebindable under **Controls → Andromeda Origins**.
- This replaces the old power toggles that were attached to the vanilla Player List binding.
- C is also the vanilla Save Hotbar Activator default, so Creative-mode users may wish to rebind one of the two controls.

## Building

This project includes the Gradle 8.8 wrapper.

### IntelliJ

1. Open this folder as a Gradle project.
2. Set the Gradle JVM to Java 21.
3. Set Gradle distribution to **Wrapper**.
4. Reload Gradle.
5. Run the `build` task.

### Windows terminal

```bat
gradlew.bat build
```

The jar will be written to:

```text
build/libs/andromeda-origins-1.21.1-1.4.5.jar
```



## 1.4.5 rebindable origin toggle

- Added a dedicated **Origin Toggle** client keybinding for all former Player List-based power toggles.
- Defaults to C and can be rebound independently under the Andromeda Origins control category.
- Wyverian firing-mode toggle now uses the same dedicated binding.

## 1.4.4 license and Siren passive correction

- Replaced the MIT License with the Bare Minimum License (BML) v1.0.
- Corrected the intended Siren passive: Siren now has permanent Haste I instead of Luck.
- Removed the mistakenly assigned permanent Haste I from Gorgon.

## 1.4.3 repository metadata

- Added the initial repository license (superseded by BML v1.0 in 1.4.4).
- Added jaselumena, pokesmells, and aulatris as project authors.
- Added author/license metadata to `fabric.mod.json`.


## 1.4.2 native namespace cleanup

- All mod-owned content uses the single native namespace `andromeda_origins:`.
- The project is treated as a new mod: no old-save namespace migration or compatibility Mixin layer is shipped.
- `/andromedaorigins repair <player>` remains available for current transient crowd-control/Incapacitated recovery.


## 1.3.9 Weaver's Nest pull fix

- Weaver's Nest now uses a controlled pull instead of a large additive velocity impulse.
- The pull sets target velocity toward the nest center, preventing sprinting, jumping, falling, or other existing momentum from turning the restraint into a launch.

## 1.3.8 lore terminology polish

- Refined Faerie, Gorgon, Lichling, Selkie, Siren, and Veilborn origin descriptions to better match Andromeda lore terminology.
- Lichling **Chimes of Necros** now calls its player-facing resource **Energy** instead of Mana. Internal power/resource IDs are unchanged for compatibility.
- Updated Chimes of Necros depletion/recovery actionbar messages to use Energy consistently.

## 1.3.5 Fenrkin weakness fix

- Fenrkin no longer initializes with its weakness state active when selected or re-added.
- Removed the unrelated night/sky logic that was driving Fenrkin weakness.
- Fenrkin weakness now tracks the behavior described in the GUI: wetness builds a lingering timer up to 2 minutes, dries down 1 second per second, and cramped overhead spaces apply the weakness only while cramped.

## 1.3.0 balance and behavior changes

- Fenrkin On the Hunt no longer causes post-hunt slowness; stalking is the slowed state.
- Fenrkin Adrenaline: 35 seconds, 3 minute cooldown.
- Selkie is pescatarian and now has a rebuilt wetness/land-weakness timer.
- Remaining flat damage modifiers were converted to percentages.
- Curtain Step has a 3 second cooldown.
- High-momentum dashes apply a temporary projectile-damage correction.
- Gorgon receives Haste I.
- Siren Shrieking Wail reaches about 10 blocks and deals 6 sonic damage.
- Siren Infatuation lasts 15 seconds and cannot be broken early.
- Iron carried anywhere in the inventory now counts for the persistent iron-withering weakness on affected origins.

## 1.2.2 Undetectable fixes

- `Undetectable` now blocks Origins-based aura/glow detection, not only the vanilla Glowing status effect.
- Fenrkin **On the Hunt** no longer reveals Undetectable Faeries/Veilborn.
- Fenrkin manual marking cannot acquire an Undetectable target.
- Existing marks stop rendering while the target is Undetectable and resume when the effect ends.
- Arachne sense, Manticore health-sense auras, Nereid friend outlines, and other Origins self/entity glow renderers also respect Undetectable.

## 1.2.1 safety changes

- Unique IDs for all explicitly named persistent attribute modifiers.
- `/andromedaorigins repair <player>` recovery command for broken health/downed state.
- Lichling revive now checks Incapacitated state before reviving and resets its transient damage tracking afterward.
- Lichling full heal now uses `origins:heal` instead of Instant Damage.
- Fixed Arachne Webbed restraint source double-decrement.
- Fixed Gorgon Constricted restraint source double-decrement.

See [COMPATIBILITY.md](COMPATIBILITY.md) for details.

## Content namespace

The Fabric mod ID and all active Andromeda content now use the same namespace: `andromeda_origins:`.
Origins, powers, tags, functions, icon items, textures/models, Figura helper powers, and internal cross-references all use this namespace.

This source is treated as a new mod: no prototype namespace or save-data migration layer is shipped. External content should reference `andromeda_origins:` directly.

## Origin selection initialization

When a player chooses or re-chooses one of the 13 Andromeda origins, that origin's ability cooldowns are refreshed and its origin-owned temporary resources are restored to their configured starting values. This runs through Origins' `entity_action_chosen` callback, not ordinary login callbacks, so reconnecting does not refresh abilities.

