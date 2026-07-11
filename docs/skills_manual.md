# BerongSMP Skills Manual

How to drive Claude Code on this repo with the project skills in `.claude/skills/`. Type a skill as `/<name>` (with arguments where noted). Each skill is a plain markdown file — edit it to change the behavior; changes apply on the next invocation.

## The skills at a glance

| Skill | One-liner | Reach for it when… |
|---|---|---|
| **Workflow** | | |
| `/micro-commit` | Split uncommitted work into compile-checked micro-commits, sync CLAUDE.md, push to main (or a branch) | You finished any chunk of work and want it shipped the way this repo always ships |
| `/save-tokens` | Switch the session to token-lean mode (targeted greps, filtered build output, terse replies) | Start of a long session, or context is running low |
| `/build-check` | `compileJava` with smart error surfacing | Before committing, after editing Java, fastest feedback loop |
| `/sim-status` | Compile + server-log health scan | "Is everything OK?" / something's broken, unsure where |
| `/run-server` | Start the dev server in the background + how to watch it | Any live in-game test |
| **Knowledge (load before coding)** | | |
| `/neoforge` | This repo's NeoForge 26.1.2 conventions and crash traps | Before any change to blocks/items/entities/packets/registrations |
| **Content pipelines (thesis)** | | |
| `/new-hazard-prop <name> [facing] [kitchen]` | Full checklist for a new 3-state hazard prop block | Adding a new campus fire hazard to the simulation |
| `/new-furniture-prop <name> [facing] [flammable]` | Full checklist for a new furniture/decor block | Adding a new placeable prop with no hazard lifecycle |
| `/new-extinguisher <name> <class>` | Full checklist for a new extinguisher item | Adding a new BFP-class suppression tool |
| **Scaffolds & audits** | | |
| `/new-command <name> [op] [arg:<type>]` | Scaffold a Brigadier command in the right command file | Adding any `/command` |
| `/new-stage <NAME> <click\|tick\|extinguish>` | Add a TutorialStage + handler (old tutorial) | Extending the original tutorial flow |
| `/check-tutorial` | Audit every TutorialStage for orphaned/unreachable stages | After `/new-stage`; player stuck reports; pre-release |
| `/place-structure [lobby\|lspu\|ssc\|tutorial\|all]` | Guided in-game structure placement verification | After editing a `.nbt`/`.schem` or a BlockPos constant |
| `/tune-stations` | Re-align tutorial NPC/station offsets from F3 coords | NPCs/stations misplaced after moving the lobby |

## Prompting guide — how to phrase requests

**Lead with the skill when one fits.** `/new-hazard-prop overloaded_microwave facing kitchen` gets a complete, gotcha-aware pipeline; "add a microwave hazard block" gets an improvisation that may miss the `items/*.json` client definition or the `HAZARD_ITEM_MAP` ordering.

**Stack knowledge + work + ship.** The high-leverage pattern for a feature session:

```
/save-tokens
/neoforge
<describe the feature>
/micro-commit
```

**Give real coordinates, not descriptions.** For anything spatial (stations, zones, viewpoints), press F3 in-game and paste the numbers — the codebase treats F3-verified coordinates as ground truth (see the coordinate tables in CLAUDE.md).

**Say where it should land.** "push to main" / "use a branch and merge after" — `/micro-commit` supports both; the default is main with every micro-commit visible.

**Trust the docs chain.** CLAUDE.md is the index; `docs/systems/*.md` hold subsystem deep-dives; `docs/history/*.md` are frozen fix logs; `docs/hazard_props_spec.md` is the hazard design spec. Pointing Claude at the right doc ("per the academy doc…") is cheaper and more accurate than re-explaining.

## Recommended workflows

**New session, unknown state**
```
/sim-status
```

**Adding a hazard prop (thesis content)**
```
/save-tokens          (optional, long session)
/neoforge
/new-hazard-prop <name> [facing] [kitchen]
/build-check
/run-server           → verify with the hazard wand in-game
/micro-commit
```

**Adding an extinguisher**
```
/neoforge
/new-extinguisher <name> <class>
/build-check → /run-server → /micro-commit
```

**Editing structures / world layout**
```
/build-check → /run-server → /place-structure <name>
(if tutorial stations moved: /tune-stations)
/micro-commit
```

**Old-tutorial changes**
```
/new-stage NAME type → /check-tutorial → /build-check → /run-server
/micro-commit
```

## Rules the skills enforce (so you don't have to repeat them)

- Every commit compiles on its own; exact-path staging, never `git add -A` blind.
- CLAUDE.md / `docs/systems/*` get a `docs:` commit whenever architecture, commands, structures, or config knobs change.
- Boot smoke test whenever a change touches registration, static init, class-loading, or structure paths — compile success does not cover those.
- New per-tick handlers must be reachable from a real code path (the `TickScheduler` class-loading rule) — `/neoforge` has the details.
