# Upstream dependencies for 1.4.80

The build pins immutable Modrinth files for Minecraft 1.21.1 so a release cannot silently resolve a different dependency.

| Dependency | Fabric file | NeoForge file |
| --- | --- | --- |
| Overgrown Origins 1.47.10 | `3wWbYKg8` | `UaDSmaII` |
| Opoli 1.117.0 | `GxdR9K9P` | `M8EO0W8a` |
| Additional Entity Attributes 2.0.2+1.21.1, bundled | `wdj5W9YU` | `6oJLT6m7` |

The [Origins changelog](https://modrinth.com/mod/overgrowns-origins/version/3wWbYKg8) moves conditional swap-pool checks to the server and fixes shared-key conflicts and return/wrap navigation settings.

The [Opoli changelog](https://modrinth.com/mod/opoli/version/GxdR9K9P) changes the network protocol, fixes actual-velocity limits, accessory-slot updates and result-stack actions, and improves custom night vision with BadOptimizations. Update client and server Origins and Opoli together. The existing Iris brightness bridge remains necessary for shader integration.

Andromeda does not use `creative_flight`, whose implicit fall immunity was removed upstream, or direct `modify_velocity` powers. Its existing `modify_falling`, elytra flight and explicit fall-immunity powers retain their definitions. No new entity-set, projectile, HUD or key-release gameplay features are enabled in this update.

Both loader builds share gameplay data. NeoForge's upstream artifacts require NeoForge 21.1.248 or newer. Kotlin for Forge must be installed using its complete distribution; the Maven aggregate alone is insufficient for a standalone server.
