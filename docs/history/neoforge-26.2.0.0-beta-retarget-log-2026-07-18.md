# NeoForge 26.2.0.0-beta Retarget Log (2026-07-18)

A same-day follow-up to
[neoforge-26.2-migration-log-2026-07-17.md](neoforge-26.2-migration-log-2026-07-17.md), triggered by
a real deploy attempt on the live MCServerHost server. Frozen log, kept for context but not updated
going forward.

## What happened

After merging the NeoForge 26.2.0.23-beta migration to `main` and uploading the rebuilt mod jar to
MCServerHost, the live server failed to boot:

```
Mod ID: 'neoforge', Requested by: 'berongsmp', Expected range: '[26.2.0.23-beta,)', Actual version: '26.1.2.80'
```

The loader hadn't been switched in the panel yet. After switching it, a second boot attempt failed
differently:

```
Mod ID: 'neoforge', Requested by: 'berongsmp', Expected range: '[26.2.0.23-beta,)', Actual version: '26.2.0.0-beta'
```

MCServerHost's NeoForge installer wizard for Minecraft `26.2.0` only offers a single installable
build, `26.2.0.0-beta` — there's no field to request a specific later patch build like `.23-beta`.
User confirmed (2026-07-18) to retarget the mod down to match exactly what the host can install,
rather than pursue a manual/unsupported install path on the panel.

## Fixes applied

1. **`neo_version` retargeted**: `26.2.0.23-beta` → `26.2.0.0-beta` in `gradle.properties`.
   `minecraft_version` (`26.2.0`) is unchanged — this is the same Minecraft version, just an earlier
   NeoForge build number, so none of the MC-API fixes from the 2026-07-17 migration needed
   revisiting. Confirmed via a fresh `compileJava` — clean, no new errors.

2. **WorldEdit's own version floor exceeds 26.2.0.0-beta.** Re-running `runServer` surfaced a second,
   independent incompatibility: WorldEdit 7.4.4 itself declares `neoforge 26.2.0.7-beta or above` in
   its own `mods.toml` — confirmed via the live FML rejection message
   (`Mod worldedit requires neoforge 26.2.0.7-beta or above, Currently neoforge is 26.2.0.0-beta`).
   This is a constraint set by WorldEdit's own release, not something this project's `build.gradle`
   controls. Fixed by disabling the `localRuntime` WorldEdit dependency in `build.gradle` (the
   `compileOnly` dependency stays, so `CopyRoomCommand.java` still compiles against the WorldEdit
   API) — `//copyroom` is dev-only and already has a `ModList.isLoaded("worldedit")` guard at its
   call site, so this doesn't affect anything players use, only the dev-time convenience of testing
   `//copyroom` locally while pinned to this build.

3. **Stale dev-runtime jar.** `run/mods/worldedit-mod-7.4.3.jar` (a leftover, manually-placed jar
   from MC-26.1-line testing, gitignored/untracked) was still being picked up by the dev `runServer`
   task and also rejected (`Mod worldedit requires minecraft 26.1.2 or above, and below 26.2`).
   Moved out of `run/mods/` rather than deleted, in case MC-26.1-line dev testing is needed again
   later. This wasn't a real bug — just local dev-environment residue — but it blocked verifying the
   fix above.

## Gates re-run and results

- `./gradlew compileJava` — clean (fresh NeoForm decompile/patch/recompile for the new build number,
  ~5 minutes, not a failure signal).
- `./gradlew test` — all 8 test classes / 44 tests passed fresh.
- `./gradlew runServer` — third attempt (after both WorldEdit issues above were resolved) booted
  cleanly to vanilla's `Done (0.745s)! For help, type "help"`, same `BerongSMP` bootstrap markers as
  before, same single pre-existing `bfp_tutorial_lobby` fallback line, no other errors.

## Client installer

Re-updated to target NeoForge `26.2.0.0-beta` (confirmed downloadable from
`maven.neoforged.net/releases/net/neoforged/neoforge/26.2.0.0-beta/`):
- Old `neoforge-26.2.0.23-beta-installer.jar` removed from
  `distribution/client-installer/payload/`, replaced with `neoforge-26.2.0.0-beta-installer.jar`.
- `berongsmp-1.0.0.jar` refreshed (same source as the 2026-07-17 migration; only the NeoForge build
  number pin changed, no Java source changed in this pass beyond the `build.gradle` WorldEdit
  disable).
- `install-berongsmp.ps1` and `README.txt` version strings updated (`26.2.0.23-beta` →
  `26.2.0.0-beta`, all 8 occurrences across both files).
- `Install-BerongSMP.exe` recompiled via `ps2exe`; `BerongSMP-Client-Installer.zip` repackaged.

## What this means for `//copyroom` on the live server

WorldEdit is **not** installable alongside NeoForge `26.2.0.0-beta` — no published WorldEdit release
supports a build that old in the 26.2 line. `//copyroom` (a dev/admin-only tool, already documented
as "not needed for players") is unavailable on the live server until either MCServerHost adds a
newer 26.2 patch-build option, or WorldEdit ships a release compatible with `.0-beta` (unlikely,
since `.7-beta` is WorldEdit's own stated floor). No player-facing feature is affected.

## Commits (in order)

1. `fix: retarget neo_version to 26.2.0.0-beta to match what MCServerHost can install`
2. `fix: disable WorldEdit localRuntime while pinned to NeoForge 26.2.0.0-beta`
3. `chore: retarget installer to NeoForge 26.2.0.0-beta`
4. `docs: record the NeoForge 26.2.0.0-beta retarget` (this log)
