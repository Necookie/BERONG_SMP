# Hazard Prop 3-State Log (2026-07-05)

Before this pass, `HAZARDOUS=true` served double duty: it was both "developing danger" (the window
during which a prop could be defused) *and* the direct trigger for `onHazardFailure` (real adjacent
fire) once its failure timer elapsed — there was no distinct "this prop is now actually burning"
block state, and no way to fix a hazard except spraying it with an extinguisher.

All 19 hazardous-property props (`woodshop_sawdust_layer` keeps its own `ACCUMULATION` mechanic)
now have a genuine 3-state lifecycle: **normal → hazardous → on-fire**.

- `HazardBlock`/`HazardFacingBlock` gained a new `ON_FIRE` `BooleanProperty` alongside `HAZARDOUS`.
- `HazardManager.triggerFailure` sets `ON_FIRE=true` on the prop itself before igniting adjacent
  blocks — "on fire" is now a real terminal state instead of an invisible transition.
- A new bare-hand right-click **prevention** interaction (`HazardBlock`/`HazardFacingBlock.useWithoutItem`)
  resets a merely-hazardous prop back to safe — teaches "prevention beats intervention" without
  needing an extinguisher. It does nothing once the prop is genuinely `ON_FIRE` (too late for a
  bare-handed fix at that point; matches Sgt. Reyes's new prevention → intervention lesson order,
  see Room 2 above).
- `HazardManager.defuse()` (the extinguisher-spray path) resets both `HAZARDOUS` and `ON_FIRE`.
- **Scope decision:** "on fire" is signaled via intensified particles (extra `spawnHazardParticles`
  calls + `FLAME`) and the existing real adjacent-fire ignition, reusing each prop's existing
  `_hazardous` model for the new `on_fire=true` blockstate variant — not 20 new sets of bespoke
  burning textures/models (a disproportionate art task; can be iterated later).
  `scripts/add_onfire_blockstates.py` regenerated all 19 blockstate JSONs with the new permutation.

