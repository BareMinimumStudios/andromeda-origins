# Upstream updates used by .37

The build pins immutable Modrinth files for Minecraft 1.21.1 so a release cannot silently resolve a different dependency.

| Dependency | Fabric file | NeoForge file |
| --- | --- | --- |
| Overgrown Origins 1.47.7 | `54ugI640` | `1FOYvYNr` |
| Opoli 1.115.0 | `HiH9ptk7` | `LiiBuizh` |
| Additional Entity Attributes 2.0.2+1.21.1, bundled | `wdj5W9YU` | `6oJLT6m7` |

The [Origins changelog](https://modrinth.com/mod/overgrowns-origins/version/54ugI640) adds native labels for restricted foods and makes the Flower Patch compatibility tag optional. Andromeda keeps its existing dietary restrictions and uses native tooltip rendering rather than an adapter to the retired API.

The [Opoli changelog](https://modrinth.com/mod/opoli/version/HiH9ptk7) describes food-tooltip insertion, broader kill callbacks, and improvements to model transformations and first-person rendering. The latest release adds path targets for reaching model parts and fixes arm, held-item, and hand-particle alignment. These provide future animation options; .37 does not replace existing Figura animations or redesign abilities around them.

The current resolved APIs were inspected with MixinMCP. Runtime tests also identified required schema corrections: feed actions use `food`, and invulnerability powers require a `damage_condition`. Existing nutrition amounts and Manticore immunity intent are retained.

Both loader builds share gameplay data. NeoForge's upstream artifacts require NeoForge 21.1.248 or newer. Kotlin for Forge must be installed using its complete distribution; the Maven aggregate alone is insufficient for a standalone server.
