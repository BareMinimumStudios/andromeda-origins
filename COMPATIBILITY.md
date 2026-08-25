# Andromeda Origins - Compatibility & Player-State Safety

## Incapacitated compatibility

Lichling does not run `incapacitated setDowned false` blindly. Its self-revive and targeted revive call:

```mcfunction
andromedaorigins internal_safe_revive
```

The Java bridge checks Incapacitated's own player-data object first. It only asks Incapacitated to revive the player when Incapacitated reports that player as downed. After an Andromeda-triggered revive, the bridge clears Incapacitated's transient `lastDmgTaken` and `lastHealthBeforeDamage` tracking so stale overkill data cannot leak into the next hit.

The Lichling self-heal uses the native Origins `origins:heal` entity action instead of routing through Minecraft's damage/death pipeline. Incapacitated remains optional; Andromeda Origins uses reflection for this bridge and loads normally without it.

## Attribute modifier safety

All explicitly named persistent Andromeda attribute modifiers use unique `andromeda_origins:*` identifiers. Apoli context-relative `*:*` modifier IDs remain context-relative and are not shared literal runtime IDs.

This project is treated as a new mod. It does not ship a prototype namespace migration, old-save NBT rewrite, or join-time cleanup for unsupported pre-release IDs.

## Restraint source counter fixes

Arachne Webbed and Gorgon Constricted previously decremented `common/restrained_sources` twice on normal expiry. Their timer now only revokes the temporary power; `entity_action_lost` is the single owner of the source decrement, so overlapping restraint effects do not lose source counts early.

## Admin recovery command

```mcfunction
/andromedaorigins repair <player>
```

This is a current-state recovery tool for a broken health/downed or interrupted crowd-control state. It:

1. clears known temporary Andromeda crowd-control powers and resets their source counters,
2. if Incapacitated is installed, asks it to rebuild/revive its downed state,
3. resets Incapacitated's transient damage tracking,
4. restores the player to their current maximum health.

Use `repair` instead of deleting the player's entire playerdata when testing this issue.

## Namespace

All Andromeda-owned origins, powers, resources, functions, tags, icon items, models, textures, and internal cross-references use `andromeda_origins:`. No secondary content namespace or save-migration Mixin is shipped.
