# Andromeda Origins

**Andromeda Origins** is the Fabric 1.21.1 Origins/Apoli content mod used by Andromeda MC. It contains the server's 13 custom Origins, command-only Champion variants, custom ability presentation, HUD resources, compatibility hooks, and supporting Java systems.

## Current release

**v1.4.46** — HUD resource-bar mapping correction. The v1.4.45 Veilborn balance and Undetectable stealth behavior are unchanged. The 31-style HUD catalog now correctly uses 25 styles on sheet 1 and 6 styles on sheet 2: Wyverian Wings is sheet 1 `bar_index: 24`, while sheet 2 begins with Nereid Halo at `bar_index: 0`.

## Features

- 13 standard custom Origins.
- 13 command-only Champion variants hidden from normal Origin selection.
- Active/passive powers built with Origins and Apoli.
- Native Minecraft scale and step-height attributes; **Pehkui is not required**.
- Custom 64×64 registered Origin icon items.
- Two custom 256×256 HUD resource-bar sheets with 31 catalogued styles.
- 33 registered custom ability sounds plus layered vanilla audio.
- Optional Incapacitated integration for downed/revival mechanics.
- Optional Figura compatibility with legacy hooks and a semantic Lua helper.
- Optional Figura ExtraBone interoperability guidance for PlayerAnimator/Emotecraft blending.
- Optional Figura ↔ Armor Model API visibility bridge so custom geo armor can stay hidden on full-body Figura avatars.
- Shared **Undetectable** state with mob-target immunity plus hidden armor/held-item rendering.
- Dietary compatibility tags, including carnivore, vegetarian, raw-food, and pescatarian restrictions.
- Iron-weakness compatibility tags for vanilla and modded iron items.
- A rebindable Andromeda-specific **Origin Toggle** control.
- Staff recovery command for interrupted crowd-control/downed-state problems.

## Origin roster

| Origin | Primary | Secondary / additional active | Identity |
|---|---|---|---|
| Arachne | Eightfold Swiftness | Weaver's Nest / Webspinner | Fast arthropod movement, webs, venom, climbing |
| Faerie | Flutter | Fae Illusions / Trickster | Small fae mobility, concealment, illusion tricks |
| Fenrkin | On the Hunt / Mark of Fenrir | Stamina Surge | Predator tracking, stamina movement, venom, Adrenaline |
| Gorgon | Ophidian Gaze | Transference | Petrification, poison/slowness pressure, speed transfer |
| Humanity | Indomitable | Mortal Resolve | Fragile mortal baseline with powerful last-stand tools |
| Lichling | Death's Defiance | Chimes of Necros | Undead sustain, revival, Doom/Wither channeling |
| Manticore | Ravenous Lunge | Beast of Blood | Heavy predator melee, pounce, Unstoppable state |
| Nereid | Convalescing Aura | Submersion | Aquatic support, healing, Wet application |
| Satyr | Rush / Swift Leap / Satyr's Landing | Vigil Perception | Momentum-driven mobility and wind/gust movement |
| Selkie | Coastal Phalanx / Surging Tides | Sealskin Bastion | Aquatic mobility, wetness management, defensive support |
| Siren | Infatuation Tempo | Shrieking Wail | Pacification/Silence and sonic crowd control |
| Veilborn | Curtain Step / Veil Transposition | Auroral Mirage | 9-heart veil skirmisher; position swapping, Undetectable burst |
| Wyverian | Ember Flames | Gusts of Freedom / Hover | Fire, Ember resource, elytra-style aerial mobility |

Player-facing ability details remain in the Origin/power descriptions in game.

### Veilborn balance (v1.4.45)

Standard Veilborn now use the following drawback values:

- **18 HP / 9 hearts** (`-2 Max HP`).
- Armor-weight thresholds apply **-10% movement and swim speed each** instead of -15%.
- Wet/Unstable reduces melee and projectile damage dealt by **60%** instead of 80%; its Silence and lingering-duration rules are unchanged.
- Repeated Curtain Step uses can extend self-applied Reality Shatter to a maximum of **90 seconds** instead of 120 seconds.

Champion Veilborn remain unaffected by these drawback changes because the Champion variant already removes the corresponding penalties.

## Champion Origins

Every standard Origin has a **Champion** variant. Champions retain the Origin's identity-defining strengths and active abilities while removing racial weaknesses and clear self-debuffs. Ability cooldowns, charge times, resources, and target-facing counterplay are still preserved.

Champion entries use `unchoosable: true`, so they do **not** appear in the normal Origins GUI. Assign them with operator permissions:

```mcfunction
/origin set <player> origins:origin andromeda_origins:champion_<origin>
```

Example:

```mcfunction
/origin set <player> origins:origin andromeda_origins:champion_fenrkin
```

See [CHAMPION_ORIGINS.md](CHAMPION_ORIGINS.md) for every ID, removed drawback, and Champion-specific safety behavior.

## Requirements

- Minecraft **1.21.1**
- Java **21**
- Fabric Loader **0.16.5+**
- Fabric API
- Origins
- Apoli

The development properties currently target:

- Origins `1.13.0-pre.3+mc.1.21.1`
- Apoli `2.12.0-pre.3+mc.1.21.1`

### Optional integrations

- **Incapacitated** — downed/revive compatibility is enabled automatically when installed.
- **Figura** — optional avatar animation compatibility; not required on server or client.
- **Figura ExtraBone** — optional avatar skeleton/blending aid; not a dependency of Andromeda Origins.
- **Armor Model API** — optional custom-armor compatibility; its geo armor renderer will respect Figura armor visibility when both mods are installed.

## Controls

Origins supplies the standard Primary and Secondary Ability keys.

Andromeda Origins additionally registers:

- **Origin Toggle** — defaults to **C**, independently rebindable under **Controls → Andromeda Origins**.

Minecraft's Creative Save Hotbar Activator also uses C by default, so Creative players may want to rebind one of them.

## Custom HUD bars

The custom HUD art is stored in:

```text
assets/andromeda_origins/textures/gui/resource_bars_1.png
assets/andromeda_origins/textures/gui/resource_bars_2.png
```

- Sheet 1 contains global styles **01–25** using Origins `bar_index` **0–24**.
- Sheet 2 contains global styles **26–31** using Origins `bar_index` **0–5**.

See [RESOURCE_BARS.md](RESOURCE_BARS.md) for the full 31-style assignment table and the Nereid shared-Wet-resource exception.

## Custom ability audio

Andromeda Origins currently registers **33 custom SoundEvents** under:

```text
andromeda_origins:ability.*
```

Assets live under:

```text
assets/andromeda_origins/sounds/abilities/
```

Long channel/ambience sounds use explicit local/positional playback and stop handling where needed. See [CUSTOM_SOUNDS.md](CUSTOM_SOUNDS.md) for the complete current sound list and behavior.

## Figura compatibility

Figura remains optional. Existing avatars can continue reading:

```text
andromeda_origins:common/figura_1
...
andromeda_origins:common/figura_5
```

New avatars should use the bundled semantic Lua helper instead of hard-coding numbered states:

```text
assets/andromeda_origins/figura/andromeda_origins.lua
```

As of v1.4.44, Armor Model API custom geo armor also respects Figura's armor visibility. An avatar can keep using:

```lua
vanilla_model.ARMOR:setVisible(false)
```

and Armor Model API-rendered armor will be suppressed for that avatar when Figura permits vanilla-model editing.

See [FIGURA_COMPAT.md](FIGURA_COMPAT.md) for state names, helper functions, Mortal Resolve support, ExtraBone/PlayerAnimator guidance, and custom-armor behavior.

## Compatibility and safety

Important compatibility behavior includes:

- safe Incapacitated revive integration,
- Mortal Resolve final-death handling,
- Undetectable-aware detection/glow checks, mob-target rejection, held-item hiding, and Armor Model API geo-armor suppression,
- shared crowd-control source-counter protection,
- Champion AoE self-hit safeguards,
- no Gorgon player/entity carry system,
- Siren Shrieking Wail using direct raycasts instead of spawned armor-stand helpers,
- native Minecraft scaling without Pehkui,
- Figura-aware suppression of Armor Model API custom geo armor when an avatar hides vanilla armor.

See [COMPATIBILITY.md](COMPATIBILITY.md) for the implementation notes.

## Admin recovery command

```mcfunction
/andromedaorigins repair <player>
```

Use this for a player stuck in an interrupted Andromeda crowd-control/downed state instead of deleting playerdata.

## Content namespace

All active project content uses:

```text
andromeda_origins:
```

The project does not ship a migration layer for unsupported prototype namespaces.

## Origin-selection initialization

Selecting, reselecting, or command-assigning a standard/Champion Andromeda Origin resets that Origin's owned cooldown/resources to its configured starting state. Figura compatibility states are also cleared so interrupted animations do not remain stuck after an Origin change or respawn.

A normal reconnect does not intentionally refresh active ability cooldowns.

## Building

The source includes the Gradle 8.8 wrapper.

### Windows PowerShell / Command Prompt

```bat
gradlew.bat build
```

### Linux / macOS

```bash
./gradlew build
```

Use **Java 21**. The release JAR is written to:

```text
build/libs/andromeda-origins-1.21.1-1.4.46.jar
```

## Documentation

- [CHANGELOG.md](CHANGELOG.md) — release history
- [CHAMPION_ORIGINS.md](CHAMPION_ORIGINS.md) — Champion IDs and removed drawbacks
- [COMPATIBILITY.md](COMPATIBILITY.md) — compatibility/state-safety implementation notes
- [CUSTOM_SOUNDS.md](CUSTOM_SOUNDS.md) — complete custom-audio registry and behavior
- [FIGURA_COMPAT.md](FIGURA_COMPAT.md) — Figura/avatar integration
- [RESOURCE_BARS.md](RESOURCE_BARS.md) — custom HUD sheet/index catalog

## Authors

- jaselumena
- pokesmells
- aulatris

## License

Licensed under the **Bare Minimum License (BML) v1.0**. See [LICENSE](LICENSE).
