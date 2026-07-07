# save-tokens

Work in token-lean mode for the rest of the session. Use when the session will be long (big refactor, many micro-commits) or when context is running low. This changes *how* Claude works, not *what* it does.

## Rules to follow after invoking

**Reading**
- Never read a whole file when a targeted `Grep` (with `-n -C 3`) or a ranged `Read` (offset/limit) answers the question.
- Never re-read a file just edited — `Edit`/`Write` fail loudly, success means the change landed.
- Before exploring code, check `CLAUDE.md`'s Key Classes table and `docs/systems/*.md` first — most architecture questions are already answered there; do not re-derive what's documented.
- For "where is X" questions across many files, use one `Grep` over the tree, not per-directory listing.

**Building & running**
- `./gradlew compileJava` for verification, never `build` unless the JAR is actually needed; never `clean` unless cache corruption is suspected.
- Pipe build output through `tail`/`grep -E "error|BUILD"` — never dump full Gradle logs into context.
- Boot the dev server only when a change alters class-loading, registration, or structure paths — compile success covers everything else.
- Read server logs with `grep` for the specific milestone/error, never the whole `latest.log`.

**Editing**
- Batch mechanical multi-file changes into one scripted `sed`/`python` pass instead of N individual Edit calls — but only for truly mechanical changes (renames, import swaps).
- Make independent tool calls in parallel in one message.

**Output**
- Status updates one line each; final summaries lead with the outcome, no play-by-play.
- Do not paste code that was just written into the summary — reference `file:line` instead.

## What NOT to sacrifice

- Compile check before every commit — always.
- The CLAUDE.md sync rule — always.
- Asking before destructive/irreversible actions — always.
