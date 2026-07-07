# F3 Tuning To-Do

All items below require in-game F3 coordinate verification.
Run `./gradlew runServer`, walk each location with F3 open, then update the file noted.

---

## 1. CCS Admin Building — Additional Room Coverage

**Why:** The current named rooms only cover small areas (classrooms, offices). The player path
in movement map recordings covers large unlabeled areas — corridors, lobby, stairwells.
Adding these makes the movement map meaningful for the full building.

**What to capture:** Walk each area below with F3 and note the two opposite floor-level corners
(same Y on both corners = floor level). Ceiling Y = floor Y + 3 (or actual room height).

| Area | File to update | Notes |
|---|---|---|
| Main lobby / entrance hall | `SimRoom.java` + `floorplans.ts` | Ground floor, large open area near Z≈4–16 |
| Ground floor corridor (between rooms and ICTS wing) | `SimRoom.java` + `floorplans.ts` | Connects Room 105–Faculty Room to ICTS |
| Stairwell (ground → 2nd floor) | `SimRoom.java` + `floorplans.ts` | Mark as `floor:'both'` in floorplans |
| 2nd floor corridor / landing | `SimRoom.java` + `floorplans.ts` | Connects Room 202–Room 205 row to TESOL wing |
| Any other named spaces (storage, restrooms, etc.) | `SimRoom.java` + `floorplans.ts` | |

**Files to update:**
- `src/main/java/net/necookie/disastersim/world/SimRoom.java` — add entries to `CCS_GROUND_ROOMS` or `CCS_UPPER_ROOMS`
- `apps/dashboard/src/lib/floorplans.ts` (dashboard repo) — add entries to `CCS_ROOMS` with correct `floor` value

---

## 2. CCS Assembly Zone

**Why:** `AssemblyZone.CCS_ZONE` is currently PLACEHOLDER `(76,-35,72) → (136,-28,88)` — a rough
estimate south of the building. The real evacuation assembly point may be in a different location.

**What to capture:** Walk to the designated assembly area outside the CCS Admin Building with F3.
Note the bounding box corners of the safe zone.

**File to update:** `src/main/java/net/necookie/disastersim/world/AssemblyZone.java`
```java
private static final AABB CCS_ZONE = new AABB(X1, Y1, Z1, X2, Y2, Z2); // replace placeholder
```

---

## 3. CCS Exit Zones

**Why:** `ExitZones.CCS_ZONES` has one placeholder exit `ccs_main_exit` at `(76,-33,70) → (90,-29,74)`.
The actual main door of the CCS Admin Building may be in a different location.
Additional exits (side door, fire escape) should be added too.

**What to capture:** Stand in each doorway with F3. Note the thin AABB (2–3 blocks deep) that
a player would pass through when exiting.

**File to update:** `src/main/java/net/necookie/disastersim/world/ExitZones.java`
```java
public static final List<ExitZone> CCS_ZONES = List.of(
    new ExitZone("ccs_main_exit", new AABB(X1,Y1,Z1, X2,Y2,Z2)),
    new ExitZone("ccs_side_exit", new AABB(X1,Y1,Z1, X2,Y2,Z2))  // if applicable
);
```

---

## 4. LSPU Library — Additional Exit Zones

**Why:** Only `main_exit` is currently defined. The library may have side or rear exits.

**What to capture:** Walk each library door with F3 and note doorway AABBs.

**File to update:** `src/main/java/net/necookie/disastersim/world/ExitZones.java`
```java
public static final List<ExitZone> ZONES = List.of(
    new ExitZone("main_exit", new AABB(50,-34,93, 54,-30,96)), // already set
    new ExitZone("side_exit", new AABB(X1,Y1,Z1, X2,Y2,Z2)),  // add after F3
    new ExitZone("rear_exit", new AABB(X1,Y1,Z1, X2,Y2,Z2))   // add after F3
);
```

---

## 5. Tutorial Station Offsets

**Why:** All NPC/station positions in `TutorialManager` are documented as placeholder offsets
from `LobbyManager.LOBBY_POS = (0,-33,0)`. Verify each after loading the lobby structure.

**File to update:** `src/main/java/net/necookie/disastersim/tutorial/TutorialManager.java`
— look for constants named `*_STATION` or `*_POS`.

---

## 6. Academy (New Tutorial Building) — Room 1 Sub-Coordinates

**Why:** `AcademyBuildingManager.POS`, all 4 NPC anchors, and the Room 3 table row are confirmed
against the schematic's actual placed entity data (see `docs/new_tutorial_script.md`'s coordinate
table). The finer marks *inside* Room 1 (Officer Cruz's Movement School) were given descriptively
by the user, not as exact F3 readings, and are currently placeholder positions in
`academy/room1/CruzRoomManager.java`.

**What to capture:**

| Item | Current placeholder | File to update |
|---|---|---|
| 4 green floor marks (Phase 1: Briefing) | `(-150,-33,34)`, `(-146,-33,30)`, `(-142,-33,34)`, `(-139,-33,30)` | `CruzRoomManager.GREEN_MARKS` |
| Maze exit point (Phase 2, used for the waypoint arrow) | `(-121.5,-33,31.5)` | `CruzRoomManager.MAZE_EXIT_POINT` |
| Jump obstacle line(s) (Phase 3) — not currently enforced per-obstacle, only "reached the far edge" | none (zone-only gate) | `CruzRoomManager.tickJump` |
| Jump zone exit point (used for the waypoint arrow) | `(-105.5,-33,31.5)` | `CruzRoomManager.JUMP_EXIT_POINT` |

**What to capture:** Walk each spot with F3 and note the exact `X Y Z`. For the jump obstacles,
also note whether per-obstacle jump enforcement is worth adding (currently the room only checks
that the player reached the zone's far edge, not that they actually jumped over anything — a
deliberate simplification while positions were unverified, matching the old tutorial's forgiving
style of not punishing imperfect technique on non-critical steps).

**Also verify while there:** `AcademyBuildingManager.VIEWPOINTS`'s `sgt_reyes`/`sgt_santos`/
`capt_morfe` entries currently reuse each NPC's own anchor position with yaw/pitch left at 0 (no
captured "facing" shot yet, unlike `officer_cruz`). Capture a proper facing angle for each and
update those three entries the same way `officer_cruz` was captured.

## After updating coordinates

1. Rebuild: `./gradlew compileJava`
2. Run server: `./gradlew runServer`
3. Test the zone in-game (walk into it, verify telemetry fires)
4. Update `CLAUDE.md` — remove PLACEHOLDER notes for anything confirmed
5. Commit with message: `fix(world): tune <zone name> coordinates from F3`
