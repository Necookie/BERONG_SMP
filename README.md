# BerongSMP: AI-Powered Disaster Risk Simulation and Preparedness Training

> **Thesis Research:** Minecraft as a Platform for AI-Enhanced Disaster Risk Simulation and Adaptive Educational Preparedness  
> **Conducted by:** Dheyn Michael Orlanda, Francis Neil Mistica, Ian Patrick Messias  
> **Academic Institution:** Laguna State Polytechnic University (LSPU)  
> **Partner Agency:** Bureau of Fire Protection (BFP) Sta. Cruz, Laguna  

---

## Executive Summary

Disaster preparedness education faces a persistent challenge: traditional drills, instructional videos, and classroom seminars cannot safely recreate the intensity, hazards, or split-second decision-making required during a real emergency. Conventional assessments usually rely on self-report questionnaires or simple headcount drills, which reveal very little about how an individual actually behaves when confronted with smoke, heat, structural tremors, or spreading fire.

BerongSMP bridges this critical educational gap. Built as an advanced NeoForge mod for Minecraft Java Edition, the platform transforms the game into a realistic, interactive disaster preparedness laboratory. Students navigate custom-designed training facilities and high-stakes emergency simulations modeled after real-world academic buildings and disaster risk management protocols.

Throughout each session, an in-game telemetry engine silently captures fine-grained behavioral data, including reaction times, extinguisher selection, PASS technique adherence, hazard mitigation, spatial navigation, and compliance with protective postures. This data streams directly to a cloud database and an external machine learning pipeline, producing objective, data-driven assessments of disaster preparedness.

---

## Core Educational Modules

Developed in close consultation with the Bureau of Fire Protection (BFP), BerongSMP organizes learning into two complementary experiences: foundational guided training in the Academy and an unguided, graded practical evaluation in New Sim Building 2.0.

### 1. The Academy (Guided Training Facility)

The Academy introduces trainees to disaster safety concepts through a four-room progression. Each room is supervised by a dedicated NPC instructor equipped with custom voice acting and interactive drills:

* **Room 1: Orientation and Reaction (Officer Cruz)**  
  Trainees receive a comprehensive briefing on situational awareness, emergency signals, and hazard observation. The room features an interactive go-and-stop drill that calibrates trainee reaction times and attentiveness.

* **Room 2: Fire Safety and Personal Survival (Sgt. Reyes)**  
  Trainees master the standard PASS protocol (Pull the pin, Aim at the base of the fire, Squeeze the lever, Sweep side to side) using functional fire extinguishers. Sgt. Reyes also teaches the essential Stop, Drop, and Roll technique, enabling trainees to extinguish themselves if they catch fire in-game.

* **Room 3: Earthquake Preparedness (Sgt. Santos)**  
  Trainees practice the Duck, Cover, and Hold on procedure during simulated seismic tremors. The system verifies that players crouch underneath sturdy furniture with solid overhead protection while warning against falling structural debris and hazards.

* **Room 4: Certification Evaluation (Capt. Morfe)**  
  Capt. Morfe administers a comprehensive practical examination combining fire response and safety protocols. Achieving a passing score grants official certification, unlocking the live simulation and automatically deploying the trainee after a brief countdown.

### 2. New Sim Building 2.0 (The Graded Practical Scenario)

Once certified, trainees enter New Sim Building 2.0, an extensive two-story institutional facility containing classrooms, science laboratories, administrative offices, and a cafeteria. The graded scenario unfolds across three operational phases:

* **Prevention Phase**  
  Trainees inspect the facility to identify and neutralize active fire hazards before they ignite. Hazards include overloaded electrical outlets, frayed cables, unventilated machinery, and improperly stored flammable materials. Quick thinking during this phase prevents minor issues from escalating.

* **Intervention Phase**  
  Any hazard that is neglected or fails will ignite into active flames. Dynamic fire propagation physics cause the fire to spread outward into nearby blocks. Trainees must choose the appropriate extinguisher for each fire class:
  * Dry chemical extinguishers for ordinary combustible materials and paper.
  * Carbon dioxide extinguishers for energized electrical appliances and lab equipment.
  * Wet chemical extinguishers for commercial kitchen grease fires.  
  Using an incompatible extinguisher is ineffective and produces direct feedback, reinforcing standard fire science principles.

* **Evacuation Phase**  
  As building alarms sound or time limits approach, trainees must prioritize personal safety. They must navigate smoke-obscured corridors, locate emergency exits, avoid blocked pathways, and safely assemble at designated outdoor safe zones.

Every run concludes with an automated performance debriefing and a full score breakdown.

---

## System Architecture and Telemetry Pipeline

BerongSMP functions as the technical data generation engine within a broader multi-tier research ecosystem:

```
[Student Trainee in Minecraft Client]
             │
             │ Registers via /register <name> <student id> <section>
             ▼
[BerongSMP NeoForge Server]
      ├── Custom Hazard and Furniture Registry (85+ unique props)
      ├── Mechanics Engines (PASS spray, Stop-Drop-and-Roll, Duck-Cover-Hold)
      ├── Real-Time Event Logger (buffers movements, actions, timestamps)
      └── Cloud REST Client
             │
             │ Sends batch telemetry upon session conclusion
             ▼
[Turso libSQL Cloud Database]
      ├── Student registration registry and session records
      └── Granular event logs, movement traces, and fire interactions
             │
             ├─────────────────────────────────────────┐
             ▼                                         ▼
[Machine Learning Analytics Pipeline]      [Web Evaluation Dashboard]
      * Random Forest classifier                 * Astro SSR interface
      * Extracts behavioral features             * Visualizes event logs and replays
      * Computes preparedness classification     * Displays scores for BFP officers
```

### Telemetry Features Collected
The mod logs high-frequency, timestamped data points that reflect real preparedness:
* **Initial decision latency:** Elapsed time between alarm activation and first physical action.
* **Extinguisher selection accuracy:** Frequency of correct agent pairing across different fire classes.
* **PASS technique fidelity:** Verification of pin pull, nozzle aiming, and sweep coverage.
* **Stop, Drop, and Roll compliance:** Speed of response and key sequence when catching fire.
* **Structural hazard avoidance:** Time spent in proximity to falling debris or toxic smoke plumes.
* **Evacuation efficiency:** Path directness from alarm onset to outdoor assembly point.

---

## Custom Gameplay Features and Engineering

* **Extensive Hazard Catalog:** Over 85 custom hazard blocks and realistic furniture props designed for school, office, kitchen, and laboratory settings.
* **Physical Fire Extinguishers:** Realistic multi-step extinguisher handling with pullable safety pins, custom spray particle cones, and class-specific chemical agents.
* **Dedicated Movement Mechanics:** Custom Stop, Drop, and Roll animation and crawling system; Duck, Cover, and Hold detection with solid overhead cover validation.
* **Dynamic Acoustic Alarms:** Authentic directional fire alarm soundscapes that reverberate throughout facility wings.
* **Automated Arena Management:** Schematic-based building loaders that clean and reset structural arenas between sessions, preventing overlapping runs and duplicate entities.
* **Voice-Acted NPC Cadre:** Realistic instructor dialogue powered by modern voice synthesis tools to maintain high training immersion.

---

## Technical Specifications and Prerequisites

* **Minecraft Version:** Java Edition 26.2.0
* **Mod Loader:** NeoForge 26.2.0.0-beta
* **Java Development Kit:** Java 25 (OpenJDK 25)
* **Build System:** Gradle with the official NeoForge development kit
* **Database Backend:** Turso (libSQL) cloud database (optional for offline testing, required for persistent telemetry)

---

## Getting Started (Development and Testing)

### Building the Project

Clone this repository and use the included Gradle wrapper to build the mod JAR:

```bash
# Compile the mod JAR into build/libs/
./gradlew build

# Fast compilation check
./gradlew compileJava
```

### Running Locally

```bash
# Launch a headless development server (stores world data in run/)
./gradlew runServer

# Launch the Minecraft development client
./gradlew runClient

# Generate data assets and blockstates
./gradlew runData
```

The `run/` directory contains local world data, runtime configurations, and server properties. It is deliberately excluded from version control.

---

## Repository Guide

* `src/` : Complete Java source code for the NeoForge mod, containing registries, interactive blocks, custom entities, network packets, and simulation engines.
* `docs/` : In-depth documentation, including user walkthroughs, administrator guides, command references, and system design specifications.
* `distribution/` : Packaged client distribution tools and setup guides for classroom workstations.
* `voiceover/` : Voice scripts, prompts, and audio files for NPC instructors.
* `scripts/` : Python utilities for texture generation, data formatting, and asset workflows.
* `migrations/` : Reference implementations and legacy prototype material.

---

## Available Documentation

Explore the `docs/` directory for exhaustive technical guides:

* **Player Guide:** See `docs/usermanual.md` for client installation, server connection, and a step-by-step gameplay walkthrough.
* **Administrator Guide:** See `docs/adminmanual.md` for dedicated server hosting, client deployment, and operator controls.
* **Commands Reference:** See `docs/commands.md` for a comprehensive list of player, instructor, and simulation commands.
* **Subsystem Architecture:** Detailed breakdowns of tutorial loops, simulation state machines, and Academy rooms can be found in `docs/systems/`.
* **Developer Guidelines:** Consult `CLAUDE.md` for code architecture rules, class indexes, and contribution conventions.

---

## Research Team and Acknowledgments

### Primary Researchers
* **Dheyn Michael Orlanda**
* **Francis Neil Mistica**
* **Ian Patrick Messias**

This research study was conducted in partial fulfillment of the academic requirements at **Laguna State Polytechnic University (LSPU)**.

### Institutional Collaborators and Acknowledgments
* **Bureau of Fire Protection (BFP) Sta. Cruz, Laguna:** We express our deepest gratitude to the BFP officers and personnel whose professional guidance, procedural standards, and practical feedback directly shaped our simulation rules and safety curricula.
* **Laguna State Polytechnic University:** For providing academic mentorship, facility access, and institutional support throughout this research.
* **NeoForge Community:** For providing the modding framework and development documentation that made this work possible.
* **ElevenLabs:** For providing text-to-speech tools that brought our NPC instructors to life.

---

## Academic Integrity and License

This project is published for academic evaluation, thesis defense review, and portfolio purposes. All rights are reserved by the original researchers. Please refer to `LICENSE` for formal terms.
