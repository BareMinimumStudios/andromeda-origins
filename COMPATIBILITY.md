# Andromeda Origins — Overgrown compatibility

This document describes **`1.4.75-overgrown.37`** for Minecraft 1.21.1.


The Andromeda Origin chooser and O-key self-view use a centered flat neutral UI while retaining Overgrown's underlying selection, layer, swap, and networking behavior.
## Runtime

- Overgrown Origins **1.47.10**
- Opoli **1.117.0**
- Fabric Loader **0.19.3+** or NeoForge **21.1.248+**
- Fabric: Fabric API **0.116.15+1.21.1** and Fabric Language Kotlin **1.13.7+kotlin.2.2.21**
- NeoForge: complete Kotlin for Forge **5.11.0** distribution
- Java **21**
- Official **Mojang mappings** in the source project
- Additional Entity Attributes **2.0.2+1.21.1** bundled for stable swim-speed parity
- Fzzy Config **0.7.7+1.21** required for categorized gameplay configuration, validation, permissions, and sync


## Current upstream integration notes

- The exact released Opoli 1.117.0 / Origins 1.47.10 Modrinth artifacts are pinned by immutable version ID; no bundled upstream source projects are required.
- `origins:slimekin` is included in Andromeda's stock-Origin policy and remains disabled by default with the rest of the stock roster.
- Diet tooltip rendering uses Opoli’s native food-tooltip insertion API; the retired custom adapter has been removed.
- Champion cooldown recovery uses `apoli:modify_resource_change` with 2x cooldown drain instead of a hard-coded polling list.
- Every Standard and Champion Origin now has native Origin tags for future category-based conditions.
- If `saves/<world>/origins/config.json` disables Andromeda Origins/powers, Andromeda logs a warning on server start and after successful datapack reloads because those registry-level disables happen before Fzzy Config suppression.

## Origin roster / server configuration

Andromeda contributes its 13 standard + 13 Champion IDs to the primary `origins:origin` layer with normal merge semantics, then filters the completed layer at runtime. This remains independent of datapack/resource ordering: stock Overgrown IDs can exist in the merged layer while Andromeda's authoritative policy decides whether they are actually available.

`.18` moves configuration persistence, validation, GUI generation, permissions, and synchronization to **Fzzy Config 0.7.7+1.21**. The root config is `andromeda_origins:gameplay`, stored server-side/local-singleplayer at `config/andromeda_origins/gameplay.toml`. The old `.17` `config/andromeda_origins.json` is no longer used.

The Fzzy UI contains nested **Standard Origins**, **Champion Origins**, and **Overgrown Defaults** sections. Each of the 26 Andromeda Origin sections has its own availability switch and its own audited gameplay-power switches. Stock Overgrown Origins are disabled by default behind a master switch and can then be controlled individually.

The same runtime Origin filter is used by the selection GUI, layer queries, and a server-side `OriginManager.availableTo` backstop. Random Origin selection is disabled. Existing online players holding an Origin that becomes disabled are cleared from that layer and returned to selection. Third-party Origin namespaces are not globally blacklisted.

Power/weakness switches use Opoli suppression with the dedicated source `andromeda_origins:config_disabled`. Suppression preserves ownership and stored resource/cooldown data. Power settings are **per Origin**, allowing shared IDs such as diets or Iron Weakness to be independently configured for each Origin that grants them. Iron Weakness still controls its linked Wither helpers as one conceptual switch. Hidden implementation helpers, Figura bookkeeping, and Champion cooldown infrastructure are excluded. Aquatic/Champion gameplay identity now uses native Origin tags; the hidden Champion marker remains only for backward-compatible Figura scripts.

Fzzy Config is registered as `RegisterType.BOTH` with permission level 2 and `SaveType.SEPARATE`. Accepted server changes are applied immediately to online players; normal config edits do not require a restart. `/reload` is only needed when datapack Origin data itself also changes. Mod Menu is optional, but when installed Fzzy Config supplies Andromeda's Config button automatically. `/andromedaorigins config` remains a convenience opener for the same screen.

## Origin-selection GUI

The current branch includes an Overgrown-native paged icon grid inspired by Alternate Origin GUI. It targets `dev.overgrown.origins.client.screen.ChooseOriginScreen` directly, preserves Overgrown's Origin availability/Orb/selection networking, disables Random Origin selection, and overlays the stock narrow detail pane with Andromeda's wider scrollable rich-text reader.

The public 1.21.1 Alternate Origin GUI builds target Apace Origins classes under `io.github.apace100.origins.*`, not Overgrown's client API. Known builds through 1.1.5 are therefore marked incompatible in `fabric.mod.json`; they should not be installed alongside this branch.

## Player data

The Overgrown build does **not** include an automatic importer, join-time Origin integrity migrator, or general old-player repair command for Apace Origins/Apoli data. If an existing production player carries incompatible old Origin/Apoli state, reset that player's affected Origin/Opoli data and have them reselect their Origin. Fresh Overgrown selection is authoritative.

## Kept compatibility systems

- **Incapacitated:** optional runtime integration used by Humanity/Lichling mechanics. Mortal Resolve retains only the state handling needed for its active final-death flow.
- **Figura:** optional numbered boolean hooks plus the bundled semantic Lua helper.
- **Armor Model API:** optional rendering bridge for Figura armor visibility.
- **Spell Engine / More RPG Library:** optional enhanced audiovisual presentation; base Origin gameplay does not depend on them.

## Overgrown-native replacements

The old Apace-only compatibility power registry is not carried forward. The port uses current Opoli/Overgrown APIs and typed actions/resources where possible, while Andromeda still owns mechanics that have no upstream equivalent and are part of the Origin design, including Veil Step, Mortal Resolve integration, Arachne crafting safety, version handshake, custom sounds/FX, targeted gameplay mixins, the Origin-selection grid, and the `.18` Fzzy Config-backed server-authoritative Origin/power configuration layer.
