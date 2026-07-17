# Deep-Dive Audit Log (2026-07-17)

A third follow-up pass, after [`code-review-remediation-log-2026-07-17.md`](code-review-remediation-log-2026-07-17.md)
and [`multiplayer-concurrency-audit-log-2026-07-17.md`](multiplayer-concurrency-audit-log-2026-07-17.md),
covering areas not yet examined: command permission gates beyond `/sim_*`, SQL injection surface,
client-side code, network payload validation, entity/AI code, and the hazard/computer block state
machines. Frozen log, kept for context but not updated going forward.

Every fix below compiles clean and passes the full unit test suite (44 tests, 8 classes).

## Findings

- **Command permissions** — audited every remaining command (`ItemCommands`, `CopyRoomCommand`,
  `RegistrationCommands`). All gating matches documented intent: `/get_extinguisher`/
  `/get_co2_extinguisher`/`/get_wet_chemical_extinguisher` are intentionally open to any player
  (CLAUDE.md already documents the latter two as "(any player)"); `/copyroom` is correctly OP-gated
  and its `ProcessBuilder` clipboard call passes a fixed argument array with user text only ever
  going via stdin, never as a shell argument — no injection risk. No fixes needed.
- **SQL injection** — grepped and manually reviewed every `TursoClient.query/queryAsync/insert/
  insertAsync/executeAsync` call site in the codebase. Every one passes user-controlled data as a
  parameterized trailing argument; string concatenation only ever joins static SQL literal
  fragments across multiple lines, never a variable into the query text. Zero injection risk found.
- **Client-side code** — `ClientEvents`/`KeyMappings`/`BerongSMPClient` all correct: HUD state is
  properly reset on both logout and login (covers world exit, server kick, and crash-recovery
  rejoin), the drop-and-roll key is edge-triggered client-side as intended.
- **Network payloads — found and fixed a real gap:** `DropAndRollPayload` (the mod's only
  serverbound payload) had no server-side rate limit at all. The client only sends it from an
  edge-triggered `KeyMapping.consumeClick()`, but nothing stops a modified client from calling
  `ClientPacketDistributor.sendToServer` in a tight loop — every other player-triggered action in
  this codebase throttles itself, this one didn't. Added a 2-tick minimum interval per player.
  (`DropAndRollManager.java`)
- **Entity/AI code** — `CustomNpcEntity`/`MinimalWanderGoal`/`NpcSpawnerItem` reviewed, no bugs
  found. `NpcSpawnerItem`'s only acquisition path is already OP-gated (`/item get`), matching
  `HazardWandItem`'s precedent, so no additional in-use permission check is needed.
- **Hazard/computer block state machines — found and fixed a real bug, verified against real
  asset data:** `ComputerBlock.randomTick`'s "seed fire next to flammable blocks" logic checked
  `BlockState.ignitedByLava()` — a vanilla method that returns `true` almost exclusively for
  TNT-like blocks, not ordinary wood/wool furniture (confirmed via research). Verified the
  practical impact directly against `ccs_admin_building.schem`: `dark_oak_planks`/
  `stripped_oak_log` furniture genuinely sits within the loop's scan radius of real computer
  placements, but `ignitedByLava()` never returns `true` for either — so this entire secondary
  fire-spread mechanism was silently a no-op for the actual building material. Replaced with
  `BlockTags.PLANKS`/`LOGS`/`WOOL` checks, matching the pattern `HazardBlock.igniteNearestFlammable`
  already established. The computer's other fire-spread paths (its own 6 adjacent air cells,
  `FireEffects.spreadComputerFire` igniting other computers) were unaffected and already correct.
  (`ComputerBlock.java`)
- **Hazard/computer block state machines — found and fixed a consistency gap:**
  `HazardFacingBlock.igniteAdjacent`/`igniteRadius` still returned `void`, while `HazardBlock`'s
  identical sibling methods were widened to `List<BlockPos>` in an earlier pass so callers could
  log which positions actually caught fire. Verified this had zero live functional impact — every
  current `onHazardFailure` override across both hazard base classes (all ~85 hazard props)
  discards the return value as a bare statement, and the one caller that uses the returned list
  (`NewSim2FireTicker`) always calls `HazardBlock`'s static methods directly regardless of the
  target prop's actual type. Fixed for consistency and to close a latent trap for future hazard
  authors; backward compatible (widening void to a return type doesn't break bare-statement
  callers). (`HazardFacingBlock.java`)

## Noted but not fixed (documented, not actionable without more information or scope)

- **`HazardBlock`'s ignite helpers (`igniteAdjacent`/`igniteRadius`/`igniteNearestFlammable`) have
  no arena-bounds check**, unlike `FireEffects.simulateFire` which explicitly clamps to the
  session's arena. A hazard prop very close to an arena's boundary wall could theoretically spill
  fire onto exterior terrain. Not fixed: (1) hazard props are always scanned from within the
  arena, so the exposure is narrow (only props within 1-3 blocks of a wall); (2) any fire that did
  leak outside is already caught by the existing `FireEffects.cleanupFireOutsideBounds` sweep
  (runs every 40 ticks, removes fire outside the arena+margin) — so the actual exposure window is
  at most ~2 seconds of stray fire, not a lasting problem. Fixing this properly would mean
  threading arena bounds into these static helpers, touching call sites in `HazardManager`,
  `NewSim2FireTicker`, and the Academy's `ReyesRoomManager` (which has no arena concept at all,
  being outside any `SimulationSession`) — judged disproportionate to the narrow, already-mitigated
  risk. Flagged for awareness, not fixed.
