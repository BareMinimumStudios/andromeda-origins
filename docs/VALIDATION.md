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

## Lichling sunlight slowness (1.4.80)

A native Fabric backend reproduced sunlight speed 0.065 persisting at night after the sunweak helper was removed. The sunlight speed power now checks exposure directly, and server tick cleanup purges only its known leftover modifiers/cache when the power is absent, suppressed, or inactive. Both Fabric and NeoForge passed sunlight -> night -> sunlight -> shelter transitions, stale saved modifier removal, foreign speed modifier preservation, Origin removal, and Champion exemption. Startup, helper commands, reload and graceful shutdown passed; temporary diagnostic code is excluded from release jars. These are simulated-player server checks, not client visual tests.

## Nereid orphan Wet grant (1.4.80)

The supplied live diagnostic shows gills enabled, Wet granted by minecraft:debuff, and an empty Nereid hydration reserve. Both loader backends reproduced a Wet grant without a hydration helper. Dry-gills evaluation now removes only Andromeda hydration's debuff-sourced Wet root/children when neither Nereid nor Selkie hydration helper has an active positive reserve. Tests passed orphan cleanup, actual air drain and damage, valid reserve preservation, expired reserve cleanup, foreign Wet-source preservation, Water Breathing and Conduit immunity/resumption. The diagnostic computes the reconciled state before formatting its report. The original lifecycle that left this particular live player's orphan grant is not yet established. Temporary diagnostics are excluded from production builds.

## Arachne and Faerie reports (1.4.80)

Both Arachne climbing definitions had an inverted sneaking hold condition. Fabric and NeoForge native checks passed climb while standing, hold while crouching, suppression, and toggle-off behavior for standard and Champion variants. Webbed referred to two nonexistent shorthand textures; both white-vignette PNG frames are now included with complete resource paths. All Andromeda overlay references are independently validated, and PNG center alpha is zero. The issue screenshot shows a full-screen missing texture, consistent with those absent overlays. Matching-client visual confirmation remains necessary.

Opoli's client key handler sends active-power requests without evaluating the grounded power condition. The client now samples ground state before vanilla movement and cancels only the two Faerie air-jump requests originating from a ground-jump press. Input-state checks passed ground press, held takeoff, release and separate airborne press, subsequent landing, and unrelated-power preservation on both loader backends. Both client Mixins compile, but a real client input/network check was not run; no game window was opened. Native ability effects, four charges, landing refill, and existing cooldowns are unchanged. Temporary probe code is excluded from production jars.

## Webbed health report (1.4.80)

The isolated native backend did not reproduce the reported NaN health on application. It did reproduce a zero movement-speed modifier remaining after Webbed expiry for Humanity. Restraint now caps jump strength at zero instead of -1, declares update_health false on its movement modifier, and purges only its own revoked/suppressed movement modifier and cache. Checks on Fabric and NeoForge passed Webbed application and 180 power ticks for Humanity, Faerie, Nereid, Lichling, standard Arachne and Champion Arachne: health/max-health, attributes and velocity remained finite; jump cap was zero; speed recovered after expiry; foreign modifiers were preserved. Pull basis checks also returned finite values for overlapping, vertical, near-vertical and horizontal offsets. These simulated checks do not establish the cause of the live NaN report or test all pack interactions/client rendering. `/andromedaorigins diagnose_web <player>` reports server health, absorption, speed, position/velocity, helper grants/sources and remaining timer without changing them. Existing NaN health is not reset or healed by this change.

## Shared attribute removal review (1.4.80)

Native AttributePower removal callbacks can see the final grant still present and skip removing their modifier. An Andromeda-only post-removal hook now purges its runtime cache and exact indexed modifiers after the grant is gone, including saved modifiers whose cache is not populated. Checks on both loaders passed Gorgon speed boost, petrification attack speed, Lichling chime slowdown and restraint removal; a remaining second source preserved the effect, repeated applications removed cleanly, saved cacheless modifiers were removed, and removal did not heal. All 26 Origins again passed 178 activation cases and passive tick/attribute repair/Origin removal checks per loader. These execution checks do not guarantee every multiplayer/client interaction is correct.

Webbed now uses the requested white edge vignette instead of decorative webs. Both frames have a fully transparent center and partial edge opacity, preserving visibility. PNGs are texture data required by the native overlay renderer, with their references validated in release jars. Final in-game appearance has not been inspected. The live NaN-health report remains unconfirmed.

## Webbed restart report (#53, 1.4.80)

Issue #53 describes being webbed immediately before a restart and remaining immobilized on return; the supplied client log confirms the missing webbed1/webbed2 textures. Webbed now clears on join/death/respawn and manual attribute repair, with tick cleanup for expired/missing timers and debuff-sourced restraint without any current Webbed/petrification/constriction owner. Other restraint sources and active petrification are preserved. No player files or healthy HP are reset.

Both Fabric and NeoForge passed real player NBT save/load roundtrips with Webbed and six remaining seconds, then cleanup of Webbed/restraint without healing; foreign-source and petrification preservation; orphan restraint cleanup; and actual native fall damage on Arachne and Siren, with and without Webbed. Arachne rejected fall damage and stayed at 14 HP; Siren accepted four base fall damage and stayed at finite 15.6/20 HP. This is save/load simulation plus direct cleanup testing, not a real client reconnect or full-pack reboot. The live NaN-health failure remains unreproduced; the report was from the build preceding the current fixes. No blanket HP correction has been introduced.

The current release is 1.4.80. The above new fixes were tested using the 1.4.79 development version before the release bump; the version-only rebuild preserves those implementations.

## Logout/entity-unloading investigation (2026-10-23, 1.4.80)

Reviewed the separate “Fix Vinery crash” chat and the supplied October 23 server reports/logs/playerdata comparisons. The failing set is Minecraft's PersistentEntitySectionManager.chunksToUnload (Yarn ServerEntityManager.unloadChunks). The reports show fastutil iterator ArrayIndexOutOfBoundsException and wrapped-list NullPointerException. Successful logouts with the same save/actions mean corrupt playerdata/items are unproven. Netty-thread Origins disconnect logging and forceload updates also occur during successful logouts; neither identifies the writer of this queue. The legacy Fabric-1.21.1 branch's Apoli unload Mixin is not present in this branch.

Fabric disconnect callbacks now dispatch Andromeda cleanup to the server executor when called from another thread. They remain synchronous on the server thread; delayed old-connection cleanup is skipped if a replacement player already owns the UUID. A real Fabric disconnect-event invocation from a worker verified deferred execution on the server thread. This addresses shared-map/ability cleanup threading; there is no established causal link to the live unloading crash.

An isolated native-manager fixture deliberately changed chunk visibility from an entity-removal callback while processUnloads/removeIf was active. Shrinking that same set produced ArrayIndexOutOfBoundsException (index 2045, array length 33) without any concurrent writer. This demonstrates a matching failure mechanism, not the live-server cause or a guilty mod. The trace also captured a separate worker-thread queue mutation. The artificial manager used in-memory storage and did not modify world chunks/player saves. The diagnostic smoke scanner flags the intentionally caught exception; the fixture's explicit assertions and normal server exit are the relevant result. Temporary probe sources are excluded from release jars.

The diagnostic Mixin is Fabric-only because NeoForge patches the unload loop to a different implementation. To collect the missing caller evidence, enable **Logout Diagnostics (Fabric Server)** through `/andromedaorigins config` as an OP (level 2), then retain the complete server log, including startup, `ENTITY_UNLOAD_TRACE` stacks and any crash report. An `ENTITY_UNLOAD_TRACE enabled via Fzzy Config` message confirms activation. No JVM argument is needed; the old property is no longer read. The trace compares callers with the manager's construction thread. It records successful queue additions/removals through updateChunkStatus only when off-thread or reentrant during removeIf, capped at 32 mutation reports per JVM. Repeated status updates which do not change membership are filtered. Failure logging remains enabled after the mutation-report limit, includes manager identity, last predicate chunk, last unloading entity class/UUID and before/after queue sizes, then rethrows the original exception. The last predicate/entity is context, not proof that entity caused the failure (iterator failure can occur after its callback returns). Direct reflective/mod writes to the set are outside mutation-stack coverage. The native set and removal algorithm remain in use; diagnostics are off by default.

### Follow-up with Origins 1.47.10 / Opoli 1.117.0 — 2026-10-23

The controlled same-thread failure still reproduces on the upgraded Fabric runtime. Three control cycles of 1,024 chunk loads/unloads without reentrant mutation complete successfully. Deliberate callback mutation shrinks the queue from 1,024 entries to one and still produces the matching Fastutil shiftKeys/remove exception. The refined trace identifies the artificial mutator and the last chunk/entity, retains failure logging after 32 mutation reports, and filters a duplicate off-thread no-op update. The same controlled failure also reproduces with tracing disabled; normal controls still pass, confirming that tracing neither creates nor suppresses that fixture failure. This establishes instrumentation coverage, not an automatic diagnosis of the production server. Deferred Fabric disconnect cleanup is verified after queued server tasks run.

Upstream bytecode review: Origins' disconnect handler updates selection/cache state and logs persisted Origins; no direct entity-manager queue access was found in that handler. Opoli's entity-unload callback fires ActionOnCallbackPower's removed action and cleans powered-entity/rope/label state; destruction-only entity-set cleanup is guarded by shouldDestroy. Andromeda's two entity_action_removed definitions only remove Figura/Submersion tags; TagAction delegates to Entity.removeTag. No direct chunk-load request was found in these actions. This narrows these specific paths without excluding indirect mod interactions or other callbacks.

A scan of the local pack finds entity-manager integrations in Architectury, OPAC, PuzzlesLib and Lithium, plus entity-unload callbacks in several mods. This is a candidate inventory, not blame: the local client pack is not an exact snapshot of the crashing server (for example, VanillaBackport versions differ), and nested/server-only mods may be absent. A live mutation stack is still needed to identify the actual caller.

The exact live caller remains unknown. Do not present the scheduling correction or the controlled reproduction as a confirmed fix. Selkie resources, ability balances, existing cleanup behavior, and mod version 1.4.80 are preserved.

Final production jars passed server-ready, helper commands, reload and graceful shutdown on both loaders. Fabric was tested with the trace enabled and its activation banner confirmed; NeoForge contains no diagnostic Mixin/config. No temporary logout probe is packaged.

## Dependency upgrade validation — 2026-10-23

Version remains 1.4.80. Tested with Origins 1.47.10 and Opoli 1.117.0 on Fabric and NeoForge in isolated background dedicated servers. Earlier sections retain their original test dependency versions.

- Full build, 318 JSON resources / 194 powers / 26 Origins, and 18 static Mixin bytecode checks pass.
- Both runtimes: all 26 standard/Champion selections, idempotent attribute repair, 178 native key-ability activation cases (standing and sneaking), 120 simulated passive-power ticks per Origin, and native Origin removal pass without exceptions. These are execution checks, not proof of every ability's complete multiplayer semantics.
- Both runtimes: Arachne climbing/crouch holding, toggles and suppression; Nereid expired hydration, air drain, suffocation, suppression, Water Breathing, Conduit, physical water, rain, and Champion exemption pass.
- Both runtimes: native Faerie hover gravity and removal, ground/air jump-press classification, Veilborn active-state lookup through LivingEntity, and 180 simulated Webbed power ticks on six Origins retain finite health/attributes/velocity. Power-only simulations do not run the full server-player cleanup loop and are not used to assert damage or post-expiry movement restoration.
- Clean production builds contain no temporary AbilityProbe/LogoutProbe classes. Dedicated startup, helper commands, datapack reload and orderly shutdown are checked separately after removing instrumentation.

Client rendering, Iris/BadOptimizations visuals, physical keyboard movement and real multiplayer synchronization require a client session; none is claimed as visually tested here. Compilation checks the current client API references. Existing logout diagnostics remain separate: the live unloading-crash cause is still unconfirmed. New upstream gameplay features are deferred.

## Selkie orphaned silence and PAL flight — 2026-10-24

The supplied Selkie player save has hydration 199/300, no groundweak helper, but common/silenced from minecraft:ccontrol and silenced_sources=1. The snapshot is read-only and is not included in release artifacts. Replaying only its Apoli powers/resources in synthetic server players reproduces blocked primary dash and secondary through native KeyDispatch.pressPower on Fabric and NeoForge. Reconciliation removes the orphaned crowd-control source and both activate. Five helper grant/removal cycles, overlapping petrification/infatuation, and preservation of a foreign silence source pass. These cycles simulate the helper transitions; they do not reproduce the original live sequence that caused counter drift. Existing saves recover automatically on join/periodic server reconciliation; manual repair also invokes reconciliation. Legitimate silence remains effective.

With PAL 1.10.0 installed in the isolated Fabric server, Veil Step entry, repeated force-flight maintenance, manual exit, suppression, death, timeout and persistent-snapshot recovery pass without external-ability warnings. Dedicated source ownership is verified; another PAL provider granted before or during Veil remains, and a provider revoked during Veil is not resurrected from the old snapshot. Creative flight remains enabled. NeoForge without PAL passes the same lifecycle tests through the fallback. The PAL bridge uses cached reflection over PAL's public API so the common jar does not require a Fabric-only dependency. PAL is not bundled or made mandatory. Client visuals were not tested. These fixes do not establish a cause for the separate unloading crash.

## Shared restriction audit — 2026-10-24

The callback/counter graph was traced across all power definitions: Gorgon ophidianed owns petrified; petrified owns restrained/silenced/pacified; Arachne webbed and Gorgon constricted also own restrained; Siren infatuated owns silenced/pacified; Selkie groundweak owns silenced. These share the same persisted increment/decrement pattern as the confirmed Selkie failure. A shared CrowdControlLifecycle now resolves petrification before its dependents, derives counters from current unsuppressed owners, removes expired/missing-timer helpers from minecraft:ccontrol, and reconciles on join, periodic ticks and manual repair. Beneficial toggles/Unstoppable are not reset generically: they also have direct grants and different ownership semantics.

Both isolated loaders pass restriction fixtures on all 26 standard/Champion Origins: stale counters set to seven and orphaned parent effects are cleared; four overlapping live helpers retain restrictions and correct counts; removing petrification preserves remaining web/constriction/infatuation; expired and missing timers recover; foreign sources survive; baseline movement/attack attributes return; and health is unchanged. Orphan Wet from minecraft:debuff is removed on all 26, while valid borrowed hydration and foreign Wet grants remain. This tests synthetic orphan states and owner transitions, not every possible live multiplayer sequence or a client visual session.

The Fabric operator command is verified through the native dispatcher, including its required literal `from`:

```mcfunction
/apoli:power revoke PLAYERNAME andromeda_origins:common/silenced from minecraft:ccontrol
```

Use it for the reported online, rehydrated Selkie when no legitimate silence should remain. It revokes only that source; it does not reset their Origin, abilities or inventory. Until the new jar is installed, another dehydration cycle can recreate the stale state. The NeoForge permission/command registration differs, so this command check is specifically for the reported Fabric server.

## Origins 1.47.12 / Opoli 1.119.0 compatibility — 2026-10-24

Exact release IDs are pinned in gradle.properties for both loaders; Andromeda remains 1.4.80. Both isolated dedicated runtimes pass all 26 Origin selections/attribute repair/removal, 178 native active-power execution cases, 120 simulated passive-power ticks per Origin, shared crowd-control orphan/overlap/expiry/source-preservation fixtures, Arachne climb/crouch/toggle/suppression, native hover gravity/removal and Faerie ground/air press classification, and Nereid hydration expiry/suffocation/protection fixtures. Server helper commands, reload and orderly shutdown pass. The Fabric fixture retains optional PAL 1.10.0; no new PAL lifecycle claim is made beyond the earlier targeted test.

No production compatibility-code changes were needed for this dependency pair. New custom-effect source names were checked against Andromeda resources; no matching source-ID checks or modify_xp_gain definitions required migration. Existing Iris/PAL bridges, shared effect cleanup, and opt-in Fabric unload diagnostics remain. This is execution and condition testing with synthetic server players, not a complete client-visual or live multiplayer test. The production logout-crash caller remains unidentified, and this dependency update is not claimed to fix it.

## 1.4.81 follow-up: nested iteration and cached airborne transfer — 2026-10-24

An additional artificial native-manager fixture calls `manager.tick()` from its entity's unload callback while an outer `processUnloads/removeIf` is iterating. Unlike the earlier status-mutation fixture, it performs no updateChunkStatus while unloading. The inner iterator removes queue entries directly. With Origins 1.47.12 / Opoli 1.119.0, the outer iterator reproduces the live `LongOpenHashSet$SetIterator.nextLong` null-wrapped failure on the server thread. In this fixture queue size becomes -1 after double removal; this is observed corruption, not a repair target. The opt-in Fabric trace now logs `ENTITY_UNLOAD_ENTRY` before nested/off-thread iteration, capped at 32 entries separately from 32 mutation stacks. Failure reports continue independently and original failures propagate. The same null-wrapped failure reproduces with diagnostics disabled. This closes a coverage gap: absence of updateChunkStatus mutation traces alone cannot exclude a nested unload pass.

A release artifact matching the crashing server's Chunksmith 4.6.0+1.21.1 (Modrinth OX87bYBj) was inspected without installing it. Unlike the local client pack, it contains an entity-manager load interception and a MinecraftServer housekeeping path which explicitly ticks entity managers. The inspected housekeeping injection is at tick TAIL (method_3748), not evidence of reentry during a world tick. Its fresh-chunk read interception uses the IO mailbox. This makes the server-only mod inventory relevant, but establishes neither an unsafe call nor culpability. Do not attribute the crash to Chunksmith or disable it on this evidence alone. The complete live caller remains unknown; collect ENTITY_UNLOAD_ENTRY, ENTITY_UNLOAD_TRACE and ENTITY_UNLOAD_FAILURE with startup and the crash report.

The optimized air-speed bridge passed both dedicated loaders for Arachne, Champion Arachne, Faerie, Champion Faerie and Gorgon speedup: repeated unchanged reads, all three modifier operations, add/remove, stacked total modifiers, Speed application/removal, sprinting, suppression and final power revocation match the original bucket-summed calculation. Tests invoke the injected handler on synthetic players; client visuals and actual keyboard handling remain outside this pass. There is no wall-clock performance benchmark claim. Full builds still validate 318 JSONs, 194 powers, 26 Origins and 18 static Mixin classes. All temporary fixtures are excluded from production/source artifacts.

## Live Fzzy Config diagnostic switch — 2026-10-24

The existing server-authoritative gameplay config now exposes logoutDiagnostics, default false, with OP level 2 permissions and the existing separate client/server save policy. It is copied into the volatile immutable runtime snapshot by the normal Fzzy sync/update hooks. Fabric's trace reads that snapshot; NeoForge shows the Fabric-only setting but does not install the Fabric diagnostic Mixin. Owner threads are always remembered when managers are constructed so enabling the option later covers already-running worlds. Server lifecycle/tick hooks log enabled/disabled transitions. Limits remain per JVM and toggling does not reset them.

An isolated Fabric fixture started without a diagnostics JVM flag and with the option disabled. It constructed a native manager while disabled, verified a quiet worker update, enabled through a real AndromedaGameplayConfig.toState/onSyncServer path, and captured both worker mutation and nested iteration stacks with the correct owner thread. After disabling through the same config path, another worker mutation remained quiet. The deliberate nested failure still propagated and was caught only by temporary test code. Default false and OP2 configuration were asserted. The GUI itself was not driven; it uses the existing generated Fzzy screen and translation conventions. Temporary probes are removed from production/source artifacts; version remains 1.4.81.

## Historical Chunksmith / 1.4.74–1.4.75 review (2026-10-24)

Read-only source/binary review; no confirmed crash fix or gameplay change. Compared the exact Minecraft 1.21.1 Fabric Chunksmith 4.1.0 and 4.3.0 artifacts. Only 15 classes differ; all 24 compared server mixin/platform classes are byte-identical, including the extra entity-manager housekeeping tick, entity-storage read redirect and FabricWorld ticket code. The housekeeping tick therefore predates 4.3.0. Its dedicated-server entry is server tick TAIL; sequential extra ticking does not establish re-entry. The new configurable world-enter pregen-centre branch is gated off on dedicated servers, although abandoned-state restoration still runs before that gate. Changed LOD network references add reachability warning bookkeeping, with no new native unload calls found. Existing hooks/configuration/interactions remain candidates; this does not exonerate Chunksmith. FabricWorld and the entity-storage read mixin also match 4.6.0; MinecraftServerMixin does not, so newer code is not universally identical.

Reviewed September 18 commits 393e149 through d547ddc, covering integrity repair, gill migration, Mortal Resolve join recovery, aquatic immunity and hydration JSON. Added repair callbacks run on data sync/respawn/manual repair; no new direct entity-unload/disconnect hook or native unload-queue access was found in this change. Changed power commands are tag/title/particle actions, with no new teleport/forceload command. Component add/remove callbacks remain an indirect interaction surface. The earlier Apoli EntitySet unload optimization predates this change, skips players/non-destroying chunk-unload removal, and is absent from the current Overgrown branch. Historical binary identity remains uncertain: commit 3f501c5 is titled v1.4.75 but still declares 1.4.74, with the version bump in ab4dd22; the actual deployed historical 1.4.74 JAR was not available for this review.

The supplied runtime archive matches 20 unload crashes: 17 within one second of the nearest logout, one six seconds later, one 60 seconds later, and one 130 seconds later immediately after a same-second teleport. Successful logouts from the same accounts also include forceload-ticket updates. These are correlations, not caller attribution. The archive has no ENTITY_UNLOAD_ markers and latest.log still identifies 1.4.80. The prepared 1.4.81 nested-entry/status-update traces remain necessary to distinguish re-entry from another queue writer. No live-server change, Chunksmith A/B runtime test, or new runtime reproduction was performed in this pass.

## Chunksmith test-server A/B runs (2026-10-24, 1.4.81)

Ran the unmodified 1.4.81 Fabric release in the designated Fabric Server area's separate `andromeda-unload-ab-1481` directory. Five arms began from identical stopped test-world snapshots: reduced stack without Chunksmith, with 4.3.0, with 4.6.0, and an OPAC 0.32.7 / Lithium 0.15.4 / ServerCore 1.5.19 pair without and with 4.6.0. Common mod hashes matched within each comparison. The final treatment additionally ran active Chunksmith generation.

Completed 672 real network logouts (336 orderly, 336 abrupt), 1,008 console-requested teleports and 30 bulk 512-chunk unload/reload waves across eight Origins. All arms recovered all 256 persistent cows in their final reload. Clients negotiated the version/component protocol, acknowledged teleport packets and finished with zero client errors. The stress phase used 100 TPS; both broader arms also completed two eight-player rounds at 20 TPS. The final Chunksmith arm generated 4,225 new chunks while bulk unloading and 40 logout events occurred. OPAC claims and its logout forceload-ticket update messages were exercised; production claim configuration/marked-forceload state was not independently reproduced.

All five servers exited normally, with no native iterator failure, unexpected fatal exception, or enabled unload-diagnostic mutation/re-entry warning. This is a negative reproduction result, not proof of a fix or exoneration of a mod. Windows, a fresh flat world, current dependencies, Creative-mode headless clients and a reduced/default-config mod stack differ from the Linux production world, player state, complete mod list and configuration. Headless clients skip command/recipe display decoding; rendering, combat and ability activation were not tested here. Existing nonfatal OSHI/JNA and data-fixer startup warnings remain. Earlier client-handshake pilots were excluded from the results. No artificial reentrant fixture was used in these runs. Test processes were stopped; gameplay code, release JAR hash and version 1.4.81 are unchanged.

## Full-pack/config Chunksmith comparison (2026-10-24, 1.4.81)

Completed two isolated Fabric arms using all locally available mod IDs from the latest supplied server report (232 standalone JARs without Chunksmith, 233 with 4.6.0), the user-authorized Andromeda Master Pack (1) client configs/Paxi packs, and the unchanged 1.4.81 / Origins 1.47.12 / Opoli 1.119.0 release pair. Figura is absent as requested. GriefLogger was not added from the extra server-mod profile because it was absent from the latest crash configuration. Starting test-world snapshots and common JAR hashes matched.

Each arm completed 144 real network logouts, 216 teleports, six 512-chunk bulk unload/reload waves and a final 256/256 cow recovery, including two eight-player rounds at 20 TPS after accelerated stress. All completed-run clients had zero errors; both servers exited 0. Enabled unload diagnostics recorded no mutation/re-entry/failure warning, and neither native iterator corruption nor an unexpected fatal exception was reproduced.

The full-pack Chunksmith task targeted 4,225 new chunks but only reached a last logged 1,873 (44.33%). Its 4-GB heap guard repeatedly held generation, including a hold lasting at least 120 seconds; this is a capacity/backpressure limitation, not a matching unload crash or successful generation completion. The bounded heap reflected available host memory. No memory guard was disabled. Full-generation stability at the production heap size remains untested.

External Discord/relay and shared storage messaging were explicitly disabled/localized in both test copies before approved startup; the live server/profiles were untouched. Both copies retain a Stellar View dependency override for its client-only Mod Menu requirement after verifying the common initializer is a no-op. Headless clients were extended for Better Combat and Spell Engine configuration acknowledgements; earlier startup/handshake pilots were excluded. The copied Vinery config rejects decimal cherryGrowthChance=0.4 where the mod expects an integer and falls back to defaults; stale datapack item/function references also remain. Those separate config errors were preserved, not silently repaired. These fresh flat-world Windows/eight-client runs do not reproduce production world/player/NPC data, Linux scheduling or the live server's actual world-specific configs; client configs were the user's explicit choice. Test game/LOD/voice ports are released. Gameplay code, release JAR hash and version remain unchanged at 1.4.81.
