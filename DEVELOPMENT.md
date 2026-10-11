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

The pinned MC Publish workflow builds once and stages one verified jar per loader. It supports separate Fabric/NeoForge Modrinth and GitHub destinations for retrying a partial release. CurseForge publishing is currently disabled because there is no Minecraft 1.21.1 listing.

Set `MODRINTH_TOKEN` as an Actions secret. The Modrinth project ID is `8rLKf3xm`. No CurseForge token or project ID is needed for the current workflow.

Tag releases as `v<mod_version>+1.21.1` after adding a dated version section to `CHANGELOG.md`. Manual publication supports the same destination choices as the other studio mods. No token is stored in the repository.

The root README is the GitHub description. Store descriptions live in `docs/MODRINTH.md` and `docs/CURSEFORGE.md`. Their banner uses the public GitHub URL: push the matching branch and image before copying either description to a store.

## Source packaging

Distribute source ZIPs with the project files directly at the ZIP root. Exclude `.git`, `.idea`, `.gradle`, `build`, `run`, and local test worlds.

`python tools/package_source.py` creates a flat ZIP and checksums under `build/release`. The publishing workflow includes these with the GitHub release; the store uploads contain only the matching loader jar.

Static-only Kotlin Mixin objects are stripped of singleton scaffolding after compilation. The verification task checks the compiled callback bytecode to prevent target constructors from receiving Kotlin singleton initialization. Helpers live outside the reserved Mixin package.

Overgrown Origins and Opoli are installed from Modrinth on both loaders. The CurseForge description is retained as a draft for a future listing. Install Kotlin for Forge using its complete `-all.jar` distribution, not the small Maven aggregate jar.

Run `python tools/validate_resources.py` for duplicate JSON keys, schema regressions, native Mixin source coverage, retained Selkie data, and retired-helper checks. CI runs this validation before building.
