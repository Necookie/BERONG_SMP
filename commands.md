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

### `/sim_status [player]`

**Permission:** Any player (self); Op level 2+ for targeting another player  
**Syntax:** `/sim_status` or `/sim_status <player>`

Prints a live snapshot of the specified player's active simulation.

**Output includes:**
- Simulation type (FIRE / EARTHQUAKE)
- Current earthquake phase (RUMBLE / PEAK / AFTERSHOCK / END)
- Time remaining (formatted as M:SS)
- Fires extinguished so far (FIRE sessions)
- Whether the timer is frozen

**Example:**
```
/sim_status               ← check yourself
/sim_status station2      ← instructor checks a student (op only)
```

---

### `/sim_list`

**Permission:** Op level 2+  
**Syntax:** `/sim_list`

Lists **all currently running simulations** across all players on the server. Shows player name, simulation type, time remaining, and whether any timer is frozen.

**When to use:**
- Instructor monitoring multiple stations simultaneously
- Checking whether any orphaned sessions need to be stopped

---

### `/sim_freeze [player]`

**Permission:** Op level 2+  
**Syntax:** `/sim_freeze` or `/sim_freeze <player>`

Pauses the simulation timer for the specified player. The disaster effects (fire spread, earthquake block destruction, camera shake) continue, but the countdown stops. The frozen player is notified.

**When to use:**
- Giving a student a moment to recover without ending the session
- Pausing for an instructor demonstration mid-simulation
- Buying time for a technical issue without losing score data

---

### `/sim_unfreeze [player]`

**Permission:** Op level 2+  
**Syntax:** `/sim_unfreeze` or `/sim_unfreeze <player>`

Resumes a previously frozen simulation timer.

---

### `/sim_time set <seconds>` / `/sim_time add <seconds>`

**Permission:** Op level 2+  
**Syntax:** `/sim_time set <seconds>` or `/sim_time add <seconds>`

| Sub-command | Argument | Range | Effect |
|---|---|---|---|
| `set` | seconds | 0 – 3600 | Replace remaining time with this value |
| `add` | seconds | −3600 – 3600 | Add/subtract seconds from remaining time (negative values reduce time) |

**Examples:**
```
/sim_time set 120        ← set exactly 2 minutes remaining
/sim_time add 30         ← extend by 30 seconds
/sim_time add -60        ← cut a minute off the clock
```

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

These commands manage the student session system — tracking individual students across shared station accounts and persisting results to the Turso cloud database.

**Access:** Either **Op level 2+** OR authenticate with `/bfp login <pin>` (PIN set in `bfpAdminPin` config, default `1234`). OPs always bypass the PIN check.

---

### `/bfp login <pin>`

**Syntax:** `/bfp login <pin>`

Authenticates you as a BFP admin for this session using the PIN set in `bfpAdminPin`. Once logged in, all `/bfp` sub-commands become available to you until you log out or the server restarts.

**Example:** `/bfp login 1234`

---

### `/bfp logout`

**Syntax:** `/bfp logout`

Revokes your PIN-based BFP admin access. Use this when stepping away from the station so others can't run admin commands on your account.

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

### `/bfp tutorial [player]`

**Syntax:** `/bfp tutorial` or `/bfp tutorial <player>`

Resets a player's tutorial state and immediately teleports them to the tutorial lobby — no relog required. Also re-initialises all tutorial NPCs (fixing any duplicates).

**What happens:**
1. `SessionManager.reset` wipes the tutorial save and any active session
2. Tutorial NPCs are re-spawned at their correct positions
3. Target player is teleported to the tutorial spawn

**Examples:**
```
/bfp tutorial
/bfp tutorial station2
```

---

### `/bfp note <text>`

**Syntax:** `/bfp note <text>`

Appends an instructor observation to the current student's active session. Notes are pipe-delimited and stored in the `bfp_notes` column in Turso. Multiple calls append with a ` | ` separator.

**When to use:**
- Recording a student's behaviour during the drill ("panicked at smoke", "tried to exit early")
- Flagging accessibility needs or special circumstances
- Logging anything relevant that the score alone doesn't capture

**Example:**
```
/bfp note student hesitated before pulling the pin
/bfp note forgot to sweep — aimed at flames directly
```

---

### `/bfp confidence <1-5>`

**Syntax:** `/bfp confidence <rating>`

Sets an instructor confidence rating (1.0–5.0) for the student's performance this session. Stored in the `confidence` column.

| Rating | Meaning |
|---|---|
| 1.0 | Very poor — student appeared lost/paralysed |
| 2.0 | Below average — major hesitation or wrong actions |
| 3.0 | Average — followed steps with minor errors |
| 4.0 | Good — competent with only small mistakes |
| 5.0 | Excellent — calm, methodical, fully correct |

**Example:** `/bfp confidence 3.5`

---

### `/bfp prep_level <level>`

**Syntax:** `/bfp prep_level <none|low|moderate|high>`

Sets the instructor's assessment of how prepared the student appeared before the simulation. Stored in `prep_level`.

| Level | Meaning |
|---|---|
| `none` | No apparent prior knowledge |
| `low` | Some awareness, significant gaps |
| `moderate` | Adequate preparation, minor gaps |
| `high` | Well-prepared, recalled steps unprompted |

**Example:** `/bfp prep_level moderate`

---

### `/bfp score <0-100> [player]`

**Syntax:** `/bfp score <value>` or `/bfp score <value> <player>`

Manually overrides the simulation score for the active session. Useful when an instructor wants to award partial credit or correct an automatic scoring error.

**Notes:**
- Clamped to 0–100
- For FIRE sessions the automatic score is `firesExtinguished × 2` (capped at 100). This command overrides that.
- Changes are written to Turso immediately

---

### `/bfp pass [player]`

**Syntax:** `/bfp pass` or `/bfp pass <player>`

Marks the active session as **passed** regardless of the automatic score. Use when a student met the practical standard even if the numeric score doesn't reflect it.

---

### `/bfp fail [player]`

**Syntax:** `/bfp fail` or `/bfp fail <player>`

Marks the active session as **failed**. Use when a student did not meet the standard even if the score is technically high (e.g., they used the wrong extinguisher type).

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

### `/bfp sessions today`

**Syntax:** `/bfp sessions today`

Lists **all sessions started today** (based on server date). Useful for a quick end-of-day review at the end of a lab session.

---

### `/bfp sessions stats`

**Syntax:** `/bfp sessions stats`

Queries **aggregate statistics** from Turso for all completed sessions:
- Total sessions completed
- Number and percentage passed
- Average score
- Fire vs. earthquake session breakdown

**When to use:**
- Quick overview during or after a lab day
- Pre-analysis before exporting the CSV

---

### `/bfp sessions search <query>`

**Syntax:** `/bfp sessions search <query>`

Searches for sessions where the student name **or** station account contains the query string (case-insensitive, partial match). Returns up to 15 matches ordered by most recent.

**Example:**
```
/bfp sessions search Juan
/bfp sessions search station3
```

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

### `/get_co2_extinguisher`

**Permission:** Any player  
**Syntax:** `/get_co2_extinguisher`

Gives you a **CO2 Extinguisher** (Class C) directly into your inventory. Used for putting out burning computer blocks.

**When to use:**
- Testing the CO2 extinguisher mechanics on computer blocks outside of a simulation
- Replacement during an electrical fire scenario
- Instructor demonstrating Class C fire response

**Notes:**
- Right-click on a `BURNING` computer block to extinguish it — the computer will be marked `BROKEN` (cannot be re-used)
- Also suppresses regular fire and soul fire, but the primary use case is electrical fires

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

## Giving Items & Blocks (Vanilla `/give`)

All BerongSMP items and blocks are obtainable via the vanilla `/give` command using their namespaced IDs. No custom command needed.

| Item / Block | `/give` ID | Notes |
|---|---|---|
| Fire Extinguisher | `berongsmp:fire_extinguisher` | Class A/B extinguisher; right-click to pull pin, then spray |
| CO2 Extinguisher | `berongsmp:co2_extinguisher` | Class C extinguisher for electrical fires; targets `BURNING` computer blocks |
| Computer Block | `berongsmp:computer_block` | Placeable; right-click to toggle ON/OFF; use flint & steel to ignite (BURNING state); extinguish with CO2 |

**Examples:**
```
/give @s berongsmp:co2_extinguisher
/give @s berongsmp:computer_block
/give @s berongsmp:fire_extinguisher
```

> `berongsmp:example_block` and `berongsmp:example_item` are leftover template placeholders — ignore them.

---

## Quick Reference Table

| Command | Permission | Notes |
|---|---|---|
| `/sim_fire` | Any player | Start fire simulation, bypasses lobby button |
| `/sim_earthquake [magnitude]` | Any player | Start earthquake simulation, optional magnitude 0.1–10.0 |
| `/sim_stop` | Any player | End active simulation early |
| `/sim_status [player]` | Any player / Op for others | Live snapshot: type, phase, time, fires extinguished |
| `/sim_list` | Op (level 2+) | List all currently active simulations on the server |
| `/sim_freeze [player]` | Op (level 2+) | Pause simulation timer without ending the session |
| `/sim_unfreeze [player]` | Op (level 2+) | Resume a frozen simulation timer |
| `/sim_time set <seconds>` | Op (level 2+) | Set remaining simulation time |
| `/sim_time add <seconds>` | Op (level 2+) | Add/subtract seconds from remaining time |
| `/sim_magnitude <value>` | Op (level 2+) | Live-adjust running earthquake magnitude |
| `/get_extinguisher` | Any player | Gives fire extinguisher item |
| `/get_co2_extinguisher` | Any player | Gives CO2 extinguisher for Class C electrical fires |
| `/spawn_lspu` | Op (level 2+) | Permanent world change — places CCS building |
| `/bfp login <pin>` | Any player | Authenticate as BFP admin using the config PIN |
| `/bfp logout` | Any player | Revoke PIN-based BFP admin access |
| `/bfp checkin <student_name>` | Op or PIN | Start session for caller; wipes tutorial state |
| `/bfp checkin <player> <student_name>` | Op or PIN | Start session for target player |
| `/bfp checkout` | Op or PIN | Finalise and save current session to DB |
| `/bfp reset [player]` | Op or PIN | Wipe tutorial + delete DB row, no record kept |
| `/bfp tutorial [player]` | Op or PIN | Reset tutorial + teleport to tutorial lobby, re-init NPCs |
| `/bfp note <text>` | Op or PIN | Append instructor observation to active session |
| `/bfp confidence <1-5>` | Op or PIN | Set instructor confidence rating (1.0–5.0) |
| `/bfp prep_level <level>` | Op or PIN | Set prep assessment: none / low / moderate / high |
| `/bfp score <0-100> [player]` | Op or PIN | Manually override simulation score |
| `/bfp pass [player]` | Op or PIN | Mark session as passed |
| `/bfp fail [player]` | Op or PIN | Mark session as failed |
| `/bfp session info` | Op or PIN | Print current session details to chat |
| `/bfp sessions list [page]` | Op or PIN | List 10 most recent sessions from DB |
| `/bfp sessions today` | Op or PIN | List all sessions started today |
| `/bfp sessions stats` | Op or PIN | Aggregate stats: total, pass rate, avg score, fire vs quake |
| `/bfp sessions search <query>` | Op or PIN | Search sessions by student name or station account |
| `/bfp sessions export` | Op or PIN | Export all sessions to `run/bfp_sessions_export.csv` |
| `/bfp student <name>` | Op or PIN | Look up last 10 sessions for a student name |

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
| `bfpAdminPin` | `"1234"` | PIN for `/bfp login` — change this before going live |
