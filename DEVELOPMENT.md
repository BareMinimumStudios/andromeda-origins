# Development

Minecraft 1.21.1 / Java 21 / Kotlin 2.2.21 / Cloche 0.18.10.

## Build

```sh
./gradlew clean build
```

The loader jars appear in `build/libs` with `-fabric.jar` and `-neoforge.jar` suffixes. Shared Kotlin and resources live in `src/main`; loader event, networking, and startup bridges live in `src/fabric` and `src/neoforge`. `verifyAndromedaResources` checks JSON, Origin references, sound files, and Manticore ownership invariants as part of every build.

The Gradle wrapper is authoritative. MixinMCP's decompilation plugin is included before development tasks so its linked IDE tools can inspect the actual resolved Minecraft and dependency classes.

## Test

Use a separate disposable world and the correct dependency set for each loader. Verify server startup, `/reload`, Origin assignment, configuration synchronization, restart/reconnect during Mortal Resolve, two-caster Manticore lunges, and the selection/reader badge hovers on a matching client. Dedicated-server startup cannot validate client rendering or every multiplayer interaction.

## Publish

The pinned MC Publish workflow builds once and stages one verified jar per loader. It supports separate Fabric/NeoForge destinations on Modrinth and CurseForge, plus GitHub, for retrying a partial release. Select only the failed destination when recovering a partial upload.

Set `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` as repository or organization Actions secrets. The project IDs are Modrinth `8rLKf3xm` and CurseForge `1778081`. Each upload job validates its own secret without printing it.

Modrinth and CurseForge version numbers and display titles use `<mod_version>+<minecraft_version>-<loader>`, for example `1.4.76+1.21.1-fabric`. The workflow validates Modrinth's 32-character limit before building.

Tag releases as `v<mod_version>+1.21.1` after adding a dated version section to `CHANGELOG.md`. Manual publication supports the same destination choices as the other studio mods. No token is stored in the repository.

The root README is the GitHub description. Store descriptions live in `docs/MODRINTH.md` and `docs/CURSEFORGE.md`. Their banner uses the public GitHub URL: push the matching branch and image before copying either description to a store.

## Source packaging

Distribute source ZIPs with the project files directly at the ZIP root. Exclude `.git`, `.idea`, `.gradle`, `build`, `run`, and local test worlds.

`python tools/package_source.py` creates a flat ZIP and checksums under `build/release`. The publishing workflow includes these with the GitHub release; the store uploads contain only the matching loader jar.

Static-only Kotlin Mixin objects are stripped of singleton scaffolding after compilation. The verification task checks the compiled callback bytecode to prevent target constructors from receiving Kotlin singleton initialization. Helpers live outside the reserved Mixin package.

Overgrown Origins and Opoli are installed from Modrinth on both loaders. The CurseForge listing is [Andromeda Origins](https://www.curseforge.com/minecraft/mc-mods/andromeda-origins). Publishing declares the correct Overgrown projects and optional integration relationships; Additional Entity Attributes remains embedded. If a store has not mirrored the required Origins/Opoli versions yet, install the versions required by the loader metadata from Modrinth. Install Kotlin for Forge using its complete `-all.jar` distribution, not the small Maven aggregate jar.

Run `python tools/validate_resources.py` for duplicate JSON keys, schema regressions, native Mixin source coverage, retained Selkie data, and retired-helper checks. CI runs this validation before building.


## Returning-player repair

`/andromedaorigins repair <player>` (alias `repair_attributes`) requires operator level 2. It removes stale Andromeda modifiers, rebuilds currently granted attributes, restores missing selected Andromeda powers, and resets the eight vanilla attribute bases managed by the legacy repair: health, speed, scale, step height, armor, toughness, knockback resistance, and attack speed. Foreign modifier IDs remain intact. Because other mods may intentionally change these base values, use the manual command only for players who need a reset.

Login/respawn cleanup runs after one second and does not reset base values. Medieval sources, helper powers, modifiers, and `siren_seduce` are cleaned only when the player has a resolved selection and no active Medieval Origin on any layer. A Pehkui eye-height reset requires evidence of old Medieval Pixie state and an installed Pehkui command. This does not convert old Cardinal Components selections into Overgrown selections; unavailable selections skip cleanup safely. Rebuilds preserve current HP, existing resources/cooldowns, and suppressed powers.
