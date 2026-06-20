# Phase 3 — Synthetic Data Generator

> **Status:** `[ ] not started`
> **Repo:** `berongsmp-template-26.1.2` (`scripts/` folder)
> **Depends on:** Phase 1 Turso schema finalized
> **Purpose:** Give groupmate labeled training data for the Random Forest classifier before real users run sessions.
> **Update `docs/major_plan.md` when complete, push to main.**

---

## Context

The RF classifier (trained by a separate team member) needs labeled examples of:
- **HIGH preparedness**: fast decision, accurate spraying, efficient path, calm movement
- **MODERATE preparedness**: average on most metrics, some hesitation
- **LOW preparedness**: slow to act, poor accuracy, erratic movement, stays near fire

The script writes directly to Turso via HTTP (same REST API the mod uses) and pre-labels each session with `prep_level`.

The 6 features the RF model will use:
| Feature | JSON field in event_log |
|---|---|
| `decision_delay_s` | Time from SIM_START to first EXT_PIN_PULL |
| `spray_accuracy` | EXT_SPRAY hits / total EXT_SPRAY |
| `path_efficiency` | straight_dist / cumulative_dist from PLAYER_TICK |
| `hazard_proximity_ratio` | ticks with nearest_fire_dist < 3 / total ticks |
| `interaction_frequency` | EXT_SPRAY count / duration_s |
| `panic_proxy` | variance of dx²+dz² between PLAYER_TICK rows |

---

## File: `scripts/generate_synthetic_data.py`

### Setup
```bash
pip install requests python-dotenv
```

Create `scripts/.env`:
```
TURSO_URL=https://yourdb-yourorg.turso.io
TURSO_TOKEN=your-token-here
```

### Profile Definitions

```python
PROFILES = {
    "HIGH": {
        "decision_delay_s": (3, 8),        # pulls pin fast
        "spray_accuracy": (0.75, 0.95),    # accurate sprayer
        "path_efficiency": (0.70, 0.90),   # direct movement
        "hazard_proximity_ratio": (0.0, 0.10),  # avoids fire
        "interaction_freq": (0.8, 1.5),    # sprays often when near fire
        "panic_proxy": (0.1, 0.5),         # calm, deliberate movement
        "score_range": (75, 100),
    },
    "MODERATE": {
        "decision_delay_s": (10, 25),
        "spray_accuracy": (0.45, 0.74),
        "path_efficiency": (0.45, 0.69),
        "hazard_proximity_ratio": (0.10, 0.25),
        "interaction_freq": (0.4, 0.79),
        "panic_proxy": (0.5, 1.5),
        "score_range": (40, 74),
    },
    "LOW": {
        "decision_delay_s": (30, 90),      # slow to act
        "spray_accuracy": (0.10, 0.44),    # misses a lot
        "path_efficiency": (0.15, 0.44),   # wanders
        "hazard_proximity_ratio": (0.25, 0.60),  # stays near fire
        "interaction_freq": (0.1, 0.39),
        "panic_proxy": (1.5, 5.0),         # erratic movement
        "score_range": (0, 39),
    },
}
```

### Event Log Generation

For each synthetic session, generate a realistic `event_log` array:

```python
def generate_event_log(profile, duration_s=120, sim_type="FIRE"):
    events = []
    t = 0  # ms offset

    # SIM_START
    events.append({"type": "SIM_START", "tOffsetMs": 0,
                   "data": {"sim_type": sim_type}})

    # Decision delay before EXT_PIN_PULL
    delay = random.uniform(*profile["decision_delay_s"]) * 1000
    t = delay
    events.append({"type": "EXT_PIN_PULL", "tOffsetMs": t,
                   "data": {"pulled": True}})

    # Generate PLAYER_TICK every 1000ms
    x, z = 35.0, 88.0  # near SIM_POS
    panic = random.uniform(*profile["panic_proxy"])
    for sec in range(duration_s):
        dx = random.gauss(0, panic)
        dz = random.gauss(0, panic)
        x += dx; z += dz
        nearest_fire = random.uniform(
            1.0 if profile["hazard_proximity_ratio"][0] > 0.2 else 3.0, 8.0)
        events.append({"type": "PLAYER_TICK", "tOffsetMs": sec * 1000,
                       "data": {"x": round(x,1), "y": -32.0, "z": round(z,1),
                                "room": "COMPUTER_LAB" if sec < 30 else "MAIN_HALL",
                                "nearest_fire_dist": round(nearest_fire, 2)}})

    # Generate EXT_SPRAY events
    spray_count = int(random.uniform(*profile["interaction_freq"]) * duration_s / 10)
    for _ in range(spray_count):
        hit = random.random() < random.uniform(*profile["spray_accuracy"])
        events.append({"type": "EXT_SPRAY", "tOffsetMs": random.uniform(delay, duration_s*1000),
                       "data": {"hit_fire": hit,
                                "distance_to_fire": round(random.uniform(1.5, 5.0), 2),
                                "fire_blocks_in_fov": random.randint(1, 8)}})

    # SIM_END
    score = random.randint(*profile["score_range"])
    events.append({"type": "SIM_END", "tOffsetMs": duration_s * 1000,
                   "data": {"score": score, "passed": score >= 50}})

    return sorted(events, key=lambda e: e["tOffsetMs"]), score
```

### Writing to Turso

Use the same `/v2/pipeline` endpoint as `TursoClient.java`:

```python
import requests, json, os
from dotenv import load_dotenv

load_dotenv()
URL = os.environ["TURSO_URL"] + "/v2/pipeline"
TOKEN = os.environ["TURSO_TOKEN"]

def turso_execute(statements):
    resp = requests.post(URL,
        headers={"Authorization": f"Bearer {TOKEN}", "Content-Type": "application/json"},
        json={"requests": [{"type": "execute", "stmt": s} for s in statements]
              + [{"type": "close"}]})
    resp.raise_for_status()
    return resp.json()

def insert_session(name, student_id, section, sim_type, score, passed, prep_level, event_log_json):
    from datetime import datetime, timedelta
    start = datetime.utcnow() - timedelta(minutes=random.randint(5, 60))
    end   = start + timedelta(seconds=120)
    turso_execute([
        {"sql": """INSERT INTO sessions
          (student_name, student_id, section, station_account, account_uuid,
           start_time, end_time, status, tutorial_completed, tutorial_duration_s,
           simulation_type, simulation_score, passed, event_log, prep_level)
          VALUES (?,?,?,?,?,?,?,'completed',1,?,?,?,?,?,?)""",
         "args": [
           {"type":"text","value": name},
           {"type":"text","value": student_id},
           {"type":"text","value": section},
           {"type":"text","value": "synthetic_station"},
           {"type":"text","value": f"synthetic-{random.randint(1000,9999)}"},
           {"type":"text","value": start.isoformat()},
           {"type":"text","value": end.isoformat()},
           {"type":"integer","value": random.randint(60,180)},
           {"type":"text","value": sim_type},
           {"type":"integer","value": score},
           {"type":"integer","value": 1 if passed else 0},
           {"type":"text","value": event_log_json},
           {"type":"text","value": prep_level},
         ]}
    ])
```

### Main Generation Loop

Generate 20 sessions per profile (60 total):
```python
NAMES = ["Ana Reyes","Ben Cruz","Carlo Santos","Diana Lim","Erik Tan",
         "Faye Ong","Gab Flores","Hannah Go","Ivan Sy","Julia Ramos"]

for prep_level, profile in PROFILES.items():
    for i in range(20):
        name = random.choice(NAMES) + f" {i}"
        student_id = f"2021-{10000+i}"
        section = random.choice(["BSIT-3A","BSIT-3B","BSCS-3A"])
        sim_type = random.choice(["FIRE","EARTHQUAKE"])
        events, score = generate_event_log(profile, sim_type=sim_type)
        passed = score >= 50
        insert_session(name, student_id, section, sim_type, score, passed,
                       prep_level, json.dumps(events))
        print(f"[{prep_level}] {name} score={score}")

print("Done — 60 synthetic sessions written to Turso.")
```

---

## `scripts/README.md`

Document for groupmate:
- How to run: `cd scripts && pip install -r requirements.txt && python generate_synthetic_data.py`
- What columns to read: `event_log` (parse JSON), `prep_level` (training label)
- The 6 features to extract from `event_log`
- Formula for each feature (see `docs/major_plan.md` Phase 3 section)
- Expected output: `prep_level` column + `confidence` float written back to `sessions` table

---

## Verification

1. `python scripts/generate_synthetic_data.py`
2. Query Turso: `SELECT prep_level, COUNT(*) FROM sessions GROUP BY prep_level`
3. Should show ~20 HIGH, ~20 MODERATE, ~20 LOW
4. Check dashboard `/sessions` — 60 sessions visible with section filters working
5. Parse one `event_log` manually — should have PLAYER_TICK, EXT_SPRAY, EXT_PIN_PULL entries

When done: update `docs/major_plan.md` Phase 3 → `[x] done`, push to main.
