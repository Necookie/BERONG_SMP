# BerongSMP — Admin & Hosting Manual

This is the operator-facing guide: hosting the server, distributing the client,
running in-game admin tools, and troubleshooting. For the player-facing setup
guide, see [`usermanual.md`](usermanual.md). For full architecture detail, see
the main [`CLAUDE.md`](../CLAUDE.md).

## Components at a glance

| Component | Where | Purpose |
|---|---|---|
| Mod server | MCServerHost (Pterodactyl panel) | Hosts the actual Minecraft world/session |
| Turso | Cloud (libSQL) | Stores student accounts + session/telemetry data |
| Client installer | `distribution/client-installer/` | One-click NeoForge + mod setup for players |
| Landing page | `BERONG_SMP_WEB/apps/landing` | Public install instructions + server status |
| Dashboard | `BERONG_SMP_WEB/apps/dashboard` | Instructor review of session data (separate repo) |

---

## Hosting the server (MCServerHost — Spider Plan)

1. Create a server on MCServerHost: game = **Minecraft Java Edition**, loader =
   **NeoForge**, matching whatever version is currently deployed (check the
   server's own boot log for the exact `neoforge` version string — it can be
   newer than this repo's `gradle.properties` `neo_version`, since the host may
   auto-pull a newer patch build; at time of writing the live server runs
   **NeoForge 26.1.2.80**).
2. Set the **Java version** to **Java 25** in the panel's server settings —
   wrong Java version is the most common reason a modded server won't boot.
3. Accept the EULA (`eula=true` in `eula.txt`).
4. **Upload the mod jar**: File Manager → `mods/` → upload
   `build/libs/berongsmp-1.0.0.jar` (build it first — see below).
   Optionally also upload `run/mods/worldedit-mod-7.4.3.jar` if you want the
   `//copyroom` dev tool available (not needed for players).
5. **Upload the config**: File Manager → `config/` → upload
   `run/config/berongsmp-common.toml` (carries the Turso URL/token, `bfpAdminPin`,
   and tuning knobs — the server runs on defaults without it).
6. **Harden `server.properties`** before going live:
   - `rcon.password` — set a real value (don't leave it empty with RCON enabled).
   - `management-server-secret` — regenerate, don't reuse a local dev value.
   - `online-mode` — `true` requires real purchased Minecraft accounts; `false`
     lets anyone join under any username via this mod's own `/register`/`/login`
     system. Pick based on your audience.
   - `white-list=true` recommended while testing, to keep it closed to known
     testers.
7. Start the server and watch the console for a clean boot: no red exception
   stack traces, `BerongSMP` bootstrap log lines, `AcademyBuildingManager`
   placing its schematic, and vanilla's `"Done"` line.
8. Grab the exact **external IP:port** from the panel's Allocation/Network tab
   (not the internal port shown in the console log) — that's the address
   players use.

### Known startup crash (fixed) — WorldEdit `NoClassDefFoundError`

If you ever see the server crash on boot with
`NoClassDefFoundError: com/sk89q/worldedit/IncompleteRegionException` pointing
at `ModCommands.java`, it means you're running a build **older** than the fix
in this repo. Root cause: `//copyroom` (`CopyRoomCommand`) references WorldEdit
types, and merely *loading* that class — triggered by `ModCommands.register()`
calling it unconditionally — fails the JVM verifier on a server without
WorldEdit installed, before its own internal `ModList.isLoaded("worldedit")`
guard ever gets a chance to run. Fixed by moving the guard to the call site in
`ModCommands.java` so the class is never loaded at all when WorldEdit is
absent. Rebuild (`./gradlew build`) and re-upload the jar if you hit this.

---

## Building the mod

```bash
./gradlew build
```

Output: `build/libs/berongsmp-1.0.0.jar`. Rebuild and re-upload to the server
any time source changes — check `git status` first if unsure whether the last
build is current.

---

## Building / updating the client installer

The installer lives in `distribution/client-installer/`:

- `install-berongsmp.ps1` — source script (tracked in git)
- `Install-BerongSMP.exe` — compiled via `ps2exe` (gitignored, regenerate as needed)
- `payload/` — bundled `neoforge-<version>-installer.jar` + the current mod jar (gitignored)
- `README.txt` — plain-language instructions shipped inside the distributed zip
- `BerongSMP-Client-Installer.zip` — the actual file uploaded to Google Drive

**To update the mod jar the installer bundles:**
```bash
./gradlew build
cp build/libs/berongsmp-1.0.0.jar distribution/client-installer/payload/
```

**To update the bundled NeoForge installer** (if the server's NeoForge version
changes): download the matching installer jar from
`https://maven.neoforged.net/releases/net/neoforged/neoforge/<version>/neoforge-<version>-installer.jar`
into `payload/`, and update the version string in `install-berongsmp.ps1`
(the `--install-client` call and the success message) to match.

**To recompile the script into the exe** (after editing `install-berongsmp.ps1`):
```powershell
Import-Module ps2exe
Invoke-ps2exe -inputFile "distribution\client-installer\install-berongsmp.ps1" `
  -outputFile "distribution\client-installer\Install-BerongSMP.exe" `
  -title "BerongSMP Client Installer" -product "BerongSMP" -version "1.0.0.0" `
  -requireAdmin:$false -noConsole:$false
```
(`ps2exe` module install: `Install-PackageProvider -Name NuGet -Force; Install-Module -Name ps2exe -Scope CurrentUser -Force`.)

**Known gotcha:** `$PSScriptRoot` / `$MyInvocation.MyCommand.Path` don't
reliably resolve inside a compiled `ps2exe` binary. The script falls back to
`[System.Diagnostics.Process]::GetCurrentProcess().MainModule.FileName` when
`$PSScriptRoot` is empty — don't remove that fallback, or the compiled exe
will crash instantly on launch with no visible error (looks like the window
"flashing and closing").

**To repackage the distributable zip:**
```powershell
$src = "distribution\client-installer"
Compress-Archive -Path "$src\Install-BerongSMP.exe","$src\README.txt","$src\payload" `
  -DestinationPath "$src\BerongSMP-Client-Installer.zip" -Force
```
Then re-upload that zip to the same Google Drive folder the landing page's
"Download Installer" button already links to (no need to change the link
itself, just replace the file at that location).

**Before repackaging**, make sure `README.txt`'s server address line is
current — it's plain text, not auto-generated.

---

## Distribution workflow

The player-facing install flow is described on the public landing page
(`BERONG_SMP_WEB/apps/landing`, `Instructions.astro`) and must stay in sync
with whatever this repo's installer actually does. If you change the install
steps, the NeoForge version, or the server address here, update that page too
— see that repo's own `usermanual.md`/`adminmanual.md` and `CLAUDE.md`.

---

## In-game admin commands

Full reference (every `/bfp` subcommand, exact OP levels, etc.) is in the main
[`CLAUDE.md`](../CLAUDE.md). Most-used day to day:

| Command | Effect |
|---|---|
| `/bfp login <pin>` | Authenticate for `/bfp` access without OP (if `bfpAdminPin` is configured) |
| `/bfp checkin <student_name>` / `/bfp checkout` | Start/finalize a shared-station session |
| `/bfp tutorial [player]` | Reset + activate the Academy for a player |
| `/bfp tutorial skipto <cruz\|reyes\|santos\|morfe> [player]` | Jump into any Academy room for demoing/testing |
| `/bfp bypass on [player]` | Skip lobby gates for quick testing |
| `/bfp sessions today` / `/bfp sessions stats` | Quick session activity checks |
| `/sim_status [player]` / `/sim_list` | Live snapshot of active simulations |
| `/sim_scan_hazards` | Verify New Sim Building 2.0's hazard scan covers the whole building |

---

## Session data & the dashboard

The mod writes account and session data directly to **Turso** (configured via
`tursoUrl`/`tursoToken` in `berongsmp-common.toml`). The companion dashboard
app (`BERONG_SMP_WEB/apps/dashboard`) reads that same database for instructor
review — session logs, movement replay, MiDRR ML preparedness assessment, and
roster stats. See that repo's own admin manual for how to use it.
