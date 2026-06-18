# Fire Extinguisher — Finite Charge + PASS Simulation Plan

## Goal
Make the fire extinguisher realistic: limited agent, weakens when nearly empty, empties out completely,
and integrates with the FIRE simulation as a scoreable, teachable mechanic using the real PASS method.

---

## Real-world basis
A standard ABC dry-chemical extinguisher lasts ~8–30 seconds of continuous discharge.
**PASS** = Pull, Aim, Squeeze, Sweep.
- **Pull** the pin — safety lock; must be removed before it can spray.
- **Aim** at the base of the fire, not the flames.
- **Squeeze** the handle to discharge.
- **Sweep** side to side across the base until out.

---

## Part A — Finite Charge (standalone item behavior)

### A1. Add durability to item registration (`BerongSMP.java`)
- Register with `.durability(300)` → 15 seconds of continuous spray at 20 ticks/sec.
- Durability items auto-stack to 1; remove any explicit `stacksTo(1)`.

### A2. Damage item on each use tick (`FireExtinguisherItem.java`)
- In `onUseTick`, server-side, call `stack.hurtAndBreak(1, user, ...)` each tick.
- Minecraft handles item destruction at max damage automatically.

### A3. Sputtering phase when nearly empty
- When `stack.getDamageValue() >= stack.getMaxDamage() - 60` (last 3 seconds):
  - Skip spray every other tick (`remainingUseTicks % 2 != 0`).
  - Halve particle count.
  - Pitch-shift sound up slightly (higher pitch = weaker pressure).

### A4. Update tooltip to show charge and pin state
- Show `§7Charge: X%` computed as `(1 - damage/maxDamage) * 100`, rounded.
- Show `§cAlmost empty!` when charge < 20%.
- Show `§c[PIN IN] Pull the pin first! (Sneak + Right-click)` when pin is not pulled.
- Show `§a[PIN PULLED] Ready to spray.` when pin is pulled.

### A5. No refill mechanic (for now)
- Item simply breaks when empty; vanilla durability bar is the visual indicator.

---

## Part B — Pull the Pin mechanic

### B1. Store pin state on the ItemStack
- Use a NeoForge data component (or simple NBT tag via `stack.getOrCreateTag()`) to store
  a boolean `pin_pulled`.
- Default: `false` (pinned, cannot spray).
- A freshly crafted or given extinguisher always starts pinned.

### B2. Pin-pull interaction (`FireExtinguisherItem.use`)
- Currently `use()` immediately calls `player.startUsingItem(hand)` to begin spraying.
- Change `use()` to gate on pin state:
  ```
  if pin not pulled:
      pull the pin (set pin_pulled = true)
      play a metallic "click" sound (SoundEvents.ITEM_ARMOR_EQUIP_CHAIN or LEVER_CLICK)
      send action bar message: "§ePin pulled — ready to spray!"
      return InteractionResult.SUCCESS  (do NOT start using)
  else:
      player.startUsingItem(hand)  — spray as normal
      return InteractionResult.CONSUME
  ```
- This means the player must right-click once to pull, then hold right-click to spray.
  Two distinct actions, matching real-world PASS.

### B3. Block spray in `onUseTick` as a safety net
- Also guard at the top of `onUseTick`: if pin not pulled, stop item use immediately
  (`user.stopUsingItem()`). Prevents any edge case where spraying starts without the pin pulled.

### B4. Pin-pull action for the simulation (sneak is NOT required)
- In open-world use, a plain right-click pulls the pin (no sneak needed — it's a one-time
  action and the player naturally tries right-click first).
- In the simulation, the player is handed a pinned extinguisher and must figure out to
  right-click first before spraying works — teaching the Pull step organically.

---

## Part C — PASS Method Integration in FIRE Simulation

### C1. Give the extinguisher to the player on simulation start (`SimulationManager.startSimulation`)
- After teleporting the player, give them a fresh pinned `FIRE_EXTINGUISHER` ItemStack
  (pin_pulled = false).
- Place it in hotbar slot 0 and set held item to slot 0.
- Player must Pull (right-click once) before they can Squeeze (hold right-click to spray).

### C2. Track fires extinguished per player in `SimulationSession`
- Add `int firesExtinguished` field with `recordExtinguish(int count)` and
  `getFiresExtinguished()` to `SimulationSession`.
- Inside `FireExtinguisherItem.extinguishAt`, after a block is successfully removed:
  - Call `SimulationManager.getSession(user.getUUID())` — if a FIRE session exists,
    call `session.recordExtinguish(1)`.

### C3. Add `SimulationManager.getSession(UUID)` helper
- `public static SimulationSession getSession(UUID uuid)` — returns the session or null.
- Used by `FireExtinguisherItem` to record extinguishes without coupling to session internals.

### C4. PASS scoring — end-of-simulation report
When `endSimulation` is called and the state is FIRE:
- Send the player a breakdown via `player.sendSystemMessage(...)`:
  ```
  §6--- Fire Drill Results ---
  §eFires extinguished: X
  §aScore: Y / 100
  §7Tip: Remember PASS — Pull, Aim at the base, Squeeze, Sweep side to side.
  ```
- Score formula: `min(100, firesExtinguished * 2)` — 50 fires = full score. Tune later via Config.

### C5. "Aim at the base" — already enforced
- `extinguishAt` only removes fire blocks (the base), not air above flames.
  The mechanic naturally rewards correct aim at no extra code cost.

### C6. Sweep feedback (phase 2, out of scope now)
- Could detect significant yaw change while spraying and bonus-score it.
- Deferred.

---

## Files to change
| File | Change |
|---|---|
| `BerongSMP.java` | Add `.durability(300)` to FIRE_EXTINGUISHER registration |
| `FireExtinguisherItem.java` | Pin state read/write, gate `use()` and `onUseTick`, damage logic, sputtering, updated tooltip, record extinguishes in sessions |
| `SimulationSession.java` | Add `firesExtinguished` counter + accessors |
| `SimulationManager.java` | `getSession()` helper; give pinned extinguisher on FIRE sim start; score report in `endSimulation` |

---

## What NOT to do
- No custom NBT event for fire extinguish (direct session lookup is simpler).
- No refill recipe (scope creep).
- No separate "empty" item variant.
- No sweep detection yet (phase 2).
- Do NOT require sneak to pull the pin — plain right-click is the natural first action.
