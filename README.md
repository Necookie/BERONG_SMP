# BerongSMP

BerongSMP is a Minecraft mod that turns a Java Edition server into a disaster preparedness
training ground. It is built with NeoForge for Minecraft, and it is the technical half of a
university thesis: players walk through a guided fire safety tutorial, then get dropped into a
live, graded fire scenario, and everything they do along the way is logged and fed into a machine
learning pipeline that scores how prepared they actually are.

This is not a small tech demo bolted onto vanilla Minecraft. It has its own custom blocks, NPCs,
hazard props, scoring rules, a student account system backed by a cloud database, and a full
telemetry contract shared with a separate ML/dashboard project. If you are reading this because you
are grading, reviewing, or curious about the thesis, the short version is: press play, walk through
the Academy, then try not to burn the building down.

## What it actually does

A player connects, registers or logs in with `/register` or `/login`, and lands in a small lobby
with two buttons.

**Button one, the Academy.** A four room tutorial, each room run by its own NPC instructor:

- Officer Cruz opens with a briefing and a go/stop reaction drill.
- Sgt. Reyes teaches extinguisher use: pull the pin, aim at the base of the fire, sweep, and what
  to do if you catch fire yourself (drop and roll, for real, in game).
- Sgt. Santos covers earthquake response: duck, cover, and hold on.
- Capt. Morfe runs the final evaluation. Score high enough and you are certified and automatically
  deployed into the live simulation a few seconds later.

**Button two, New Sim Building 2.0.** The graded scenario, once certified. It runs in three phases:

- **Prevention.** Find and defuse hazard props before they fail, either by hand for a merely
  hazardous one, or with the right extinguisher once it is already burning.
- **Intervention.** Put out anything that escalated into a real fire, using the extinguisher that
  actually matches the hazard: dry chemical for ordinary fires, CO2 for electrical fires, wet
  chemical for kitchen grease fires. The wrong one does not work, and you get told so.
- **Evacuation.** When the alarm sounds or time runs out, get to the nearest exit and reach the
  outdoor assembly point.

Every session is scored automatically, written to a cloud database, and available for an
instructor to review afterward, including a full movement replay.

## Why it exists

This project was built around consultation with the Bureau of Fire Protection and modeled on a
real evacuation plan for LSPU, with the goal of measuring disaster preparedness the same way a real
drill would, rather than just asking someone to fill out a survey afterward. Every interaction the
mod tracks, from how long it takes someone to react, to whether they used the right extinguisher,
to whether they went the right way during evacuation, is designed to feed features into a
preparedness classifier rather than just log noise. The exact contract for that data lives in
[`docs/telemetry_contract.md`](docs/telemetry_contract.md).

## Requirements

- Java 25
- NeoForge for Minecraft 26.2.0 (the exact build is pinned in `gradle.properties`)
- A Turso (libSQL) database if you want student accounts and session telemetry to actually persist.
  The mod runs fine without one for local testing, it just will not save anything.

## Getting started (development)

```bash
# Build the mod jar
./gradlew build

# Run a headless dev server (world data lands in run/)
./gradlew runServer

# Run a dev client and connect to it
./gradlew runClient

# Fast compile check without a full build
./gradlew compileJava
```

The `run/` folder is the working directory for local dev runs. It holds the world save, server
config, and op list, and it is intentionally not committed to git.

## Playing it

If you just want to install the client and play, follow
[`docs/usermanual.md`](docs/usermanual.md). It walks through installing the NeoForge client,
connecting to a running server, registering an account, and getting through both the Academy and
the graded simulation.

## Hosting or administering it

If you are setting up your own server, distributing the client to a class, or need the `/bfp`
admin command reference, start with [`docs/adminmanual.md`](docs/adminmanual.md) and
[`docs/commands.md`](docs/commands.md).

## Repository layout

```
src/            Mod source code (Java)
docs/           All project documentation, see the map below
distribution/   The one click client installer players actually download
voiceover/      NPC voice line scripts and generated audio
scripts/        Python helpers that generate the mod's hand drawn textures
migrations/     The original Fabric prototype this project grew out of
run/            Local dev server working directory (gitignored)
```

## Documentation map

Everything beyond this README lives under `docs/`. A few starting points:

| Doc | What it is for |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | The deep architecture reference: package layout, event handling, every major system, and a running changelog of fixes. The single source of truth for how the code is built. |
| [`docs/major_plan.md`](docs/major_plan.md) | The phased implementation plan and database schema. |
| [`docs/systems/`](docs/systems/) | Focused write ups of the tutorial, simulation, and Academy subsystems. |
| [`docs/telemetry_contract.md`](docs/telemetry_contract.md) | The exact data contract between the mod and the ML pipeline. |
| [`docs/hazard_props_spec.md`](docs/hazard_props_spec.md) | The design spec for all 85 hazard prop blocks in the simulation. |
| [`docs/usermanual.md`](docs/usermanual.md) | Player facing setup and walkthrough guide. |
| [`docs/adminmanual.md`](docs/adminmanual.md) | Hosting, client distribution, and admin tooling guide. |
| [`docs/commands.md`](docs/commands.md) | Every in-game command, what it does, and who can run it. |
| [`docs/history/`](docs/history/) | Frozen point in time audit and remediation logs, kept for context, not updated going forward. |

## License

This project is all rights reserved, see [`LICENSE`](LICENSE). It is published for academic
review, portfolio, and evaluation purposes, not for reuse. The original NeoForge template this
project was scaffolded from keeps its own permissive license, preserved at
[`docs/third_party/NEOFORGE_MDK_LICENSE.txt`](docs/third_party/NEOFORGE_MDK_LICENSE.txt).

## Contributing, security, and conduct

This is a solo thesis project, but the repository is public and issues are welcome. See
[`CONTRIBUTING.md`](CONTRIBUTING.md) for how to file a useful bug report or question,
[`SECURITY.md`](SECURITY.md) for how to report a security concern privately, and
[`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) for how everyone is expected to behave here.

## Credits

Built by Necookie. Scaffolded from the official [NeoForge MDK](https://github.com/NeoForged/MDK)
template. Voice lines produced with ElevenLabs text to speech. Thanks to the Bureau of Fire
Protection for the consultation that shaped the simulation's fire response steps.
