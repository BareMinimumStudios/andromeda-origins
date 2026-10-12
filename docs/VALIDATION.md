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

## Let's Do food balance (1.4.77)

Dedicated Fabric and NeoForge servers used Farm & Charm 1.1.27, Brewery 2.1.12, Candlelight 2.1.14, and bundled Foundation 1.0.3. Each loader passed 11 native-backend checks: unchanged golden-food stacking/nutrition, individual blessing toggles through both effect application overloads, removal of existing blessings while preserving other effects, held Wellington nutrition/saturation and original effects/container/eating properties, live value changes and restoring upstream values, placed Wellington on/off, Fzzy snapshot conversion, and exception-safe placed-food context cleanup. The prior seven golden-food bypass checks, Lichling/Humanity revival checks, and the ability execution smoke audit also passed. These tests used simulated server players and actual upstream implementations; no game window or matching-client GUI test was run.

Settings are server-authoritative and sync through the existing Fzzy lifecycle. Existing per-stack custom components are preserved. The item food overlay is cached and refreshed after policy or base-food changes; it does not mutate item defaults or saved stacks. Stack-size overrides and drink/wine changes are outside this port.

Production builds also passed startup, helper commands, reload, and normal shutdown on both loaders with Farm & Charm, Brewery, Candlelight, Foundation, and Incapacitated absent. Expected optional Mixin target warnings were recorded. A Fabric control server without Andromeda reproduced the eight Let's Do/Foundation missing-data-fixer messages, confirming they originate upstream.

## Gorgon climbing and Selkie hydration (1.4.77)

Both requested values were already present in the current source. The independent resource validator now traverses standard and Champion Gorgon power dependencies and rejects climbing powers. It also checks the native Selkie wetness reserve starts and caps at 300 seconds, updates every 20 ticks, and drains by one only outside water and rain. The existing 21-file Selkie baseline remains unchanged. Champion Selkie retains its existing immunity to dry-out penalties. No wider Selkie rework or Nereid hydration change was made.


## Legacy player repair (1.4.77)

Fabric and NeoForge dedicated-server backend checks passed Humanity's 18 HP value, missing grant restoration, no healing, old Andromeda health modifiers, Medieval height/diet/tag cleanup, foreign modifier preservation, active Medieval selection preservation, repeated repair stability, fail-closed missing selection handling, manual base restoration, operator permissions, and suppressed attribute preservation. All 26 Origins produced stable attribute values across repeated rebuilds; standard Arachne scale was 0.8 and Faerie scale was 0.83, matching current data. Startup, native helper commands, reload, and normal shutdown passed on both loaders. Temporary diagnostic code is excluded from production jars.

These checks simulate server players and stale data. They do not test a real returning player's old Cardinal Components save or a matching client's visual height. The optional Pehkui eye-height command path was not exercised because Pehkui is absent from the test servers.


## Veil Step cloud presentation (1.4.78)

Both dedicated-server backends passed standard and Champion Veilborn checks for observer/viewer rendering policy, suppression, stationary lifetime, and model restoration after timeout. Startup, helper commands, reload, and shutdown also passed. Client dispatcher rendering is cancelled for marked casters, with an optional Armor Model API guard; the server emits the existing cloud every other tick while stationary or moving. Temporary diagnostics are excluded from release jars. A matching two-client visual check remains necessary to verify the final appearance, including custom avatar/rendering mods.


## Night vision and aquatic reports (1.4.79)

The installed Iris 1.8.14 beta uses CommonUniforms.getNightVision() for its shader uniform; the optional client-only bridge merges the active Opoli strength without status effects. Fabric and NeoForge native backend checks verified Gorgon 1.0 and Arachne 0.75 strengths, toggles and suppression, with no Night Vision potion. Shader appearance has not been visually tested.

Native activation verified Selkie dash cooldowns of 600 ticks on dry land and 100 ticks while physically in water on both loaders. Nereid land suffocation is enforced after server player updates with native air decrease hooks and client prediction retained. Both loaders passed full selected-origin hydration expiry, one-air-per-tick drain, zero-air drowning damage, suppression, Water Breathing, Conduit Power, physical water, rain, and Champion exemption. Removing Water Breathing or Conduit immediately resumed draining. Inspection of the installed Hybrid Aquatic 1.7.4 helmet implementation confirms it grants native Water Breathing, which uses the same protection; a live helmet-equipped client was not tested. The original live-server infinite-breathing failure was not reproduced before this hardening.

Submersion checks reproduced and fixed partial-water pull exclusion and player-update air recovery overwriting the previous drain. Both loaders passed real native hit actions, target/caster isolation, partial-water conditions, duration extension/cap, dry-target drowning, expiry cleanup, marked-ally immunity, Nereid ally hydration, and HUD restrictions.

All 26 Origins passed a dedicated-server execution audit on each loader, with 178 activation cases, passive ticking, repeated attribute repair, and removal of the native Origin power source. These simulated-player checks detect loading/execution failures; they do not establish that every ability behaves correctly with real multiplayer targets or client rendering. Matching-client gameplay and shader appearance still need confirmation. Temporary probe code is excluded from production jars.

## Lichling sunlight slowness (1.4.79)

A native Fabric backend reproduced sunlight speed 0.065 persisting at night after the sunweak helper was removed. The sunlight speed power now checks exposure directly, and server tick cleanup purges only its known leftover modifiers/cache when the power is absent, suppressed, or inactive. Both Fabric and NeoForge passed sunlight -> night -> sunlight -> shelter transitions, stale saved modifier removal, foreign speed modifier preservation, Origin removal, and Champion exemption. Startup, helper commands, reload and graceful shutdown passed; temporary diagnostic code is excluded from release jars. These are simulated-player server checks, not client visual tests.
