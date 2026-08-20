# JustTeams-Fabric Project Status

## Purpose

This document is the persistent implementation checklist, completed-work ledger, investigation notebook, and resume point for the Fabric recreation of JustTeams.

### Working rules

1. Update this document after meaningful implementation, verification, or roadmap checkpoints.
2. Record completed work separately from code that merely compiles.
3. Record deliberate deviations explicitly.
4. Consult this file before beginning a feature so completed work is not repeated.
5. Do not mark a feature replicated solely because the build succeeds.
6. When reference behavior is uncertain, document the uncertainty and ask the user for a specific reference rather than inventing behavior.
7. When navigating the reference source, use call-chain and resemblance searching, not only exact class/permission-name searches.

## Version Constraints

API compatibility is a hard constraint. Every implementation must be verified against the exact versions pinned by the Fabric project.

- Minecraft: `1.21.11`
- Yarn mappings: `1.21.11+build.4`
- Fabric Loader: `0.18.4`
- Fabric API: `0.141.4+1.21.11`
- Fabric Loom observed during builds: `1.15.5`

Do not copy an API call from documentation/source for another Minecraft, Yarn, Fabric API, Loader, or Loom version without first verifying the matching version.

## Build Cadence

Work proceeds in cycles of up to **10 repository/activity rounds**, followed by a user build:

```powershell
./gradlew clean build --refresh-dependencies
```

Latest recorded successful user build checkpoint:

- `BUILD SUCCESSFUL`
- Fabric Loom `1.15.5`
- 8 actionable tasks executed
- approximately 2m 52s

## Deliberate Exception: Team Bank

The bank intentionally does not use a generic economy provider.

- A team bank is represented by a chest-style inventory.
- Only items configured in `justteams.properties` as currency may be deposited.
- Withdrawals remain permission-controlled.
- `justteams.bypass.bank.withdraw` bypasses the member withdrawal restriction.
- The bank feature can be disabled through configuration.
- Default configured currency items currently include emerald, emerald block, and deepslate emerald ore.

## Completed / Verified Infrastructure

These areas have reached meaningful implementation or build-verification milestones. Do not recreate them from scratch without a specific parity issue:

- Project builds successfully against the current pinned dependency set.
- Team persistence infrastructure exists and has gone through multiple API compatibility fixes.
- Persistent item-backed bank storage replaced the earlier generic economy-provider approach.
- LuckPerms-aware permission handling exists with fallback behavior when LuckPerms is unavailable.
- Team chat routing exists and uses the corrected server lookup path compatible with the current mappings.
- Friendly-fire/PvP handling is registered through Fabric's server living-entity damage event and checks team membership plus persisted PvP state.
- Team state includes persisted ownership/co-owner, membership, public/private, PvP, home, warps, bank, and glow-related data.
- Team GUI and settings GUI infrastructure exist.
- The canonical permission class mirrors a substantial set of established JustTeams command and bypass nodes.

These still require behavioral parity auditing before final acceptance.

## Reference-Source Navigation Method

When a reference implementation is available, use the following investigation sequence:

### 1. Start from a known caller

If a command calls something such as:

```java
teamManager.toggleGlow(player);
```

trace that method rather than guessing a class name.

### 2. Search by resemblance, not just exact feature names

Useful search terms include:

- called method names
- method signatures
- field names (`glow`, `glowEnabled`, etc.)
- configuration keys (`team_glow`)
- user-facing messages
- API operations likely to implement the behavior
- imports used by the feature
- nearby methods in the same manager
- lifecycle events
- permission-check patterns such as `hasPermission`, `hasElevatedPermissions`, `isOp`, wildcard permissions, and admin checks

### 3. Follow the behavioral call chain

For a feature, establish:

```text
command/UI
  -> authorization
  -> state change
  -> actual gameplay/rendering mechanism
  -> affected players
  -> lifecycle updates/cleanup
  -> persistence
```

### 4. Search permission context broadly

Do not assume one command has one permission node. A single permission may authorize multiple commands, or a command may instead be controlled by a team role, feature flag, or admin permission.

Inspect the surrounding authorization code and permission definitions before adding a new Fabric permission constant.

### 5. Translate only after the reference behavior is established

After the reference behavior is understood, find the equivalent API in the pinned Fabric/Yarn version. Verify the exact method/class signature against that version before committing code.

### 6. Build after implementation

Use the user's actual Gradle build as the final compilation verification. A successful build confirms compilation, not complete behavioral parity.

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

### Team Glow — ACTIVE

`Team` contains a persisted `glowEnabled` setting and storage restores it after restart.

Reference behavior already verified:

- `/team glow` exists.
- It is gated by the `team_glow` feature configuration.
- The player must belong to a team.
- The player must have elevated team permissions, meaning Owner or Co-Owner in the reference role model.
- There is no verified dedicated `justteams.command.glow` permission check in the command handler.
- The actual command calls `teamManager.toggleGlow(player)`.

Therefore, do **not** invent a dedicated glow permission merely because a glow command exists.

The unresolved part is the implementation behind `toggleGlow` and its lifecycle behavior.

Required work:

1. Trace the reference implementation of `TeamManager.toggleGlow`.
2. Determine exactly how teammate highlighting is transmitted/rendered.
3. Determine whose glow is visible to whom.
4. Determine how the team color is applied.
5. Determine join, leave, disconnect, reconnect, respawn, world-change, disband, and disable cleanup behavior.
6. Translate the verified mechanism to the pinned 1.21.11/Yarn/Fabric API surface.
7. Add the command/UI integration using the verified authorization model.
8. Runtime-test the feature before marking it replicated.

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

### A. Complete team glow

Reference audit -> API verification -> implementation -> lifecycle handling -> runtime testing.

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
- grouped permissions
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

Include teams, membership, ownership/co-ownership, invitations, join requests, homes, warps, settings, PvP state, bank contents, glow state, and other persisted state.

### G. Multiplayer and edge-case testing

Test:

- multiple simultaneous teams
- invite/request races and duplicates
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

Classify every reference capability as exactly one of:

- **Replicated**
- **Deliberate exception**
- **Not applicable**
- **Still missing**

The project is not complete merely because it compiles.

## Current Cycle

Current cycle limit: **10 rounds**.

Recorded checkpoints:

- **Round 1/10:** Verified that `glowEnabled` is persisted team state and should not be casually removed.
- **Round 2/10:** Verified that team glow belongs to the JustTeams feature set; identified missing Fabric command/UI/gameplay integration and separate Team Ender Chest placeholders.
- **Round 3/10:** Reconciled completed infrastructure, active investigations, known gaps, and remaining roadmap into the persistent project ledger.
- **Round 4/10:** Reference investigation established `/team glow` authorization: `team_glow` feature flag + team membership + Owner/Co-Owner (`hasElevatedPermissions`).
- **Round 5/10:** Verified there is no dedicated glow command permission check in the reference handler; authorization must not be invented as `justteams.command.glow`.
- **Round 6/10:** Established the next reference-tracing target as `TeamManager.toggleGlow(player)` and documented the call-chain/resemblance-search methodology.

## Current Resume Point

**Next task: trace `TeamManager.toggleGlow(player)` in the 2.5.3 reference.**

Use resemblance/call-chain searching if exact repository search fails. Look for the actual state mutation and rendering/visibility mechanism before making Fabric changes.

If the reference source cannot be accessed or indexed sufficiently, tell the user exactly what reference is needed and why.

Before beginning unrelated work, consult this document so completed work and established investigation methods are not repeated.