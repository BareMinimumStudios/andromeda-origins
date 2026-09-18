# Figura Compatibility

**Current release: v1.4.73.** Figura hook IDs and the existing model contract are unchanged; Champion Veilborn's 5-second Auroral Mirage cooldown does not add or renumber Figura hooks.

Andromeda Origins does **not** require Figura. Figura is an optional integration and the server-side mod does not import Figura classes. The bundled helper is authored for the project’s Minecraft 1.21.1 / Figura 0.1.6-era setup.

## Compatibility architecture

Andromeda keeps two compatibility layers:

1. **Legacy numbered hooks** — preserved for existing avatars.
2. **Semantic Lua helper** — recommended for new avatars.

### Legacy hooks

The following synced resources remain supported:

```text
andromeda_origins:common/figura_1
andromeda_origins:common/figura_2
andromeda_origins:common/figura_3
andromeda_origins:common/figura_4
andromeda_origins:common/figura_5
```

All current writes are explicit boolean states (`set 1` / `set 0`) rather than mixed increment/decrement behavior. They are reset on Origin selection and respawn to reduce stuck animations after death, interrupted abilities, or Origin changes.

## Semantic helper

The bundled helper is stored at:

```text
src/main/resources/assets/andromeda_origins/figura/andromeda_origins.lua
```

For an avatar, copy that file into the avatar's scripts as `andromeda_origins.lua`, then:

```lua
local ao = require("andromeda_origins")
```

Example:

```lua
local ao = require("andromeda_origins")

function events.tick()
   local nbt = ao.snapshot()

   if ao.state("manticore_lunge", nbt) then
      animations.model.lunge:play()
   else
      animations.model.lunge:stop()
   end
end
```

### Helper functions

- `ao.snapshot()` — returns `player:getNbt()`.
- `ao.getOrigin(nbt)` — returns the active Andromeda Origin key when detected.
- `ao.isChampion(nbt)` — checks the shared Champion marker power.
- `ao.hasPower(id, nbt)` — checks for an Apoli power by ID.
- `ao.resource(id, nbt)` — reads a numeric/boolean-like Apoli resource value.
- `ao.hook(index, nbt)` — reads one of the legacy numbered Figura hooks.
- `ao.state(name, nbt)` — preferred semantic state lookup.
- `ao.hasTag(tag, nbt)` — checks player command tags used as fallbacks.
- `ao.extraBoneLoaded()` — safely tests whether `figuraextrabone` is loaded.
- `ao.states()` — returns the semantic-state definition table.

The helper searches both key-based and entry-based Apoli NBT layouts so avatar authors do not need to duplicate the underlying parsing logic.

## Semantic state catalog

| Origin | Semantic states |
|---|---|
| Arachne | `arachne_scurry`, `arachne_web_spit`, `arachne_weaving`, `arachne_leap` |
| Faerie | `faerie_flutter`, `faerie_illusion`, `faerie_air_jump` |
| Fenrkin | `fenrkin_howl`, `fenrkin_hunt`, `fenrkin_stamina_surge`, `fenrkin_underdog`, `fenrkin_pounce` |
| Gorgon | `gorgon_gaze`, `gorgon_transference` |
| Humanity | `human_indomitable`, `human_mortal_resolve`, `human_champion_mortal_resolve` (`humanity_*` aliases also exist) |
| Lichling | `lichling_self_defiance`, `lichling_target_defiance`, `lichling_chimes` |
| Manticore | `manticore_lunge`, `manticore_beast_of_blood`, `manticore_wet_shake` |
| Nereid | `nereid_aura_channel`, `nereid_aura_finish`, `nereid_submersion` |
| Satyr | `satyr_max_momentum`, `satyr_swift_leap`, `satyr_stomp`, `satyr_rush`, `satyr_vigil` |
| Selkie | `selkie_coastal_phalanx_cast`, `selkie_coastal_phalanx_hit`, `selkie_surging_tides`, `selkie_sealskin_bastion` |
| Siren | `siren_infatuation_cast`, `siren_infatuation_hit`, `siren_wail` |
| Veilborn | `veilborn_transposition`, `veilborn_auroral_mirage`, `veilborn_wet_shake` |
| Wyverian | `wyverian_fire_breath`, `wyverian_fireball_charge`, `wyverian_fireball_release`, `wyverian_wing_flap`, `wyverian_hover` |

### Mortal Resolve

Mortal Resolve uses a dedicated synced resource:

```text
andromeda_origins:humanity/figura_mortal_resolve
```

The older command tags `andromeda_mortal_resolve` and `andromeda_champion_mortal_resolve` remain fallback signals in the helper for compatibility.

## Undetectable rendering

The shared `andromeda_origins:common/undetectable` state now also hides standard held items, prevents vanilla mob targeting, and suppresses Armor Model API custom geo armor through Andromeda's optional armor dispatcher bridge. The state publishes the synchronized command tag:

```text
andromeda_undetectable
```

Avatar scripts can use this as an optional visual signal when they render custom weapons/items as Figura model parts. Andromeda can suppress Minecraft's normal held-item render paths, but it cannot automatically hide arbitrary custom Figura geometry that an avatar author draws independently.

For example, an avatar that manually renders a weapon model may choose to hide it while the tag is present using the helper's `ao.hasTag(...)` function.

## PlayerAnimator / Emotecraft / Figura ExtraBone

Figura ExtraBone is **not** an Andromeda state API and is not a dependency. Its role is skeleton/blending interoperability between Figura and animation systems such as PlayerAnimator/Emotecraft.

For avatars that also implement custom run, idle, bow/crossbow, or emote animations:

- avoid broad `animations:stopAll()` calls for routine Origin-state changes;
- animate only the model parts the ability actually needs;
- prefer Figura priority/blending instead of force-resetting every limb every tick;
- keep Origin-state detection separate from pose/skeleton blending;
- use `ao.extraBoneLoaded()` only to opt into ExtraBone-specific behavior when your avatar actually supports its model structure.

Installing ExtraBone alone does not automatically make an arbitrary Figura avatar use segmented/PlayerAnimator-compatible bones; the avatar still has to be authored to take advantage of them.

## Armor Model API custom armor

Andromeda Origins v1.4.44 includes an optional client-side bridge for Armor Model API. Armor Model API's custom geo armor does not use Figura's normal vanilla armor render path, so without the bridge an avatar can hide vanilla armor while the custom geo model remains visible. The bridge is authored against **Armor Model API 1.1.0** and **Figura 0.1.6**; Rogues & Warriors 3.1.1 was used as the concrete 1.21.1 content-mod test case.

Avatar authors do not need a new API. Continue using Figura's normal visibility controls:

```lua
vanilla_model.ARMOR:setVisible(false)
```

Or hide an individual slot:

```lua
vanilla_model.HELMET:setVisible(false)
vanilla_model.CHESTPLATE:setVisible(false)
vanilla_model.LEGGINGS:setVisible(false)
vanilla_model.BOOTS:setVisible(false)
```

When Armor Model API is installed, Andromeda Origins checks the same Figura slot state before that custom armor renderer draws. The bridge:

- is client-only;
- does not add a hard Figura or Armor Model API dependency;
- respects Figura's `VANILLA_MODEL_EDIT` permission;
- is generic to Armor Model API rather than hard-coded to Rogues & Warriors item IDs;
- fails open to normal armor rendering if the optional integration cannot be resolved.

This is independent of Andromeda's semantic Origin states and legacy `figura_1`–`figura_5` hooks.

## Backward compatibility

The numbered hook IDs are intentionally retained. Existing Andromeda avatars do not need to migrate immediately. New work should prefer semantic state names so future compatibility changes can remain isolated inside the helper.
