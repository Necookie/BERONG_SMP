# Hazard Prop State Management Log

Tracks the rollout of gameplay-driven state management for the 20 hazard prop blocks. Previously, `HAZARDOUS` (and the sawdust layer's `ACCUMULATION`) was set once to its safe default at placement and never changed at runtime — see the 2026-07-01 audit that found no `HazardManager`/`HazardSpawner` and no `SimulationManager`/`SimulationEffects` references to the `block.hazard` package at all. All 20 items below now have a working normal→hazardous→failure lifecycle per `docs/hazard_props_spec.md`, driven by the new `HazardManager`.

| # | Item | Status | Failure consequence (Items.md) |
|---|---|---|---|
| S-1 | `plastic_trash_bin` | ✅ Done | Class A fire from the smoldering vape battery |
| S-2 | `daisy_chain_extension` | ✅ Done | Class E electrical wall fire at the cord junction |
| S-3 | `woodshop_sawdust_layer` | ✅ Done | Flash-ignites a 3×3 area at accumulation=5 (state machine lives entirely in `HazardManager`, no `HAZARDOUS` property to hook) |
| S-4 | `stage_spotlight` | ✅ Done | Ignites the curtains into a climbing Class A fire |
| S-5 | `archive_box_stack` | ✅ Done | Deep, smoldering Class A archive fire |
| S-6 | `dust_choked_pc` | ✅ Done | Class E hardware fire from the popped power supply |
| S-7 | `charging_cart` | ✅ Done | Explosion from battery thermal runaway (short delay, wide radius) |
| S-8 | `frayed_console_wire` | ✅ Done | Arcs and ignites the carpet underneath |
| S-9 | `malfunctioning_vending` | ✅ Done | Internal plastics catch fire, Class E smoke |
| S-10 | `ceiling_projector` | ✅ Done | Shattered bulb drops burning plastic clusters |
| S-11 | `swollen_phone_battery` | ✅ Done | Torch-like chemical fire (short delay) |
| S-12 | `damaged_lipo_pack` | ✅ Done | Violent white-hot burst, 2-block radius |
| S-13 | `vape_in_iron_locker` | ✅ Done | Explodes internally, ignites neighboring lockers |
| S-14 | `pa_system_backup` | ✅ Done | Severe Class E electrical panel fire, PA blackout |
| S-15 | `smartboard_inverter` | ✅ Done | Water-shorted circuitry ignites the wall behind it |
| S-16 | `unattended_grease_pan` | ✅ Done | Class F/K grease fire (short delay) |
| S-17 | `grease_clogged_hood` | ✅ Done | Sparks ignite the duct work (long delay — slow buildup) |
| S-18 | `contaminated_kitchen_bin` | ✅ Done | Instant ignition, unquenchable floor flames |
| S-19 | `jammed_panini_press` | ✅ Done | Carbonized oils engulf the countertop line |
| S-20 | `commercial_deep_fryer` | ✅ Done | Oil reaches auto-ignition, massive grease fire (short delay, wide radius) |
| S-21 | `computer` (`ComputerBlock`) | ✅ Done | Electrical fire spreads to nearby equipment (240-tick delay — faster than the 300-tick generic default). Special-cased like sawdust: no `HAZARDOUS` property, so `HazardManager` tracks a lazily-seeded timer keyed off `BURNING=true` instead of the generic activate/defuse flow. Its 3 existing ignition triggers (flint & steel, session-start, periodic CCS spread) are untouched. |

**Not yet implemented:** the water-triggers-explosion interaction called out for `unattended_grease_pan` ("Water triggers a 3x3 fiery explosion!") is flavor text only for now — no `neighborChanged`/fluid-contact hook exists yet. Score impact from hazard failures (beyond incrementing `fireSpreadCount`) is also not tuned.

**Dev testing tool:** `HazardManager.tick()` only runs inside an active FIRE/CCS_FIRE session (hazard positions are cached once at `startSimulation`), so there's no automatic way to exercise the state machine outside one. `HazardWandItem` (`berongsmp:hazard_wand`, get it via `/item get hazard_wand`) closes that gap — right-click any hazard prop to toggle normal↔hazardous (or `ComputerBlock`'s `BURNING` state) or force its failure consequence immediately, with or without a live session, instead of typing `/setblock` coordinates.

