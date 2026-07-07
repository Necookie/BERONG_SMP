# micro-commit

Ship the current work as a series of small, independent commits — compile-checked, doc-synced, and pushed. This automates the workflow this repo already uses everywhere: one atomic logical change per commit, lots of them, all visible on `main`.

## Arguments

- *(none)* — commit whatever is currently uncommitted, split into logical micro-commits, push to `main`
- `branch <name>` — same, but on a new branch `<name>`; merge back into `main` with `--ff-only` when told to finish (never squash — every micro-commit must stay visible on `main`)
- `one` — everything as a single commit (only when the diff really is one logical change)

## Steps

1. `git status --porcelain` + `git diff` — inventory every change. If the tree is clean, say so and stop.
2. Group the changes into **independent logical units** (one rename, one extraction, one fix each). If two files only compile together, they are one unit — never split a unit that would break the build mid-history.
3. For each unit, in order:
   a. Run `./gradlew compileJava` (skip only for pure doc/resource changes with no Java impact).
   b. Stage **exact file paths** — never `git add -A` or `git add .` unless the inventory confirmed nothing unrelated is present.
   c. Commit with a conventional message: `refactor:`/`fix:`/`feat:`/`docs:`/`chore:` prefix, imperative subject ≤ 72 chars, body explaining *why* (not what — the diff shows what).
4. **CLAUDE.md sync check** — after all commits, ask: did any change touch the Key Classes table, package layout, structure files, config knobs, or commands? If yes, update `CLAUDE.md` (or the owning `docs/systems/*.md` — per the Documentation Map, deep-dive content lives there, not in CLAUDE.md) and commit that as its own `docs:` commit.
5. Push: `git push origin main` (or the branch). Confirm with `git log origin/main --oneline -n <count>`.

## Rules

- Every commit must compile on its own — a reader bisecting history should never land on a broken tree.
- Never commit `run/` world saves, logs, or anything in `.gitignore`'s spirit even if untracked.
- If a change looks like it contains a secret (tokens, PINs, URLs with credentials), stop and ask before committing.
- If `main` has diverged from `origin/main`, stop and report — never force-push.

## Example invocations

- `/micro-commit` — ship everything to main now
- `/micro-commit branch hazard-rework` — ship on a branch for later ff-merge
