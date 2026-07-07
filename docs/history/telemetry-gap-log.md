# Telemetry Gap Remediation Log

Tracks fixes applied from the 2026-06-23 telemetry gap analysis (ranked Critical → Low).

| # | Priority | Item | Status | Notes |
|---|---|---|---|---|
| T-1 | 🔴 Critical | `fire_alarm_activate` not written to CSV | ✅ Done | Added `TelemetryCsvWriter.writeRow()` in `FireAlarmBlock.useWithoutItem()` alongside existing `session.logger.log()` |
| T-2 | 🔴 Critical | `assembly_area_reached` not written to CSV | ✅ Done | Added `TelemetryCsvWriter.writeRow()` in `AssemblyZone.onPlayerArrived()` alongside existing `session.logger.log()` |
| T-3 | 🟠 High | `session_end` hazard_distance hardcoded to `99.0` | ✅ Done | Replaced with `hazardDistance(session, level, playerForCsv)` in `SimulationManager.endSimulation()` |
| T-4 | 🟠 High | CO2ExtinguisherItem emits no telemetry | ✅ Done | Added `extinguisher_use` row with `nearby_player_count` in `CO2ExtinguisherItem.sprayServer()`; `extinguishAt` now returns `boolean`; added `countNearbyPlayers` helper |
| T-5 | 🟡 Medium | AssemblyZone coordinates are placeholder | ✅ Done | Library `AABB(30,-35,64,76,-28,82)` north of building (verified); CCS `AABB(76,-35,73,136,-28,90)` outside south wall (Z:73–90) |
| T-6 | 🟡 Medium | ExitZones coordinates are placeholder | ✅ Done | `main_exit AABB(50,-34,93,54,-30,96)` tuned; `ccs_main_exit AABB(95,-33,68,125,-29,74)` set (south-wall centre) |
| T-7 | 🟡 Medium | `fire_alarm_positions` in map_metadata.json was empty `[]` | ✅ Done | Added `TelemetryCsvWriter.scanAndRegisterFireAlarms()` which scans the arena for `FireAlarmBlock` after first structure placement; rewrites `map_metadata.json` with discovered positions |
| T-8 | 🟢 Low | `mod_version` missing from sessions CSV | ✅ Done | Added `mod_version` column to `sessions_*.csv` header and rows; resolved via `ModList.get()` + cached in `TelemetryCsvWriter` |
| T-9 | 🟢 Low | `extinguisher_use` throttled to one per 40 ticks | ✅ Done | Decoupled `resetExtinguishEventPending()` from `cleanupFireOutsideBounds()`; now resets every 20 ticks (1 s window) for better temporal resolution |
| T-10 | 🔴 Critical | CSV `move` event misnamed — ML pipeline expects `move_tick` | ✅ Done | Renamed `"move"` → `"move_tick"` in `SimulationManager.tickTelemetry()` |
| T-11 | 🟠 High | CO2 extinguisher not in Turso `event_log` | ✅ Done | Added `session.logger.log("extinguisher_use", ...)` to `CO2ExtinguisherItem.sprayServer()` every 20 damage ticks; dashboard `extractRubricSignals` now counts both `EXT_SPRAY` and `extinguisher_use` |
| T-12 | 🟢 Low | `SIM_START` event missing spawn position | ✅ Done | Moved `SIM_START` logger call to after `spawnPos` resolution; now includes `x/y/z` so dashboard event timeline shows where the player spawned |
| T-13 | 🟠 High | `extinguisher_positions: []` always empty in `map_metadata.json` | ✅ Done | Added static nominal positions (`LIBRARY_EXTINGUISHER_POS`, `CCS_EXTINGUISHER_POS`) in `TelemetryCsvWriter`; `map_metadata.json` now written on every server start (not just first) |
| T-14 | 🟡 Medium | Contract doc used `move` but mod emitted `move_tick` | ✅ Done | Updated `telemetry_contract.md` §3, §4, §6, §7 to say `move_tick` throughout |
| T-15 | 🟡 Medium | CCS scenario types (`ccs_fire`/`ccs_earthquake`) missing from contract and DB | ✅ Done | Updated `telemetry_contract.md` §2/§3/§5; `endSimulation` stores `session.getState().name()` so CCS sessions write `CCS_FIRE`/`CCS_EARTHQUAKE` to Turso; dashboard `simulation_type` type and SQL aggregations updated |
| T-16 | 🟢 Low | New `duck_cover_hold` event type (live duck/cover/hold drill) not in contract/dashboard | ⏳ Pending | Emitted by `SimulationManager.applyDuckCoverHold` via `session.logger.log()` + `TelemetryCsvWriter.writeRow()`, same shape as `fire_alarm_activate` (x/y/z, no `hazard_distance`). Dashboard session-timeline event rendering needs to account for it — tracked as a follow-up in the `BERONG_SMP_WEB` repo. |
| T-17 | 🔴 Critical | The Academy (new tutorial) emitted **no telemetry at all** — verified by grepping the whole `academy` package for `Telemetry`/`TursoClient`/`SessionManager` (zero matches) | ✅ Done | Added `AcademyTelemetry` + wired `academy_*` event rows into all 4 room managers — see "Academy telemetry" note above. `scenario_type="ACADEMY"` rows land in the same `gameplay_logs_*.csv`; dashboard ingestion/visualization for this scenario type is a follow-up in `BERONG_SMP_WEB`, same as T-16. |


