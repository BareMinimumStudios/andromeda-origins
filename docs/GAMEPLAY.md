# Gameplay guide

## In-game Origin / power configuration

Gameplay configuration is registered through **Fzzy Config 0.7.7+1.21** and stored as TOML under:

```text
config/andromeda_origins/gameplay.toml
```

Fzzy Config was introduced in `.18` and remains a required dependency. It provides validation, search, nested categories, permission-aware synced editing, restore/default controls, and automatic Mod Menu integration. The old `.17` file `config/andromeda_origins.json` is no longer read and may be deleted after upgrading.

The config can be opened from the **Config** button for Andromeda Origins in Mod Menu, or with:

```mcfunction
/andromedaorigins config
```

On multiplayer servers, synced gameplay entries use Fzzy Config's default permission level **2**. Players without sufficient permission can receive/use the authoritative server state but cannot commit server gameplay changes.

The generated UI is organized as:

- **Standard Origins** → one section for each of the 13 normal Andromeda Origins.
- **Champion Origins** → one section for each of the 13 Champion variants.
- **Overgrown Defaults** → a master stock-Origin switch plus individual stock-Origin switches.

Each Andromeda/Champion Origin section begins with **Origin Enabled**, followed only by that Origin's audited top-level gameplay powers, diets, and weaknesses. Internal Figura state, Champion cooldown acceleration, and nested implementation helpers are intentionally excluded. Aquatic/Champion gameplay identity is handled by native Origin tags rather than a configurable marker power.

Power switches are **per Origin**. For example, disabling Iron Weakness for Faerie does not disable Gorgon's or Lichling's copy, even where the underlying power ID is shared. Linked Iron Weakness implementation pieces are still grouped behind one visible switch.

Disabled powers are **suppressed through Opoli**, not revoked. Ownership and stored cooldown/resource state remain intact, so re-enabling a power does not recreate or reset it. Disabling a currently selected Origin clears that invalid selection and returns the player to the normal Origin-choice flow. Origin selection is explicit; the Random Origin choice is disabled.

Ordinary Fzzy Config changes apply live; a full game/server restart is normally unnecessary. `/reload` is only relevant when datapacks themselves are being changed. Fzzy Config's `SaveType.SEPARATE` is used so a remote server's synced gameplay settings do not overwrite the player's local/single-player config file.

## Origin roster

| Origin | Primary | Secondary / additional active | Identity |
|---|---|---|---|
| Arachne | Eightfold Swiftness | Weaver's Nest / Webspinner | Fast arthropod movement, webs, venom, climbing |
| Faerie | Flutter | Fae Illusions / Trickster | Small fae mobility, concealment, illusion tricks |
| Fenrkin | On the Hunt / Mark of Fenrir | Stamina Surge | Predator tracking, stamina movement, Adrenaline |
| Gorgon | Ophidian Gaze | Transference | Petrification, poison/slowness pressure, speed transfer |
| Humanity | Indomitable | Mortal Resolve | Fragile mortal baseline with powerful last-stand tools |
| Lichling | Death's Defiance | Chimes of Necros | Undead sustain, revival, Doom/Wither channeling |
| Manticore | Ravenous Lunge | Beast of Blood | Heavy predator melee, pounce, Unstoppable state |
| Nereid | Convalescing Aura | Submersion | Aquatic support, marked-ally hydration, sinking and drowning control |
| Satyr | Rush / Swift Leap / Satyr's Landing | Vigil Perception | Momentum-driven mobility and wind/gust movement |
| Selkie | Coastal Phalanx / Surging Tides | Sealskin Bastion | Aquatic mobility, wetness management, defensive support |
| Siren | Infatuation Tempo | Shrieking Wail | Pacification/Silence and sonic crowd control |
| Veilborn | Veil Step / Veil Transposition | Auroral Mirage | 9-heart veil skirmisher; rapid Veil repositioning, position swapping, Undetectable burst |
| Wyverian | Ember Flames | Gusts of Freedom / Hover | Fire, Ember resource, elytra-style aerial mobility |

Player-facing ability details remain in the Origin/power descriptions in game.

### Veilborn current mechanics

Standard Veilborn now use the following drawback values:

- **18 HP / 9 hearts** (`-2 Max HP`).
- Armor-weight thresholds apply **-10% movement and swim speed each** instead of -15%.
- Wet/Unstable reduces melee and projectile damage dealt by **60%** instead of 80%; its Silence and lingering-duration rules are unchanged.
- **Veil Step** replaces the old Curtain Step pearl: enter the Veil for up to **5 seconds**, move freely in 3D at approximately **15 blocks/sec** with no travel-distance cap and no block phasing. Crossing a living entity applies 5 seconds of Reality Shatter; Standard Veilborn also receive 5 seconds on exit.

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

See [CHAMPION_ORIGINS.md](../CHAMPION_ORIGINS.md) for every ID, removed drawback, and Champion-specific safety behavior.

## Optional enhanced Origin FX

Spell Engine and More RPG Library are **suggested, not required**. Andromeda checks for them at runtime and loads its particle bridge only when Spell Engine is present. Existing bundled sounds and vanilla particles are never replaced, so removing the libraries or disabling enhanced FX returns the mod to its normal presentation.

Enhanced FX definitions are data-driven under `data/andromeda_origins/andromeda_fx/` and currently cover 109 enhanced events across all 13 Origins. Operators can compare the two presentation modes without restarting:

```mcfunction
/andromedaorigins enhanced_fx
/andromedaorigins enhanced_fx enabled false
/andromedaorigins enhanced_fx particles false
/andromedaorigins enhanced_fx sounds false
```

The switches persist in `config/andromeda_origins_fx.json`. See [ENHANCED_FX.md](../ENHANCED_FX.md) for the event/asset mapping and architecture.

## Origin selection and Alternate Origin GUI

Andromeda contributes its 13 Standard + 13 Champion IDs to the normal `origins:origin` layer, then filters the completed Overgrown layer at runtime using the synced Fzzy gameplay config. Stock Human, Merling, Arachnid, Avian, Blazeborn, Buzzborne, Elytrian, Enderian, Feline, Phantom, Shulk, and Slimekin entries default to disabled regardless of datapack/resource ordering, but can be re-enabled from **Overgrown Defaults** in the config. Champion IDs remain available for operator assignment but stay hidden from ordinary selection because their Origin definitions are unchoosable.

The 1.21.1 public builds of **[Alternate Origin GUI](https://github.com/UltrusBot/AltOriginGui)** by UltrusBot target the older Apace Origins client classes. Overgrown is a separate implementation with different screen/API packages, so installing that jar alongside this branch is not supported. Andromeda includes an independently implemented Overgrown-native paged icon grid inspired by that project's useful layout. No Alternate Origin GUI source code or assets are bundled. The integration retains Overgrown's Origin objects, availability checks, Orb of Origin flow, and network selection logic while replacing the cramped detail reader with Andromeda's wide scrollable reader. Random Origin selection is intentionally disabled.

Do **not** install `altorigingui` 1.1.5 or older with this build; the mod metadata explicitly marks those versions incompatible to prevent a client mixin crash.

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

See [RESOURCE_BARS.md](../RESOURCE_BARS.md) for the full 31-style assignment table and the Nereid shared-Wet-resource exception.

## Custom ability audio

Andromeda Origins currently registers **34 custom SoundEvents** under:

```text
andromeda_origins:ability.*
```

Assets live under:

```text
assets/andromeda_origins/sounds/abilities/
```

Long channel/ambience sounds use explicit local/positional playback and stop handling where needed. See [CUSTOM_SOUNDS.md](../CUSTOM_SOUNDS.md) for the complete current sound list and behavior.

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

See [FIGURA_COMPAT.md](../FIGURA_COMPAT.md) for state names, helper functions, Mortal Resolve support, ExtraBone/PlayerAnimator guidance, and custom-armor behavior.

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

See [COMPATIBILITY.md](../COMPATIBILITY.md) for the implementation notes.

## Admin recovery command

```mcfunction
```

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

## More information

See the [README](../README.md) for installation, [development guide](../DEVELOPMENT.md) for building and publishing, and [release history](../CHANGELOG.md) for changes.

## Food balance

In Fzzy Config's gameplay screen, **Food Balance** offers separate switches for Farmer's Blessing, Grandma's Blessing, and Stoutheart. All three are disabled by default. Disabled effects are blocked when applied and removed from players within one second, including effects saved before joining. These switches disable the effects from every source; other effects keep their normal behavior.

Beef Wellington defaults to **6 nutrition** and **0.5 saturation modifier**, restoring 6 hunger points (three hunger icons) and 6 saturation per serving. Set nutrition from 0–20 and the modifier from 0–2, or switch **Balance Beef Wellington** off to use Candlelight's values. Both held portions and dishes placed through Foundation are covered. Effects, eating time, and containers are preserved. Changes apply live on the server and synced clients.

**Block Golden Food Incapacitated Bypass** prevents only golden apples, enchanted golden apples, and golden carrots from resetting down counters/timers, providing adrenaline recovery, or reviving players. Their normal food effects remain. Origin revival abilities keep working. No stack sizes are changed. Farm & Charm, Brewery, Candlelight, Foundation, and Incapacitated remain optional dependencies.
