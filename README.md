# Andromeda Origins

**Andromeda Origins** is the Fabric 1.21.1 Origins/Apoli content mod used by Andromeda MC. It contains the server's 13 custom Origins, command-only Champion variants, custom ability presentation, HUD resources, compatibility hooks, and supporting Java systems.

## Current release

**v1.4.73** — Nereid gill-state safety plus support/repair hardening. Standard Nereid land suffocation now uses a dedicated gill source and self-repairs on join; Nereid kelp ally marks now expire after 15 seconds, and Convalescing Aura/Submersion hydrate only currently marked Selkies/Nereids. Submersion now uses real water physics: aquatic targets still sink but are immune to forced drowning, while unmarked non-aquatic targets are sunk, heavily movement-limited, rapidly lose air, and drown once it is exhausted. Long channel sounds were remixed for voice-chat clarity, Arachne cobweb shift-click crafting is guarded against the Apoli pre.2 duplication path, and `/andromedaorigins repair` now cleans stale legacy Origin sources/modifiers such as Medieval Origins Pixie health/size/diet residue before rebuilding the current Andromeda attributes.

## Features

- 13 standard custom Origins.
- 13 command-only Champion variants hidden from normal Origin selection.
- Active/passive powers built with Origins and Apoli.
- Native Minecraft scale and step-height attributes; **Pehkui is not required**.
- Custom 64×64 registered Origin icon items.
- Two custom 256×256 HUD resource-bar sheets with 31 catalogued styles.
- 33 registered custom ability sounds plus layered vanilla audio.
- Optional Spell Engine / More RPG Library enhanced audiovisual layer with data-driven Origin FX definitions, impact-position support, extra flashy event hooks, and no hard runtime dependency.
- Optional Incapacitated integration for downed/revival mechanics.
- Mortal Resolve restores Incapacitated counters after its final-death handoff and repairs legacy stale unlimited-down counters on join.
- Optional Figura compatibility with legacy hooks and a semantic Lua helper.
- Optional Figura ExtraBone interoperability guidance for PlayerAnimator/Emotecraft blending.
- Optional Figura ↔ Armor Model API visibility bridge so custom geo armor can stay hidden on full-body Figura avatars.
- Shared **Undetectable** state with mob-target immunity plus hidden armor/held-item rendering.
- Dietary compatibility tags, including carnivore, vegetarian, raw-food, and pescatarian restrictions.
- Iron-weakness compatibility tags for vanilla and modded iron items.
- A rebindable Andromeda-specific **Origin Toggle** control.
- Staff recovery command for interrupted crowd-control/downed-state problems.
- Server-side Apoli hot-path caching and EntitySet unload indexing to reduce repeated full-power/full-world scans.

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
| Nereid | Convalescing Aura | Submersion | Aquatic support, marked-ally hydration, sinking and drowning control |
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

Every standard Origin has a **Champion** variant. Champions retain the Origin's identity-defining strengths and active abilities while removing racial weaknesses and clear self-debuffs. The current parity audit also confirms that positive ability-side protections such as the shared 50% movement-ability projectile reduction remain intact on Champions. Charge times, resources, and target-facing counterplay are still preserved. Champion cooldowns generally recover at 2× rate, with explicit Champion-specific overrides documented separately (currently Champion Veilborn Auroral Mirage at 5 seconds).

Champion entries use `unchoosable: true`, so they do **not** appear in the normal Origins GUI. Assign them with operator permissions:

```mcfunction
/origin set <player> origins:origin andromeda_origins:champion_<origin>
```

Example:

```mcfunction
/origin set <player> origins:origin andromeda_origins:champion_fenrkin
```

See [CHAMPION_ORIGINS.md](CHAMPION_ORIGINS.md) for every ID, removed drawback, and Champion-specific safety behavior.

## Optional enhanced Origin FX

Spell Engine and More RPG Library are **suggested, not required**. Andromeda checks for them at runtime and loads its particle bridge only when Spell Engine is present. Existing bundled sounds and vanilla particles are never replaced, so removing the libraries or disabling enhanced FX returns the mod to its normal presentation.

Enhanced FX definitions are data-driven under `data/andromeda_origins/andromeda_fx/` and currently cover 109 enhanced events across all 13 Origins. Operators can compare the two presentation modes without restarting:

```mcfunction
/andromedaorigins enhanced_fx
/andromedaorigins enhanced_fx enabled false
/andromedaorigins enhanced_fx particles false
/andromedaorigins enhanced_fx sounds false
```

The switches persist in `config/andromeda_origins_fx.json`. See [ENHANCED_FX.md](ENHANCED_FX.md) for the event/asset mapping and architecture.

## Requirements

- Minecraft **1.21.1**
- Java **21**
- Fabric Loader **0.16.5+**
- Fabric API
- Origins
- Apoli **2.12.0-pre.2+mc.1.21.1**

The development properties currently target:

- Origins `1.13.0-pre.2+mc.1.21.1`
- Apoli `2.12.0-pre.2+mc.1.21.1`

### Optional integrations

- **Incapacitated** — downed/revive compatibility is enabled automatically when installed.
- **Figura** — optional avatar animation compatibility; not required on server or client.
- **Figura ExtraBone** — optional avatar skeleton/blending aid; not a dependency of Andromeda Origins.
- **Armor Model API** — optional custom-armor compatibility; its geo armor renderer will respect Figura armor visibility when both mods are installed.
- **Spell Engine** — optional enhanced Origin particles and supporting sound layers; base Andromeda FX remain the fallback.
- **More RPG Library** — optional additional water, wind, stone, claw, music, and other themed FX assets used when available. More RPG Library manages its own dependencies.

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

Andromeda Origins currently registers **34 custom SoundEvents** under:

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
/andromedaorigins repair_attributes <player>
```

`repair` is the full recovery command. It cleans stale legacy Origin ownership/modifiers, clears stuck Andromeda transient helper powers, runs the iron/Nereid/Selkie compatibility migrations, rebuilds the player's Andromeda-managed raw attributes, and re-applies the currently granted Apoli/Origin attribute powers before performing the crowd-control/Incapacitated cleanup. This means an Arachne, Lichling, Champion variant, etc. is repaired back to that Origin's effective stats rather than being left at vanilla-player values. `repair_attributes` runs only the attribute rebuild. Neither command re-selects the Origin, so ability cooldowns/resources are not reset.

## Exact client/server version sync

Andromeda Origins now performs a Fabric login-query handshake before a player joins. The server rejects clients that do not have Andromeda Origins installed, and it rejects clients whose Andromeda Origins version does not exactly match the server. The disconnect message shows the required/server version and the client's reported version when available.

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
build/libs/andromeda-origins-1.21.1-1.4.52.jar
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


