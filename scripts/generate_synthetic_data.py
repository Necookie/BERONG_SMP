"""
BerongSMP — Synthetic Training Data Generator
Writes 60 labeled sessions (20 HIGH / 20 MODERATE / 20 LOW) directly to
Turso so the RF classifier has examples before real users run drills.

Setup:
  pip install -r requirements.txt
  cp .env.example .env   # fill in TURSO_URL and TURSO_TOKEN

Usage:
  python generate_synthetic_data.py [--dry-run]
"""

import json
import os
import random
import sys
from datetime import datetime, timedelta, timezone

import requests
from dotenv import load_dotenv

load_dotenv()

TURSO_URL   = os.environ.get("TURSO_URL", "").rstrip("/")
TURSO_TOKEN = os.environ.get("TURSO_TOKEN", "")

DRY_RUN = "--dry-run" in sys.argv

# ---------------------------------------------------------------------------
# Preparedness profiles
# ---------------------------------------------------------------------------

PROFILES = {
    "HIGH": {
        "decision_delay_s":       (3,  8),
        "spray_accuracy":         (0.75, 0.95),
        "path_efficiency":        (0.70, 0.90),
        "hazard_proximity_ratio": (0.00, 0.10),
        "interaction_freq":       (0.8, 1.5),
        "panic_proxy":            (0.1, 0.5),
        "score_range":            (75, 100),
    },
    "MODERATE": {
        "decision_delay_s":       (10, 25),
        "spray_accuracy":         (0.45, 0.74),
        "path_efficiency":        (0.45, 0.69),
        "hazard_proximity_ratio": (0.10, 0.25),
        "interaction_freq":       (0.4, 0.79),
        "panic_proxy":            (0.5, 1.5),
        "score_range":            (40, 74),
    },
    "LOW": {
        "decision_delay_s":       (30, 90),
        "spray_accuracy":         (0.10, 0.44),
        "path_efficiency":        (0.15, 0.44),
        "hazard_proximity_ratio": (0.25, 0.60),
        "interaction_freq":       (0.1, 0.39),
        "panic_proxy":            (1.5, 5.0),
        "score_range":            (0,  39),
    },
}

FIRE_ROOMS  = ["COMPUTER_LAB", "MAIN_HALL", "ENTRANCE"]
QUAKE_ROOMS = ["MAIN_HALL", "STAIRWELL", "UPPER_FLOOR", "ENTRANCE"]

NAMES = [
    "Ana Reyes", "Ben Cruz", "Carlo Santos", "Diana Lim", "Erik Tan",
    "Faye Ong", "Gab Flores", "Hannah Go", "Ivan Sy", "Julia Ramos",
    "Karl Dela Cruz", "Lena Bautista", "Marco Villanueva", "Nina Aquino",
    "Oscar Mendoza",
]

SECTIONS = ["BSIT-3A", "BSIT-3B", "BSCS-3A", "BSCS-3B"]

# ---------------------------------------------------------------------------
# Event log generation
# ---------------------------------------------------------------------------

def generate_fire_log(profile: dict, duration_s: int = 120):
    """Generate a realistic FIRE session event log."""
    events = []
    decision_delay_ms = random.uniform(*profile["decision_delay_s"]) * 1000
    panic = random.uniform(*profile["panic_proxy"])

    events.append({"type": "SIM_START", "tOffsetMs": 0,
                   "data": {"sim_type": "FIRE"}})

    # Pin pull after decision delay
    events.append({"type": "EXT_PIN_PULL", "tOffsetMs": decision_delay_ms,
                   "data": {"pulled": True}})

    # Player ticks every 1 s
    x, z = 35.0 + random.uniform(-2, 2), 88.0 + random.uniform(-2, 2)
    rooms = FIRE_ROOMS
    for sec in range(duration_s):
        dx = random.gauss(0, panic * 0.4)
        dz = random.gauss(0, panic * 0.4)
        x = max(30, min(55, x + dx))
        z = max(83, min(108, z + dz))
        prox_lo, prox_hi = (1.0, 3.5) if random.random() < profile["hazard_proximity_ratio"][1] else (3.5, 8.0)
        nearest_fire = round(random.uniform(prox_lo, prox_hi), 2)
        events.append({"type": "PLAYER_TICK", "tOffsetMs": sec * 1000,
                       "data": {"x": round(x, 1), "y": -32.0, "z": round(z, 1),
                                "room": random.choice(rooms),
                                "nearest_fire_dist": nearest_fire}})

    # Spray events (after pin pull)
    spray_count = max(1, int(random.uniform(*profile["interaction_freq"]) * duration_s / 10))
    for _ in range(spray_count):
        hit = random.random() < random.uniform(*profile["spray_accuracy"])
        t_ms = random.uniform(decision_delay_ms, duration_s * 1000)
        events.append({"type": "EXT_SPRAY", "tOffsetMs": t_ms,
                       "data": {"hit_fire": hit,
                                "distance_to_fire": round(random.uniform(1.5, 5.0), 2),
                                "fire_blocks_in_fov": random.randint(1, 8)}})

    # Fire spread events
    spread_count = random.randint(3, 12)
    for i in range(spread_count):
        t_ms = random.uniform(0, duration_s * 1000)
        fx = round(random.uniform(30, 55), 1)
        fz = round(random.uniform(83, 108), 1)
        events.append({"type": "FIRE_SPREAD", "tOffsetMs": t_ms,
                       "data": {"pos": f"{fx},{-33},{fz}",
                                "total_spread_count": i + 1}})

    score = random.randint(*profile["score_range"])
    passed = score >= 50
    events.append({"type": "SIM_END", "tOffsetMs": duration_s * 1000,
                   "data": {"fires_extinguished": spray_count // 3,
                            "score": score, "passed": passed}})

    events.sort(key=lambda e: e["tOffsetMs"])
    return events, score, passed


def generate_earthquake_log(profile: dict, duration_s: int = 120):
    """Generate a realistic EARTHQUAKE session event log.

    Decision delay = time to first crouch (QUAKE_DROP reaction).
    No spray events; path efficiency and proximity still apply.
    """
    events = []
    decision_delay_ms = random.uniform(*profile["decision_delay_s"]) * 1000
    panic = random.uniform(*profile["panic_proxy"])
    magnitude = round(random.uniform(5.5, 8.5), 1)

    events.append({"type": "SIM_START", "tOffsetMs": 0,
                   "data": {"sim_type": "EARTHQUAKE", "magnitude": magnitude}})

    events.append({"type": "QUAKE_DROP_REACT", "tOffsetMs": decision_delay_ms,
                   "data": {"crouched": True, "delay_ms": round(decision_delay_ms)}})

    x, z = 35.0 + random.uniform(-2, 2), 88.0 + random.uniform(-2, 2)
    for sec in range(duration_s):
        dx = random.gauss(0, panic * 0.5)
        dz = random.gauss(0, panic * 0.5)
        x = max(30, min(55, x + dx))
        z = max(83, min(108, z + dz))
        events.append({"type": "PLAYER_TICK", "tOffsetMs": sec * 1000,
                       "data": {"x": round(x, 1), "y": -32.0, "z": round(z, 1),
                                "room": random.choice(QUAKE_ROOMS),
                                "nearest_fire_dist": 9.9}})  # no fire in quake

    cover_ok = random.random() < (0.9 if profile["panic_proxy"][1] < 1.0 else 0.4)
    events.append({"type": "QUAKE_COVER", "tOffsetMs": decision_delay_ms + random.uniform(500, 3000),
                   "data": {"has_cover": cover_ok}})

    score = random.randint(*profile["score_range"])
    passed = score >= 50
    events.append({"type": "SIM_END", "tOffsetMs": duration_s * 1000,
                   "data": {"score": score, "passed": passed, "magnitude": magnitude}})

    events.sort(key=lambda e: e["tOffsetMs"])
    return events, score, passed

# ---------------------------------------------------------------------------
# Turso HTTP
# ---------------------------------------------------------------------------

def turso_execute(statements: list[dict]):
    if DRY_RUN:
        return None
    if not TURSO_URL or not TURSO_TOKEN:
        raise ValueError("TURSO_URL and TURSO_TOKEN must be set in .env")
    resp = requests.post(
        TURSO_URL + "/v2/pipeline",
        headers={"Authorization": f"Bearer {TURSO_TOKEN}",
                 "Content-Type": "application/json"},
        json={"requests": [{"type": "execute", "stmt": s} for s in statements]
              + [{"type": "close"}]},
        timeout=15,
    )
    resp.raise_for_status()
    return resp.json()


def insert_session(
    name: str, student_id: str, section: str,
    sim_type: str, score: int, passed: bool,
    prep_level: str, event_log_json: str,
    tutorial_duration_s: int,
):
    now   = datetime.now(timezone.utc)
    start = now - timedelta(days=random.randint(0, 14),
                            minutes=random.randint(5, 120))
    end   = start + timedelta(seconds=120)
    stmt  = {
        "sql": """
            INSERT INTO sessions
              (student_name, student_id, section, station_account, account_uuid,
               start_time, end_time, status, tutorial_completed, tutorial_duration_s,
               simulation_type, simulation_score, passed, event_log, prep_level)
            VALUES (?,?,?,?,?,?,?,'completed',1,?,?,?,?,?,?)
        """,
        "args": [
            {"type": "text",    "value": name},
            {"type": "text",    "value": student_id},
            {"type": "text",    "value": section},
            {"type": "text",    "value": "synthetic_station"},
            {"type": "text",    "value": f"synthetic-{random.randint(100000,999999)}"},
            {"type": "text",    "value": start.isoformat()},
            {"type": "text",    "value": end.isoformat()},
            {"type": "integer", "value": tutorial_duration_s},
            {"type": "text",    "value": sim_type},
            {"type": "integer", "value": score},
            {"type": "integer", "value": 1 if passed else 0},
            {"type": "text",    "value": event_log_json},
            {"type": "text",    "value": prep_level},
        ],
    }
    turso_execute([stmt])

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

SESSIONS_PER_LEVEL = 20
FIRE_RATIO         = 0.6   # 60% FIRE, 40% EARTHQUAKE per level

def main():
    if DRY_RUN:
        print("[dry-run] No data will be written to Turso.\n")

    total = 0
    for prep_level, profile in PROFILES.items():
        for i in range(SESSIONS_PER_LEVEL):
            name       = random.choice(NAMES) + f" {i+1}"
            student_id = f"2021-{10000 + total}"
            section    = random.choice(SECTIONS)
            sim_type   = "FIRE" if random.random() < FIRE_RATIO else "EARTHQUAKE"
            tut_s      = random.randint(60, 240)

            if sim_type == "FIRE":
                events, score, passed = generate_fire_log(profile)
            else:
                events, score, passed = generate_earthquake_log(profile)

            event_log_json = json.dumps(events)
            insert_session(name, student_id, section, sim_type,
                           score, passed, prep_level, event_log_json, tut_s)

            total += 1
            tag = "[DRY]" if DRY_RUN else "[OK ]"
            print(f"{tag} [{prep_level:8s}] {name:25s}  {sim_type:10s}  score={score:3d}  "
                  f"events={len(events)}")

    print(f"\n{'Dry-run complete' if DRY_RUN else 'Done'} — {total} synthetic sessions "
          f"({'not ' if DRY_RUN else ''}written to Turso.")


if __name__ == "__main__":
    main()
