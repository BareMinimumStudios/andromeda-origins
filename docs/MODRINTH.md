![Andromeda Origins — thirteen species, from sea to sky](https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/docs/assets/andromeda-banner.png)

# Andromeda Origins

**Minecraft 1.21.1 · Fabric & NeoForge**

Climb walls as an Arachne, stalk prey as a Fenrkin, or slip through the Veil. Built for Andromeda MC, these **13 Origins** change how you move, fight, eat, and survive. Their advantages come with weaknesses that give each species its own place in the world.

Read your abilities in the in-game Origin reader, track cooldowns and resources on custom HUD bars, and tune individual powers through server-controlled settings. **13 Champion variants** give operators an additional set of hidden Origins to assign.

[Download on Modrinth](https://modrinth.com/mod/andromeda-origins) · [Community](https://discord.gg/pcRw79hwey)

## Origin roster

| Origin | Playstyle |
| --- | --- |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/arachne.png" alt="" width="32" height="32" align="absmiddle"> Arachne | Webs, climbing, venom, and swift movement |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/faerie.png" alt="" width="32" height="32" align="absmiddle"> Faerie | Small fae mobility, illusions, and concealment |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/fenrkin.png" alt="" width="32" height="32" align="absmiddle"> Fenrkin | Predator tracking, pursuit, and stamina |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/gorgon_eye_2.png" alt="" width="32" height="32" align="absmiddle"> Gorgon | Petrification and speed transference |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/humanity.png" alt="" width="32" height="32" align="absmiddle"> Humanity | Determination and a powerful final stand |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/lichling.png" alt="" width="32" height="32" align="absmiddle"> Lichling | Undead sustain, revival, and withering magic |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/manticore.png" alt="" width="32" height="32" align="absmiddle"> Manticore | Pouncing melee predator and blood-fueled combat |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/nereid.png" alt="" width="32" height="32" align="absmiddle"> Nereid | Aquatic healing and support |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/satyr.png" alt="" width="32" height="32" align="absmiddle"> Satyr | Momentum, leaps, and wind movement |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/selkie.png" alt="" width="32" height="32" align="absmiddle"> Selkie | Aquatic mobility, hydration, and protection |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/siren.png" alt="" width="32" height="32" align="absmiddle"> Siren | Sonic control, pacification, and Silence |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/veilborn.png" alt="" width="32" height="32" align="absmiddle"> Veilborn | Veil movement, transposition, and concealment |
| <img src="https://raw.githubusercontent.com/BareMinimumStudios/andromeda-origins/overgrown-1.21.1/src/main/resources/assets/andromeda_origins/textures/item/origin_icon/wyverian.png" alt="" width="32" height="32" align="absmiddle"> Wyverian | Ember flames and aerial mobility |

## Installation

For **Minecraft 1.21.1 / Java 21**, install the same Andromeda version on the client and server. Choose the download for your loader and install the matching dependencies:

| Fabric | NeoForge |
| --- | --- |
| [Overgrown's Origins](https://modrinth.com/mod/overgrowns-origins/versions) | [Overgrown's Origins](https://modrinth.com/mod/overgrowns-origins/versions) |
| [Opoli](https://modrinth.com/mod/opoli/versions) | [Opoli](https://modrinth.com/mod/opoli/versions) |
| [Fzzy Config](https://modrinth.com/mod/fzzy-config/versions) | [Fzzy Config](https://modrinth.com/mod/fzzy-config/versions) |
| [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin/versions) | [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge/versions) (complete distribution) |
| [Fabric API](https://modrinth.com/mod/fabric-api/versions) | — |

Additional Entity Attributes is bundled with Andromeda.

Use the **Overgrown fork** of Origins and Opoli. Apace's original Origins/Apoli releases use a different API. Existing legacy player data is not automatically imported; back up your world before changing frameworks.

## Configuration and Champions

Open the gameplay screen through Mod Menu when available, or use `/andromedaorigins config` with operator permission level 2. Settings are stored in `config/andromeda_origins/gameplay.toml` and apply live. Power switches preserve their stored cooldown and resource state.

The **Food Balance** section controls Farmer's Blessing, Grandma's Blessing, and Stoutheart, plus Beef Wellington nutrition and saturation. The default Wellington serving restores 6 hunger points and 6 saturation, whether eaten from an item or a placed dish. A separate switch blocks golden apples, enchanted golden apples, and golden carrots from bypassing Incapacitated. These settings apply live; the integrations are optional.

Champion variants are hidden from normal selection. Operators can assign one with:

```mcfunction
/origin set <player> origins:origin andromeda_origins:champion_fenrkin
```

## Optional integrations

Figura hooks support custom avatars; Incapacitated connects downed and revival mechanics. Spell Engine and More RPG Library provide additional ability effects. Dietary and iron tags cover compatible modded foods and equipment. Availability depends on your loader and the installed mods.

Andromeda's code and artwork use The Bare Minimum License v1.0. Bundled third-party components retain their own licenses.
