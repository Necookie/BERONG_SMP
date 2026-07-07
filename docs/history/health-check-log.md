# Health-Check Remediation Log

Tracks fixes applied from the 2026-06-23 health check report.

| # | Severity | Item | Status | Commit |
|---|---|---|---|---|
| C-1a | 🔴 | Assembly zone force-field on wrong face (Z+ instead of Z-) | ✅ Done | `cf57f50` |
| C-1b | 🔴 | AssemblyZone/ExitZones placeholder coordinates | ✅ Done | AssemblyZone AABB confirmed correct (same AABB drives force-field + detection); main_exit tuned from F3 (`defebb0`); side/rear exits deferred until additional buildings added |
| C-2  | 🔴 | Default BFP PIN was hardcoded `"1234"` | ✅ Done | (this commit) |
| W-1  | 🟡 | ModCommands.java monolith (807 lines) | ✅ Done | (this commit) |
| W-2  | 🟡 | onServerTick() mixes fire/quake/telemetry/HUD | ✅ Done | (this commit) |
| W-3  | 🟡 | Silent `catch (Exception ignored)` in TursoClient | ✅ Done | (this commit) |
| W-4  | 🟡 | Zero unit test coverage | ✅ Done | (this commit) |
| L-1  | 🟢 | Telemetry metadata hardcoded; coords out of sync with code | ✅ Done | (this commit) |
| L-2  | 🟢 | Tutorial station offsets placeholder | ⏳ Blocked (needs in-game F3 tuning) | — |
| L-3  | 🟢 | No rate-limit on /bfp login PIN | ✅ Done | (this commit) |
| L-4  | 🟢 | Turso URL-set/token-missing warning | ✅ Done | W-3 commit |


