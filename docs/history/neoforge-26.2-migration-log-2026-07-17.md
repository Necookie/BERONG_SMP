# NeoForge 26.2 Migration Log (2026-07-17)

BerongSMP moved from NeoForge `26.1.2.36-beta` (Minecraft `26.1.2`) to NeoForge `26.2.0.23-beta`
(Minecraft `26.2.0`) on the `neoforge-26.2-upgrade` branch, purely to stay current — no specific
feature drove the jump. Frozen log, kept for context but not updated going forward.

## Version pins

| | Before | After |
|---|---|---|
| `minecraft_version` | `26.1.2` | `26.2.0` |
| `minecraft_version_range` | `[26.1.2]` | `[26.2.0]` |
| `neo_version` | `26.1.2.36-beta` | `26.2.0.23-beta` |
| `worldedit_version` | `7.4.3` | `7.4.4` |

## Guarantee: buildings/textures untouched

Verified, not just asserted, via two independent methods:
1. `git diff main...neoforge-26.2-upgrade --stat -- src/main/resources/data/berongsmp/structure/ src/main/resources/assets/berongsmp/textures/` — zero output.
2. SHA-256 checksums of all 6 structure files (`.nbt`/`.schem`) and all 630 texture files, recorded
   before any branch edits and re-checked at the end — byte-identical.

No commit in this migration touches either directory. This was a toolchain/API version bump only.

## What broke and how it was fixed

Everything below was confirmed against the actual decompiled NeoForm sources for MC 26.2 (extracted
from the cached `mergeWithSources_*` NeoForm-runtime jar), not guessed from changelog text.

1. **WorldEdit artifact resolution failure.** `worldedit-neoforge-mc26.2.0:7.4.4` doesn't exist on
   either `maven.neoforged.net` or `maven.enginehub.org`. Confirmed via direct HTTP checks against
   `maven-metadata.xml` that EngineHub changed their artifact-naming convention between MC lines:
   the 26.1 line published under the full patch version (`worldedit-neoforge-mc26.1.2`), but the
   26.2 line publishes under major.minor only (`worldedit-neoforge-mc26.2`, release `7.4.4`).
   Fixed by introducing a separate `worldedit_mc_version` property in `gradle.properties`
   (`26.2`), decoupling WorldEdit's artifact coordinate from `minecraft_version` in `build.gradle`.
   WorldEdit 7.4.4 loaded correctly in the `runServer` boot log with no `LinkageError`/
   `NoClassDefFoundError` regression.

2. **Colored block constants removed from `Blocks`.** Mojang consolidated every per-color block
   family (`Blocks.RED_CONCRETE`, `Blocks.WHITE_WOOL`, etc.) into a single `ColorCollection<Block>`
   record per material (e.g. `Blocks.CONCRETE`, `Blocks.WOOL`), accessed via `.pick(DyeColor)`.
   Affected 6 files, ~40 call sites, all mechanical rewrites
   (`Blocks.RED_CONCRETE` → `Blocks.CONCRETE.pick(DyeColor.RED)`): `TutorialLobbyManager`,
   `AcademyBuildingManager`, `FurnitureFactory`, `LspuFacadeModule`, `LspuHallwayModule`,
   `CcsLabModule`. No behavior change — every `fill()`/`set()`/`setBlock()` call still places the
   exact same block.

3. **`I18n.exists(String)` removed.** MC 26.2's client `I18n` class only exposes
   `get(String, Object...)` now. Replaced with `Language.getInstance().has(String)` (the same
   existence check the removed method used internally) in `ItemDescriptionTooltip`.

4. **`EntityType.create(ValueInput, Level, EntitySpawnReason)` signature changed.** The 3-arg
   overload `SchemLoader` relied on for entity deserialization now takes an
   `EntitySpawnRequest(reason, ignoreChecks)` record instead of a bare `EntitySpawnReason`. Fixed by
   wrapping: `new EntitySpawnRequest(EntitySpawnReason.LOAD, false)`, matching the same wrapping
   pattern used internally by vanilla's own call sites.

5. **`EntityType.VILLAGER` moved.** All vanilla entity-type constants were extracted out of
   `EntityType` into a new class, `EntityTypes` (plural) — confirmed `EntityType.java` no longer
   declares any `public static final EntityType<...>` constants at all in MC 26.2. Fixed the one
   call site in `TutorialLobbyManager` (`EntityType.VILLAGER` → `EntityTypes.VILLAGER`); the
   `Villager` entity class's package (`net.minecraft.world.entity.npc.villager.Villager`) had
   already been correctly imported.

No structural changes to this mod's own design were needed — every fix was a call-site adaptation
to a Mojang-side rename/restructure, none touched game logic.

## Gates run and results

- `./gradlew compileJava` — clean, after the 4 fix commits above.
- `./gradlew test` — all 8 test classes / 44 tests passed fresh (not cached).
- `./gradlew runServer` — booted cleanly to vanilla's `Done (0.494s)! For help, type "help"`;
  `BerongSMP`'s own bootstrap lines and lobby placement logged correctly; WorldEdit 7.4.4 loaded
  with its NeoForge platform registered. One pre-existing `ERROR`-level log line
  (`Failed to load structure: berongsmp:bfp_tutorial_lobby`) is expected, unrelated fallback
  behavior — `TutorialLobbyManager.buildLobby` deliberately falls back to a programmatic build when
  that (never-shipped) `.nbt` file is absent; confirmed the file was already absent before this
  migration touched anything.
- **Playtest (user-run, 2026-07-18):** confirmed working — no further detail requested.

## Client installer

Updated to target NeoForge `26.2.0.23-beta` (the exact build validated above, per explicit user
confirmation rather than assuming a newer untested patch build):
- `distribution/client-installer/payload/neoforge-26.2.0.23-beta-installer.jar` (downloaded fresh,
  old `26.1.2.80` jar removed)
- `distribution/client-installer/payload/berongsmp-1.0.0.jar` (rebuilt from this branch)
- `install-berongsmp.ps1` and `README.txt` version strings updated
- `Install-BerongSMP.exe` recompiled via `ps2exe`; `BerongSMP-Client-Installer.zip` repackaged

Per the plan's scope boundary, this migration does **not** touch the live MCServerHost server,
upload anything to Google Drive, or touch the landing page — those remain the user's own next step.

## Commits (in order)

1. `chore: bump to NeoForge 26.2.0.23-beta pins`
2. `fix: decouple WorldEdit artifact MC-version suffix from minecraft_version`
3. `fix: adapt to MC 26.2's colored-block registry restructure`
4. `fix: replace removed I18n.exists with Language.getInstance().has`
5. `fix: wrap EntitySpawnReason in EntitySpawnRequest for MC 26.2's EntityType.create`
6. `chore: update installer script to NeoForge 26.2.0.23-beta`
7. `chore: update installer README to NeoForge 26.2.0.23-beta`
