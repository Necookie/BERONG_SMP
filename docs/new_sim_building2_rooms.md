# New Sim Building 2.0 — Room Coordinates

F3/WorldEdit-verified absolute world coordinates for every named room in New Sim Building 2.0
(`new_sim_building2.0.schem`, placed at `SimulationManager.NEW_SIM_BUILDING2_POS = BlockPos(-182,
-34, 358)`). Captured room-by-room with `//copyroom <name>` (see `CopyRoomCommand`) the same way
`SimRoom`'s `CCS_UPPER_ROOMS`/`CCS_GROUND_ROOMS` tables in CLAUDE.md were originally compiled.

This building is now wired into `SimRoom` (`NEW_SIM2_UPPER_ROOMS`/`NEW_SIM2_GROUND_ROOMS`, mirroring
the CCS pattern) and `/sim_fire new_sim_building2` (`SimulationState.NEW_SIM_BUILDING2_FIRE` — a
prevention/intervention/evacuation scenario, see [docs/systems/simulation.md](systems/simulation.md)).
No `/sim_earthquake` variant targets this building yet. This doc remains the source-of-truth survey
data the room tables were transcribed from — re-survey and update both together if the schematic
ever changes.

Pos1 = minimum corner, Pos2 = maximum corner (as returned by `//copyroom`), both inclusive.

## 2nd Floor

Floor Y = −23, ceiling Y = −14 (10 blocks tall) for every room on this floor.

| Room | X min | X max | Z min | Z max | W | L | H | Floor Area | Wall Area | Ceiling Area | Volume |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Room 201 | -88 | -81 | 446 | 453 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Male CR | -100 | -93 | 446 | 453 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Female CR | -100 | -93 | 455 | 462 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Conference Room | -88 | -81 | 455 | 471 | 8 | 17 | 10 | 136 | 500 | 136 | 1360 |
| Room 202 | -100 | -93 | 464 | 471 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Room 203 | -88 | -81 | 473 | 480 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Lecture Hall | -88 | -81 | 482 | 498 | 8 | 17 | 10 | 136 | 500 | 136 | 1360 |
| ComLab 201 | -100 | -93 | 473 | 489 | 8 | 17 | 10 | 136 | 500 | 136 | 1360 |
| Room 204 | -100 | -93 | 491 | 498 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Room 205 | -100 | -93 | 500 | 507 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Room 206 | -88 | -81 | 500 | 507 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Room 207 | -88 | -81 | 509 | 516 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Clinic | -88 | -81 | 518 | 525 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Study Lobby | -100 | -93 | 509 | 525 | 8 | 17 | 10 | 136 | 500 | 136 | 1360 |
| Faculty Room | -88 | -81 | 527 | 534 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Research Lab | -100 | -93 | 527 | 534 | 8 | 8 | 10 | 64 | 320 | 64 | 640 |
| Library | -101 | -81 | 536 | 542 | 21 | 7 | 10 | 147 | 560 | 147 | 1470 |
| Basketball Court | -105 | -81 | 434 | 444 | 25 | 11 | 10 | 275 | 720 | 275 | 2750 |
| Hallway 1 | -105 | -102 | 445 | 542 | 4 | 98 | 10 | 392 | 2040 | 392 | 3920 |
| Hallway 2 | -91 | -90 | 445 | 535 | 2 | 91 | 10 | 182 | 1860 | 182 | 1820 |

20 rooms.

## 1st Floor

Y varies per room on this floor — see the Y min/max columns. Most rooms are the standard 10
blocks tall (Y −33 to −24), but **Main Hallway** and **Lobby** were captured as a single 1-block
floor layer (Y −33 only, not full room volume), and **General CR** is 9 tall (Y −33 to −25, one
short of the standard 10) — recorded as captured, not corrected.

| Room | X min | X max | Y min | Y max | Z min | Z max | W | L | H | Floor Area | Wall Area | Ceiling Area | Volume |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Cafeteria | -105 | -81 | -33 | -24 | 434 | 444 | 25 | 11 | 10 | 275 | 720 | 275 | 2750 |
| Room 101 | -89 | -81 | -33 | -24 | 446 | 456 | 9 | 11 | 10 | 99 | 400 | 99 | 990 |
| Under Maintenance | -105 | -97 | -33 | -24 | 446 | 456 | 9 | 11 | 10 | 99 | 400 | 99 | 990 |
| Room 102 | -89 | -81 | -33 | -24 | 458 | 468 | 9 | 11 | 10 | 99 | 400 | 99 | 990 |
| Room 103 | -105 | -97 | -33 | -24 | 458 | 468 | 9 | 11 | 10 | 99 | 400 | 99 | 990 |
| Main Hallway | -95 | -91 | -33 | -33 | 446 | 530 | 5 | 85 | 1 | 425 | 180 | 425 | 425 |
| Lobby | -118 | -81 | -33 | -33 | 482 | 498 | 38 | 17 | 1 | 646 | 110 | 646 | 646 |
| Room 104 | -89 | -81 | -33 | -24 | 496 | 506 | 9 | 11 | 10 | 99 | 400 | 99 | 990 |
| Kitchen Lobby | -105 | -97 | -33 | -24 | 508 | 524 | 9 | 17 | 10 | 153 | 520 | 153 | 1530 |
| Room 105 | -89 | -81 | -33 | -24 | 508 | 518 | 9 | 11 | 10 | 99 | 400 | 99 | 990 |
| Room 106 | -89 | -81 | -33 | -24 | 520 | 530 | 9 | 11 | 10 | 99 | 400 | 99 | 990 |
| Principal's Office | -105 | -97 | -33 | -24 | 526 | 530 | 9 | 5 | 10 | 45 | 280 | 45 | 450 |
| Badminton Court | -105 | -81 | -33 | -24 | 532 | 542 | 25 | 11 | 10 | 275 | 720 | 275 | 2750 |
| General CR | -118 | -106 | -33 | -25 | 472 | 476 | 13 | 5 | 9 | 65 | 324 | 65 | 585 |

14 rooms.

## Totals

34 named rooms surveyed across 2 floors (20 on the 2nd floor, 14 on the 1st floor).
