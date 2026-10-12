# .37 validation — 2026-10-10

## 1.4.77 ability audit — 2026-10-11

Reproduced a downed Champion Lichling failing to self-revive when sneaking: the primary took its target-revival branch. With incapacitated state taking priority, both standard and Champion Lichling revive to full health in either sneak state.

Incapacitated 2.0.1 was tested on Fabric and NeoForge in disposable dedicated-server worlds. Tests used simulated server players, with outgoing packets suppressed for the NeoForge diagnostic; they do not validate client packet handling or visuals. Checks covered healthy-player no-op behavior, synchronous revival inside a nested command, Champion Humanity recovery, 178 activation cases per loader across all 26 Origins, and 120 manual passive ticks per Origin. These are execution smoke checks, not assertions of every ability's gameplay outcome, target selection, or multiplayer balance.

Resource validation checks all 194 root powers, 26 Origin definitions, sound files, and namespaced wildcard power references. The Lichling revival branch has a regression check. Diagnostic classes are excluded from production source and release jars. Both production jars passed dedicated-server startup and reload checks; Fabric was also checked with Incapacitated absent.

The complete `.36` source was imported into the `overgrown-1.21.1` branch, preserving Git history. The original checkout and downloaded source archive were retained as recovery copies. Synced ChatGPT project references were not modified.

## Verified

| Check | Result |
| --- | --- |
| Clean Cloche build, Java 21 / Kotlin 2.2.21 | Fabric and NeoForge artifacts pass |
| JSON and resource checks | 318 JSON files, 194 root powers, 26 Origins, 34 registered sound files |
| Kotlin conversion | No Java sources remain; loader-specific event/networking bridges compile |
| Static Mixin bytecode | Nine compiled classes across common/Fabric/NeoForge contain static callbacks without singleton fields or initializers |
| Selkie preservation | SHA-256 checks match all 21 original Selkie resource files |
| Fabric dedicated server | Startup, command dispatch, resource reload, and graceful shutdown pass |
| NeoForge dedicated server | Startup, command dispatch, resource reload, and graceful shutdown pass |
| Native helper creation | Web nest, fireball, and dragon charge return success on both servers |
| Fabric version negotiation | Missing and mismatched clients rejected; matching version passes login negotiation |
| Configuration persistence | Both test servers generate the categorized gameplay TOML configuration |
| Publishing configuration | Both workflow YAML files parse; release staging validates loader metadata, version, and dated changelog |
| MixinMCP | Build plugin installed, project sync succeeds, updated dependency APIs and Origin screen injection targets inspected |

The dedicated servers used isolated child folders under the supplied Fabric Server directory. Their disposable worlds and loopback ports were separate from the supplied server's world and production configuration. Tests used Origins 1.47.7 and Opoli 1.115.0 on both loaders, Fzzy Config 0.7.7, bundled Additional Entity Attributes 2.0.2+1.21.1, and NeoForge 21.1.248. NeoForge uses the complete Kotlin for Forge 5.11.0 distribution.

The final servers load 1,131 powers including upstream and generated subpowers, 39 registered Origins including upstream entries, 47 inline badges, three power-gated recipes, and 112 Enhanced FX definitions. No Andromeda power parsing failures or obsolete-field warnings remain in the final server logs.

## Findings fixed during verification

- NeoForge items and sounds now register during its registry events instead of in the mod constructor.
- New helper entities explicitly initialize their Opoli power attachment before receiving powers.
- Seven current-schema corrections across six power files replace obsolete feed fields and provide Manticore's required damage condition.
- Modified resources use UTF-8, preserving Minecraft formatting characters.
- Optional reflection handles missing methods safely and selects the compatible Figura entity overload.
- Mortal Resolve stores its deadline in Origin storage and clears expired/dead state. Manticore victims retain the actual caster's identity.

Fabric logs three upstream messages about missing data fixers for Opoli's custom projectile, minion, and clone entities. An isolated control server without Andromeda reproduces them. The provided Fabric server libraries also log OSHI/JNA hardware-probe warnings during startup; these do not prevent startup. An empty flat-world preset produces a one-time vanilla `No key layers in MapLike[{}]` message on fresh test worlds. These are recorded separately from Andromeda failures.

## Remaining validation limits

Missing badge sprites use an independently drawn Andromeda glyph; copied upstream fallback textures have been removed. Native badges still use the artwork supplied by Origins.

The tooltip fix replaces the recipe preview's background texture with flat slots and vanilla item rendering, and resets rendering state before badge tooltips. Compilation and dependency inspection pass. No game window was opened, so the final in-game appearance still needs visual confirmation.

Dedicated-server startup cannot prove every ability interaction. Matching-client tests for Origin selection, Fzzy synchronization, two-caster Manticore lunges, Mortal Resolve death/reconnect/restart, and optional Figura/Incapacitated integrations remain manual release checks. NeoForge's required versioned payload negotiation compiles and the server starts; a full matching-client configuration handshake was not exercised.

MC Publish is configured, but no release has been published. Set the Actions tokens and `CURSEFORGE_PROJECT_ID` repository variable before publishing to both stores. The required Origins/Opoli downloads are linked from the store descriptions; unverified CurseForge aliases are explicitly ignored.

## Reproduce

```sh
python tools/validate_resources.py
./gradlew clean build
```

Use a disposable dedicated server with the listed dependencies for runtime checks. `DEVELOPMENT.md` lists the remaining matching-client scenarios. The build's verification tasks check packaged resources and static Mixin bytecode automatically; CI also runs the independent JSON/schema/Selkie regression validator.

## Golden food compatibility (1.4.77)

The server-authoritative Fzzy setting `blockIncapacitatedFoodBypass` defaults to true and applies immediately. It blocks only golden apples, enchanted golden apples, and golden carrots inside Incapacitated's food hook. It prevents food-based revival, adrenaline recovery, and down-counter/timer resets without cancelling vanilla consumption or food effects. Other foods and Origin revival abilities keep their existing behavior. Incapacitated remains optional.

Dedicated Fabric and NeoForge tests against Incapacitated 2.0.1 invoked its real food hook with simulated players. All three foods preserved the downed state, counter, and timer with blocking enabled; disabling the setting restored upstream behavior. A non-golden food temporarily added to the upstream revival tag still revived successfully with blocking enabled. Standard and Champion Lichling self-revival passed while standing and sneaking, and Champion Humanity revival passed. Temporary diagnostic code is excluded from release builds. These server tests do not replace matching-client gameplay testing.

Both production builds also reached server-ready, passed helper/reload checks, and shut down normally without Incapacitated installed. Mixin logs an expected missing optional target warning in that case; the generic smoke-log scanner reports it as an exception string even though startup succeeds.
