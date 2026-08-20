# JustTeams-Fabric Project Status

## Purpose

This document is the persistent implementation checklist and resume point for the Fabric recreation of JustTeams.

The goal is feature and behavior parity with the reference implementation where that behavior can be verified. The team bank is a deliberate exception: it is implemented as an item-backed chest that accepts only configured currency items.

## Version Constraints

All API work must match the versions pinned by this repository. Do not substitute APIs from newer or older Minecraft, Yarn, Fabric API, Fabric Loader, or Loom versions without verifying compatibility.

Current project baseline:

- Minecraft: `1.21.11`
- Yarn mappings: `1.21.11+build.4`
- Fabric Loader: `0.18.4`
- Fabric API: `0.141.4+1.21.11`
- Fabric Loom observed during builds: `1.15.5`

## Build Cadence

Work proceeds in cycles of up to **10 repository/activity rounds**, followed by:

```powershell
./gradlew clean build --refresh-dependencies
```

The last recorded checkpoint before this file was created was successful.

## Deliberate Exception: Team Bank

The bank intentionally does **not** use a generic economy provider.

Instead:

- A team bank is represented by a chest-style inventory.
- Only items configured in `justteams.properties` as currency may be deposited.
- Withdrawals remain permission-controlled.
- `justteams.bypass.bank.withdraw` bypasses the member withdrawal restriction.
- The bank feature can be disabled through configuration.

Default configured currency items currently include emerald, emerald block, and deepslate emerald ore.

## Implemented / Existing Areas To Audit

The current codebase already contains substantial implementations for:

- team creation and lifecycle
- membership and ownership/co-owner state
- invites and join requests
- team chat
- chat spy permissions
- friendly-fire / PvP behavior
- team home
- team warps
- item-backed team bank
- team GUI and settings GUI
- persistence
- LuckPerms-aware permission handling with fallback behavior

Presence in the codebase is not itself proof of complete parity. Each area still requires behavioral auditing.

## Current Investigation

### `glowEnabled`

`Team` contains a persisted `glowEnabled` setting. Storage writes and restores it.

Current task:

1. Find every command, GUI, and gameplay reference explicitly.
2. Determine whether team glow belongs to verified JustTeams behavior.
3. If verified, implement it with the exact pinned API surface.
4. If not part of the intended replica, decide whether to retain the persisted compatibility state or remove it deliberately with a storage migration/compatibility review.

Do not delete this field casually because it is persisted team state.

## Remaining Completion Roadmap

### A. Feature-parity audit

Audit every existing feature against verified JustTeams behavior, including permissions, validation, ownership restrictions, messages, and edge cases.

### B. Permission-node parity

Verify that all exact JustTeams permission nodes used by the reference behavior are represented correctly, including command, user, bypass, chat-spy, and feature-specific permissions.

Also verify fallback behavior when LuckPerms is absent.

### C. GUI audit

Review all GUI slots and handlers.

Resolve placeholder or unfinished entries only after verifying that the corresponding feature belongs to the intended JustTeams behavior. Do not invent replacement functionality merely to fill a slot.

### D. Persistence audit

For every persisted feature:

1. Create or modify state.
2. Stop the server cleanly.
3. Restart.
4. Verify exact state restoration.

This includes teams, membership, ownership/co-ownership, invitations, join requests, homes, warps, settings, PvP state, bank contents, and any other persisted state.

### E. Multiplayer and edge-case testing

Test:

- multiple simultaneous teams
- invite and request races/duplicates
- leave/disband edge cases
- private/public access
- team chat isolation
- chat spy
- friendly-fire state
- permission changes while online
- concurrent bank interaction
- disconnect/GUI-close behavior
- full inventory behavior

### F. Configuration audit

Ensure every exposed configuration property is actually consumed and invalid configuration is handled safely.

### G. Cleanup

Before release:

- remove dead code
- remove obsolete economy-provider remnants
- remove unjustified placeholders
- consolidate duplicated logic
- normalize messages and error handling
- document intentional deviations from the reference

### H. Final acceptance checklist

Classify each reference capability as exactly one of:

- Replicated
- Deliberate exception
- Not applicable
- Still missing

The project should not be considered complete merely because it compiles.

## Current Resume Point

Current implementation cycle: **Round 1/10 completed**.

Next step: continue explicit file inspection for `glowEnabled` references using the `JustTeams-Fabric` branch and nested `JustTeams-Fabric/` project path, then proceed through the remaining parity roadmap.
