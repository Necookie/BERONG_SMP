# Contributing to BerongSMP

Thanks for taking a look at this project. BerongSMP is a solo university thesis, so it is not
actively looking for outside code contributions in the usual open source sense, but the project is
public, the source is readable, and issues, questions, and suggestions are genuinely welcome.

## Before you open anything

Please read `LICENSE` first. This code is all rights reserved. Being able to read a file here does
not mean you are free to copy, reuse, or redistribute it. If you want to build on this project or
use part of it elsewhere, open an issue and ask, do not assume.

## Reporting a bug or suggesting a change

The most useful thing you can do is open a GitHub issue. A good bug report includes:

- What you did (steps to reproduce, ideally starting from a fresh `./gradlew runServer`)
- What you expected to happen
- What actually happened, including the relevant lines from the server log or `run/telemetry/`
- Your Java version and operating system

If you are proposing a design or gameplay change rather than reporting a bug, a short description
of the problem you are trying to solve is more useful than a fully worked out solution. This
project has a fairly involved simulation and telemetry pipeline (see `CLAUDE.md` and
`docs/major_plan.md`), so context helps a lot.

## If you do want to send a pull request

Small, focused pull requests are far more likely to be reviewed than large ones. This project's
own git history is a good model to follow: each commit does one clear thing and says what and why
in the message body, for example `fix: reset hunger alongside HP on simulation start/end` rather
than a vague `updates`. Please match that style.

Practical requirements before opening a pull request:

1. **Read `CLAUDE.md` first.** It is the single source of truth for how this codebase is laid out
   (package layout, the event bus split, tick scheduling, hazard state machine, and so on) and it
   links out to `docs/systems/` for the tutorial, simulation, and Academy subsystems in depth.
2. **Build and test locally.** This project targets Java 25.
   ```bash
   ./gradlew compileJava   # fast syntax/type check
   ./gradlew build         # full build, runs tests
   ./gradlew runServer     # headless dev server, world data lives under run/
   ./gradlew runClient     # dev client, connect to the dev server to play through changes
   ```
3. **Keep changes scoped.** If a change touches a system that already has its own doc under
   `docs/systems/`, update that doc in the same pull request rather than leaving it stale.
4. **Do not commit generated or local-only files.** `run/`, `build/`, `.gradle/`, and similar are
   already gitignored. If you added a new tool that produces artifacts, gitignore them too.
5. **Explain telemetry or scoring changes clearly.** Anything that changes what the mod logs, or
   how a simulation run is scored, affects the ML pipeline described in `docs/telemetry_contract.md`.
   Bump the contract version and say so in your pull request description if you touch that surface.

## Code style

There is no separate style guide beyond "match what is already there." Look at a neighboring class
before adding a new one: shared base classes exist for a reason (see the "Shared Base Classes"
section of `CLAUDE.md`), and reusing them instead of copying boilerplate is expected.

## Questions

If something in `CLAUDE.md` or the `docs/` folder is unclear, that is worth an issue on its own.
Documentation gaps are considered bugs in this project, not just nice to haves.
