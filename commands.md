# BerongSMP Commands Reference

All commands are registered by `ModCommands.java` and available on any server running the mod.
Commands that require a player cannot be run from the server console.

---

## Simulation Commands

### `/sim_fire`

**Permission:** Any player  
**Syntax:** `/sim_fire`

Starts a **Fire simulation** for you immediately, bypassing the lobby button. You are teleported into the LSPU Library arena, the structure is freshly placed, and a fire extinguisher is placed in your hotbar slot 1 with the pin **not yet pulled**.

**When to use:**
- Testing the fire simulation without walking to the lobby button
- Admin/instructor wants to drop a specific student directly into a fire drill

**What happens:**
1. Structures placed (LSPU Library + SSC Building)
2. Player teleported to arena entrance
3. Fire extinguisher given in slot 1 (pin not pulled — remember PASS: Pull, Aim, Squeeze, Sweep)
4. Fire blocks begin spawning every `fireSpawnInterval` ticks
5. Timer counts down from `simDurationTicks`
6. HUD shows `SIMULATION STATUS: FIRE` and time remaining

**Notes:**
- Fails silently if you already have a simulation running — you'll get a "You already have a simulation in progress!" message
- Cannot be run from the server console

---

### `/sim_earthquake`

**Permission:** Any player  
**Syntax:** `/sim_earthquake` or `/sim_earthquake <magnitude>`

Starts an **Earthquake simulation** for you immediately, bypassing the lobby button.

| Argument | Type | Range | Required |
|---|---|---|---|
| `magnitude` | double | 0.1 – 10.0 | No |

If `magnitude` is omitted, the value from `berongsmp-common.toml` (`quakeMagnitude`, default `5.0`) is used.

**When to use:**
- Testing the earthquake simulation without the lobby button
- Running a controlled drill at a specific intensity (e.g., `/sim_earthquake 3.0` for a gentle shake, `/sim_earthquake 9.5` for severe)
- Instructors scripting a lesson where they want a predictable magnitude

**What happens:**
1. Structures placed (LSPU Library + SSC Building)
2. A random **epicenter** is chosen within the arena
3. Player teleported to arena entrance
4. Earthquake message displayed: `⚠ Magnitude X.X Earthquake has begun! Brace for impact!`
5. Phase state machine begins: **RUMBLE → PEAK → AFTERSHOCK → END**
   - **RUMBLE** (default 5 s): Low-intensity random block destruction within `magnitude × 3` blocks of epicenter. Camera begins slow oscillation.
   - **PEAK** (default 10 s): Maximum intensity. Unsupported blocks (nothing below them) are queued closest-to-epicenter first and break gradually every tick — creates a cascading structural collapse outward. Camera shake is strongest here.
   - **AFTERSHOCK** (default 5 s): Half the break count, smaller radius. Camera shake fades.
   - **END**: No more destruction. Phase machine stops.
6. Fire that spreads naturally during block destruction is cleared every second
7. HUD shows `SIMULATION STATUS: EARTHQUAKE` + time remaining + camera shake intensity

**Magnitude scale reference:**

| Magnitude | Feel |
|---|---|
| 0.1 – 2.0 | Barely noticeable, minimal destruction |
| 3.0 – 4.9 | Moderate — some blocks fall, light camera shake |
| 5.0 | Default config value — balanced for gameplay |
| 6.0 – 7.9 | Strong — significant collapse, pronounced shake |
| 8.0 – 10.0 | Severe — rapid widespread destruction, heavy shake |

**Notes:**
- The lobby button also starts this simulation but with a **random magnitude between 6.0 and 9.5** (strong range), so button-triggered quakes are always intense
- Fails if you already have an active simulation

---

### `/sim_stop`

**Permission:** Any player  
**Syntax:** `/sim_stop`

Ends your active simulation early. Behaves identically to the timer expiring naturally.

**When to use:**
- Ending a drill early (session went wrong, player is stuck, admin intervention)
- Resetting the arena to a clean state before starting a new session
- Can also be called from the **server console** to stop any running simulation by a specific player — though note the console version still resolves to the calling player

**What happens:**
1. Session removed
2. LSPU Library + SSC Building structures restored to original state
3. HUD cleared
4. Player teleported back to lobby spawn
5. If it was a FIRE session, score is printed: fires extinguished and score out of 100

**Notes:**
- Safe to call even if no simulation is running (no-op)
- If the player died during the simulation, the structure is still restored but the teleport is deferred to their next respawn

---

## Earthquake Tuning Commands

### `/sim_magnitude <value>`

**Permission:** Op level 2+ (game master) only  
**Syntax:** `/sim_magnitude <value>`

| Argument | Type | Range | Required |
|---|---|---|---|
| `value` | double | 0.1 – 10.0 | Yes |

Changes the **magnitude of your currently active earthquake session live**, taking effect on the very next tick. Does not restart the simulation or reset the phase.

**When to use:**
- Instructor wants to dial up intensity mid-session to demonstrate a stronger quake
- Gradually increase magnitude as a teaching progression without stopping and restarting
- Dial back an accidental high-magnitude start so players aren't overwhelmed

**Example flow:**
```
/sim_earthquake 3.0        ← start mild for explanation
/sim_magnitude 6.0         ← escalate during PEAK phase for impact
/sim_magnitude 9.0         ← push to severe for demonstration
/sim_stop                  ← end when done
```

**Notes:**
- Fails with `No active earthquake simulation to adjust.` if you have no running earthquake session (works only for EARTHQUAKE, not FIRE)
- Restricted to ops — regular players cannot use this to make the simulation easier or harder

---

## BFP Admin Commands

All `/bfp` sub-commands require **Op level 2+**. They manage the student session system — tracking individual students across shared station accounts and persisting results to the Turso cloud database.

---

### `/bfp checkin <student_name>`

**Syntax:** `/bfp checkin <student_name>` or `/bfp checkin <player> <student_name>`

Starts a new session for a student sitting at the station.

**What happens:**
1. Any existing session for that account is closed and saved
2. Tutorial saved data for the account UUID is wiped (fresh start)
3. A new row is inserted into Turso with `status='active'`

**Examples:**
```
/bfp checkin Juan dela Cruz
/bfp checkin station2 Maria Santos
```

---

### `/bfp checkout`

**Syntax:** `/bfp checkout`

Finalises the calling player's current session and writes `end_time` + `status='completed'` to Turso. Use this when a student finishes and leaves without triggering a simulation end.

---

### `/bfp reset [player]`

**Syntax:** `/bfp reset` or `/bfp reset <player>`

Wipes tutorial state and **deletes** the active DB row — no record is kept. Use when a session was started by mistake or needs to be discarded.

---

### `/bfp session info`

**Syntax:** `/bfp session info`

Prints the current session details to chat:
- Student name, station account
- Session start time
- Whether the tutorial is complete
- Simulation type, score, and pass/fail

---

### `/bfp sessions list [page]`

**Syntax:** `/bfp sessions list` or `/bfp sessions list <page>`

Lists the 10 most recent sessions from Turso in chat, paginated. Each entry shows student name, account, simulation type, score, pass status, and session status.

---

### `/bfp sessions export`

**Syntax:** `/bfp sessions export`

Queries all sessions from Turso and writes them to `run/bfp_sessions_export.csv`. Columns: `id, student_name, station_account, account_uuid, start_time, end_time, status, tutorial_completed, tutorial_duration_s, simulation_type, simulation_score, passed, notes`.

---

### `/bfp student <name>`

**Syntax:** `/bfp student <name>`

Looks up the last 10 sessions for a given student name and prints them to chat. Useful for checking a student's history across multiple attempts.

**Example:**
```
/bfp student Juan dela Cruz
```

---

## Utility Commands

### `/get_extinguisher`

**Permission:** Any player  
**Syntax:** `/get_extinguisher`

Gives you a **Fire Extinguisher** item directly into your inventory (not pinned to slot 1, added to first available slot).

**When to use:**
- You dropped or lost the extinguisher during a fire simulation and need a replacement
- Testing the extinguisher mechanics outside of a simulation
- Instructors demonstrating how the extinguisher works before a drill

**Notes:**
- The extinguisher given by `/sim_fire` starts with the pin NOT pulled. This one is also unpulled — right-click once to pull the pin (the "P" in PASS), then right-click again to spray
- Can be used at any time, simulation or not

---

### `/spawn_lspu`

**Permission:** Op level 2+ (game master) only  
**Syntax:** `/spawn_lspu`

Spawns the **LSPU CCS Building** at your current position using the `CCSBuildingConstructor`. This is a procedurally constructed building, not the NBT simulation arena.

**When to use:**
- Dev/admin use: placing the CCS building at an arbitrary location for inspection or world-building
- Testing structure generation without running a full simulation

**Notes:**
- Places the building at your exact block position — make sure you're standing where you want the structure's origin
- This is a permanent world modification, not tied to any simulation session and not restored by `/sim_stop`
- Restricted to ops

---

## Quick Reference Table

| Command | Permission | Notes |
|---|---|---|
| `/sim_fire` | Any player | Start fire simulation, bypasses lobby button |
| `/sim_earthquake [magnitude]` | Any player | Start earthquake simulation, optional magnitude 0.1–10.0 |
| `/sim_stop` | Any player | End active simulation early |
| `/sim_magnitude <value>` | Op (level 2+) | Live-adjust running earthquake magnitude |
| `/get_extinguisher` | Any player | Gives fire extinguisher item |
| `/spawn_lspu` | Op (level 2+) | Permanent world change — places CCS building |
| `/bfp checkin <student_name>` | Op (level 2+) | Start session for caller; wipes tutorial state |
| `/bfp checkin <player> <student_name>` | Op (level 2+) | Start session for target player |
| `/bfp checkout` | Op (level 2+) | Finalise and save current session to DB |
| `/bfp reset [player]` | Op (level 2+) | Wipe tutorial + delete DB row, no record kept |
| `/bfp session info` | Op (level 2+) | Print current session details to chat |
| `/bfp sessions list [page]` | Op (level 2+) | List 10 most recent sessions from DB |
| `/bfp sessions export` | Op (level 2+) | Export all sessions to `run/bfp_sessions_export.csv` |
| `/bfp student <name>` | Op (level 2+) | Look up last 10 sessions for a student name |

*`/sim_stop` accepts console input but the stop targets the calling player — from console it resolves to no player UUID, so use it in-game.

---

## Config Defaults Referenced Above

These values live in `run/config/berongsmp-common.toml` and are hot-reloadable (no restart needed):

| Key | Default | Affects |
|---|---|---|
| `simDurationTicks` | 2400 (2 min) | How long any simulation runs |
| `quakeMagnitude` | 5.0 | Default magnitude for `/sim_earthquake` with no arg |
| `quakeRumbleDuration` | 100 ticks (5 s) | Length of RUMBLE phase |
| `quakePeakDuration` | 200 ticks (10 s) | Length of PEAK phase |
| `quakeAftershockDuration` | 100 ticks (5 s) | Length of AFTERSHOCK phase |
| `quakeBreakCount` | 2 | Blocks destroyed per quake interval |
| `quakeInterval` | 10 ticks | How often earthquake effect fires |
| `fireSpawnCount` | 3 | Fire blocks placed per spawn interval |
| `fireSpawnInterval` | 20 ticks (1 s) | How often fire is placed |
| `tursoUrl` | `""` | Turso HTTPS database URL for session tracking |
| `tursoToken` | `""` | Turso Bearer auth token |
| `passThresholdFire` | 5 | Fires extinguished required to mark a session as passed |
