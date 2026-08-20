# JustTeams-Fabric Project Status

## Purpose

This document is the persistent implementation checklist, completed-work ledger, and resume point for the Fabric recreation of JustTeams.

Rules for maintaining this document:

1. Update it after each meaningful implementation, verification, or roadmap checkpoint.
2. Record completed work separately from work that merely exists in the codebase but has not yet been behaviorally audited.
3. Record deliberate deviations explicitly.
4. Before starting a new implementation area, consult this file to avoid repeating completed work.
5. Do not mark an area fully replicated solely because the project compiles.

The goal is feature and behavior parity with the reference implementation where that behavior can be verified. The team bank is currently the explicit deliberate exception.

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

Latest recorded user build checkpoint:

- `BUILD SUCCESSFUL`
- Fabric Loom `1.15.5`
- 8 actionable tasks executed
- approximately 2m 52s

This successful build is the baseline for the current cycle.

## Deliberate Exception: Team Bank

The bank intentionally does **not** use a generic economy provider.

Instead:

- A team bank is represented by a chest-style inventory.
- Only items configured in `justteams.properties` as currency may be deposited.
- Withdrawals remain permission-controlled.
- `justteams.bypass.bank.withdraw` bypasses the member withdrawal restriction.
- The bank feature can be disabled through configuration.

Default configured currency items currently include emerald, emerald block, and deepslate emerald ore.

## Completed / Verified Infrastructure

These items have already reached meaningful implementation or verification milestones and should not be restarted from scratch:

- Project builds successfully against the current pinned dependency set.
- Team persistence infrastructure exists and has been brought through multiple API compatibility fixes.
- Persistent item-backed bank storage has replaced the earlier generic economy-provider approach.
- LuckPerms-aware permission handling exists with fallback behavior when LuckPerms is unavailable.
- Team chat routing exists and uses the corrected server lookup path compatible with the current mappings.
- Friendly-fire/PvP handling is registered through Fabric's server living-entity damage event and checks team membership plus persisted PvP state.
- Team state includes persisted ownership/co-owner, membership, public/private, PvP, home, warps, bank, and glow-related data.
- Team GUI and settings GUI infrastructure exist.
- The canonical permission class mirrors a substantial set of established JustTeams command and bypass nodes.

These areas still require feature-parity and runtime auditing; they are not automatically classified as fully replicated.

## Implemented Areas Requiring Behavioral Audit

Audit the existing implementation rather than recreating these systems:

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

## Known Gaps / Active Investigations

### Team glow — ACTIVE

`Team` contains a persisted `glowEnabled` setting and storage restores it after restart.

The reference JustTeams feature set includes team glow, so this is not dead state to remove.

Current Fabric status:

- persisted state exists
- no verified `/team glow` command is currently present in the audited command surface
- no glow entry was found in the audited team settings GUI
- the current canonical permission class does not yet list a dedicated glow command node
- actual teammate highlighting still requires implementation and lifecycle cleanup

Required work:

1. Verify exact reference permission and activation semantics.
2. Add command/UI access using the verified behavior.
3. Implement teammate glow against the pinned 1.21.11/Yarn/Fabric API surface.
4. Correctly update glow when members join/leave, teams disband, settings change, and players disconnect/reconnect.
5. Add runtime testing before marking replicated.

### Team Ender Chest — ACTIVE / REFERENCE VERIFICATION REQUIRED

The GUI currently contains explicit placeholder actions for the team Ender Chest.

Required work:

1. Verify exact reference behavior and permission semantics.
2. Implement only if it belongs to the intended JustTeams replica.
3. Otherwise remove or deliberately classify the placeholder.

### Permission parity — ACTIVE

The permission class contains many mirrored nodes, but permission parity has not yet been fully audited feature-by-feature.

Do not assume the presence of a constant means every handler checks it correctly.

## Remaining Completion Roadmap

### A. Complete active glow investigation

Finish the reference audit, implementation, lifecycle handling, and runtime tests.

### B. Resolve Team Ender Chest placeholders

Verify the reference behavior before implementing or removing them.

### C. Feature-parity audit

For every existing feature, compare:

- command behavior
- permission checks
- ownership restrictions
- validation
- state transitions
- user-visible messages
- edge cases

### D. Permission-node parity

Verify exact JustTeams permission behavior, including:

- command permissions
- user/admin defaults
- wildcard behavior
- bypass permissions
- chat spy
- feature-specific nodes
- LuckPerms-present and LuckPerms-absent behavior

### E. GUI audit

Review every GUI slot, action, permission check, and inventory lifecycle.

Resolve placeholders only through verified reference behavior.

### F. Persistence audit

For every persisted feature:

1. Create or modify state.
2. Stop the server cleanly.
3. Restart.
4. Verify exact state restoration.

This includes teams, membership, ownership/co-ownership, invitations, join requests, homes, warps, settings, PvP state, bank contents, glow state, and any other persisted state.

### G. Multiplayer and edge-case testing

Test:

- multiple simultaneous teams
- invite and request races/duplicates
- leave/disband edge cases
- private/public access
- team chat isolation
- chat spy
- friendly-fire state
- glow lifecycle
- permission changes while online
- concurrent bank interaction
- disconnect/GUI-close behavior
- full inventory behavior

### H. Configuration audit

Ensure every exposed configuration property is actually consumed and invalid configuration is handled safely.

### I. Cleanup

Before release:

- remove dead code
- remove obsolete economy-provider remnants
- remove unjustified placeholders
- consolidate duplicated logic
- normalize messages and error handling
- document intentional deviations from the reference

### J. Final acceptance checklist

Classify each reference capability as exactly one of:

- **Replicated**
- **Deliberate exception**
- **Not applicable**
- **Still missing**

The project should not be considered complete merely because it compiles.

## Current Cycle

Current cycle limit: **10 rounds**.

Completed in current cycle:

- **Round 1/10:** Verified that `glowEnabled` is persisted team state and should not be casually removed.
- **Round 2/10:** Verified that team glow belongs to the JustTeams feature set; identified the missing Fabric command/UI/gameplay integration and the separate Team Ender Chest placeholders.
- **Round 3/10:** Reconciled completed infrastructure, active investigations, known gaps, and the remaining roadmap into this persistent project ledger.

## Current Resume Point

Next implementation task:

1. Continue the team-glow reference audit with strict permission/API matching.
2. Inspect the exact command and gameplay integration points needed for glow.
3. Only then make the smallest verified repository changes required.

Before beginning any unrelated feature, review this document to determine whether the work is already completed, already under investigation, or intentionally deferred.
