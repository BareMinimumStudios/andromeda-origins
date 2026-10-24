# Upstream dependencies for 1.4.81

The build pins immutable Modrinth files for Minecraft 1.21.1 so a release cannot silently resolve a different dependency.

| Dependency | Fabric file | NeoForge file |
| --- | --- | --- |
| Overgrown Origins 1.47.12 | `u1Nxa3lt` | `FDYkfFnf` |
| Opoli 1.119.0 | `tESmBbuQ` | `P0ZxlZtm` |
| Additional Entity Attributes 2.0.2+1.21.1, bundled | `wdj5W9YU` | `6oJLT6m7` |

The [Origins changelog](https://modrinth.com/mod/overgrowns-origins/version/u1Nxa3lt) fixes the built-in Enderian pearl spawn height and Avian airborne walking-speed transfer.

The [Opoli changelog](https://modrinth.com/mod/opoli/version/tESmBbuQ) fixes custom-effect grant-source collisions, custom effects after reload, last-stack food actions, and several previously inactive power hooks. Update client and server Origins and Opoli together. The existing Iris brightness bridge remains necessary for shader integration.

Andromeda does not use `creative_flight`, whose implicit fall immunity was removed upstream, or direct `modify_velocity` powers. Its existing `modify_falling`, elytra flight and explicit fall-immunity powers retain their definitions. No new entity-set, projectile, HUD or key-release gameplay features are enabled in this update.

Both loader builds share gameplay data. NeoForge's upstream artifacts require NeoForge 21.1.248 or newer. Kotlin for Forge must be installed using its complete distribution; the Maven aggregate alone is insufficient for a standalone server.

The October 24 pair adds built-in Enderian pearl and Avian airborne-speed fixes. Opoli 1.119.0 fixes last-stack food actions, separates custom-effect power sources as `apoli:effect/<namespace>/<path>`, and preserves custom-effect registry entries across reload. Andromeda has no matching custom-effect source-ID checks or modify_xp_gain definitions requiring migration in this audit. Existing shared restriction recovery and Iris/PAL bridges remain in place. New connected-block, taming, breeding, trading and other gameplay APIs are deferred.

### Airborne transfer in 1.4.81

Opoli's native `attribute_modify_transfer` is not a drop-in replacement for the legacy bridge: two total modifiers +20%/+30% produce 1.56x natively versus the retained 1.50x bucket-summed multiplier. The five existing transfer owners keep their semantics. The bridge now uses the identity of Opoli's dirty-invalidated `ModifierSummary` to cache its own legacy sums per entity. Unchanged attributes avoid repeated modifier scans; missing summary support falls back to recomputation. No shared mutable cache is introduced. Native transfer migration would be a separate movement-balance decision. Connected-block harvesting, breeding/taming, extra trades and HUD redesigns remain deferred.
