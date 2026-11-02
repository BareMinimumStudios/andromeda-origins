# Upstream dependencies for 1.4.83

The build pins immutable Modrinth files for Minecraft 1.21.1 so a release cannot silently resolve a different dependency.

| Dependency | Fabric file | NeoForge file |
| --- | --- | --- |
| Overgrown Origins 1.49.0 | `6thhlmG1` | `FQEvanTQ` |
| Opoli 1.124.0 | `6rLBJtq0` | `oHslKfYl` |
| Additional Entity Attributes 2.0.2+1.21.1, bundled | `wdj5W9YU` | `6oJLT6m7` |

Historical 1.4.81 baseline: the [Origins changelog](https://modrinth.com/mod/overgrowns-origins/version/u1Nxa3lt) fixes the built-in Enderian pearl spawn height and Avian airborne walking-speed transfer.

Historical 1.4.81 baseline: the [Opoli changelog](https://modrinth.com/mod/opoli/version/tESmBbuQ) fixes custom-effect grant-source collisions, custom effects after reload, last-stack food actions, and several previously inactive power hooks. Update client and server Origins and Opoli together. The existing Iris brightness bridge remains necessary for shader integration.

Andromeda does not use `creative_flight`, whose implicit fall immunity was removed upstream, or direct `modify_velocity` powers. Its existing `modify_falling`, elytra flight and explicit fall-immunity powers retain their definitions. No new entity-set, projectile, HUD or key-release gameplay features are enabled in this update.

Both loader builds share gameplay data. NeoForge's upstream artifacts require NeoForge 21.1.265 or newer; Fabric requires Loader 0.20.1 or newer. Kotlin for Forge must be installed using its complete distribution; the Maven aggregate alone is insufficient for a standalone server.

The October 24 pair adds built-in Enderian pearl and Avian airborne-speed fixes. Opoli 1.119.0 fixes last-stack food actions, separates custom-effect power sources as `apoli:effect/<namespace>/<path>`, and preserves custom-effect registry entries across reload. Andromeda has no matching custom-effect source-ID checks or modify_xp_gain definitions requiring migration in this audit. Existing shared restriction recovery and Iris/PAL bridges remain in place. New connected-block, taming, breeding, trading and other gameplay APIs are deferred.

### Airborne transfer in 1.4.81

Opoli's native `attribute_modify_transfer` is not a drop-in replacement for the legacy bridge: two total modifiers +20%/+30% produce 1.56x natively versus the retained 1.50x bucket-summed multiplier. The five existing transfer owners keep their semantics. The bridge now uses the identity of Opoli's dirty-invalidated `ModifierSummary` to cache its own legacy sums per entity. Unchanged attributes avoid repeated modifier scans; missing summary support falls back to recomputation. No shared mutable cache is introduced. Native transfer migration would be a separate movement-balance decision. Connected-block harvesting, breeding/taming, extra trades and HUD redesigns remain deferred.

## November 1 upgrade applied in 1.4.83

The current official Minecraft 1.21.1 releases are [Opoli 1.124.0](https://modrinth.com/mod/opoli/version/6rLBJtq0) (Fabric `6rLBJtq0`, NeoForge `oHslKfYl`) and [Origins 1.49.0](https://modrinth.com/mod/overgrowns-origins/version/6thhlmG1) (Fabric `6thhlmG1`, NeoForge `FQEvanTQ`). Their released jar metadata requires Fabric Loader 0.20.1+ or NeoForge 21.1.265+. This patch updates both loaders to this pair and Fabric API to 0.116.17+1.21.1. Update matching client/server dependencies together.

The strongest benefits are lower allocation/CPU costs for native attribute powers and faster power/layer/badge reloads. HUD bars and badge icons gain fallback textures; data may use JSONC/JSON5. Keep this repository's strict JSON and validation rather than converting files just to use the new syntax.

Before upgrading, audit pack overrides for the now-effective loading_priority behavior on Origins/global power sets. Apoli removes sneaky/share_effects and moves voice/speech types to Birdsong. None of Andromeda's own power definitions use those removed/moved types, so Birdsong is not required for its existing abilities. Ordinary voice-chat use is independent of Apoli's speech powers.

Retain the airborne transfer bridge: native and legacy total-modifier stacking differ, as documented above. New spawn offsets alone do not establish a replacement for exact raycast hit-position commands. Avoid deleting these bridges or helper logic without matching movement/impact tests.


## Champion recovery targeting in 1.4.83

The shared recovery root now contains explicit native `modify_resource_change` filters for player-facing ability cooldown IDs. New ability cooldowns must be deliberately added to `powers/common/champion_cooldowns.json`; internal cadence/resource-consumption timers should remain excluded. Do not restore an unfiltered modifier: it accelerates charged channels, pulse costs and timers granted by other mods or incoming effects. Plain resources and Java world-time deadlines are separate from cooldown recovery.
