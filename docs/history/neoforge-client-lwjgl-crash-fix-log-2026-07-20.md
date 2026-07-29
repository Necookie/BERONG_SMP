# NeoForge Client LWJGL Crash Fix Log (2026-07-20)

## Symptom

A player who ran `Install-BerongSMP.exe` got a game crash immediately on launch:
`ERROR: GAME CRASHED — an unexpected issue occurred... Exit Code: 1`. No
`crash-reports/*.txt` file was written and no JVM `hs_err_pid*.log` was
produced. `logs/latest.log` stopped at exactly the same three lines on every
attempt (7 consecutive launches, same result each time):

```
[main/INFO]: Loading ImmediateWindowProvider fmlearlywindow
[main/INFO]: Closing FML Loader <hash>
[main/INFO]: Clearing ModLoader
```

This is before FancyModLoader even reaches mod discovery — `BerongSMP.class`
is never touched. Confirmed via direct inspection of the player's
`.minecraft` (GPU: Intel Iris Xe, driver dated 2025-09, ruled out as a driver
problem — the failure signature matched a known upstream bug exactly, not a
generic GL/driver crash).

## Root cause

Confirmed upstream NeoForge bug, not project code:
[neoforged/NeoForge#3233](https://github.com/neoforged/NeoForge/issues/3233).
Early `26.2.0.x-beta` builds have FancyModLoader declaring its own LWJGL
dependency, which overrides Minecraft's own "unsafe-using" LWJGL variant
(introduced as of `26.2-pre-1`) in the generated launcher profile — the
version mismatch crashes the client during early-window (GLFW/OpenGL) init,
before any log4j-backed exception handler can catch it (hence no crash
report). Fixed upstream in NeoForge **26.2.0.6-beta**
([PR #3250](https://github.com/neoforged/NeoForge/pull/3250)).

This project's `neo_version` was **26.2.0.0-beta** — pinned there by the
2026-07-18 retarget (see
[neoforge-26.2.0.0-beta-retarget-log-2026-07-18.md](neoforge-26.2.0.0-beta-retarget-log-2026-07-18.md))
because MCServerHost's guided installer only offers that one exact build for
the live server. That means **every** player using the installer as it stood
would hit this crash — not a one-off.

## Fix

The mod's own `neoforge.mods.toml` dependency declaration is an **open-ended
minimum** (`versionRange="[${neo_version},)"`, see
`src/main/templates/META-INF/neoforge.mods.toml`) — it requires NeoForge
`>= 26.2.0.0-beta`, not an exact match. That means a **client** running a
newer 26.2.0.x-beta patch build is still a valid target for the exact same
mod jar; no rebuild needed.

Verified empirically end-to-end before making any change:
1. Downloaded `neoforge-26.2.0.7-beta-installer.jar` (first build that is
   both past the LWJGL fix in `.6-beta` *and* satisfies WorldEdit 7.4.4's own
   `26.2.0.7-beta` floor noted in the retarget log — picked deliberately to
   kill both birds).
2. Ran `--install-client` against a real `.minecraft` — added a
   `neoforge-26.2.0.7-beta` version profile alongside the existing
   `neoforge-26.2.0.0-beta` one (purely additive, nothing removed).
3. Launched it: full clean boot (all texture atlases loaded, no crash).
4. Connected to the **live production server**, which is still running
   whatever build MCServerHost actually deployed (`26.2.0.0-beta`-line) —
   confirmed NeoForge tolerates this specific client/server patch mismatch:
   login succeeded, BerongSMP's own `/login` flow worked
   ("Academy already certified..."), normal gameplay, clean voluntary
   disconnect. No "Incompatible client" rejection.

Only the **client installer** was changed — `gradle.properties`' `neo_version`
was deliberately left at `26.2.0.0-beta` (still the correct value: it's what
the live server actually runs, and bumping it would tighten the mod's own
minimum-version floor for no reason, since the open-ended range already
covers newer clients):

- `distribution/client-installer/install-berongsmp.ps1` — installer jar
  filename, install-step message, and the post-install "select this profile"
  message all bumped `26.2.0.0-beta` → `26.2.0.7-beta`.
- `distribution/client-installer/payload/neoforge-26.2.0.0-beta-installer.jar`
  replaced with `neoforge-26.2.0.7-beta-installer.jar`.
- `distribution/client-installer/README.txt` — all four version mentions
  updated to match.
- `Install-BerongSMP.exe` recompiled via `ps2exe` (same flags as documented
  in `docs/adminmanual.md`) and `BerongSMP-Client-Installer.zip` repackaged.

**Not yet done — needs the project owner, requires hosting-panel access this
environment doesn't have:** the zip still needs to be re-uploaded to the
Google Drive folder the landing page links to (see
`docs/adminmanual.md`'s Distribution workflow section). The live server
itself is untouched and still on whatever MCServerHost deployed for
`26.2.0.0-beta` — leaving it there is fine (cross-patch connection is
confirmed working), but if MCServerHost's panel turns out to allow a manual
NeoForge server-side upgrade (Pterodactyl file manager + console, bypassing
the installer wizard's version dropdown), moving the server to `26.2.0.7-beta`
too would be the architecturally cleaner end state — tracked as an open
follow-up, not required for players to be unblocked.
