# build-check

Compile the mod and report any errors with file:line context. Fast check — does not run the server or build the full JAR.

## Steps

1. Run `./gradlew compileJava`
2. If it succeeds: report "Compile OK" and nothing else
3. If it fails:
   - List each error with: file path (relative to `src/`), line number, error message
   - Group errors by file
   - Identify the root cause if multiple errors share a common origin (e.g., a renamed method causing cascade failures)
   - Suggest the fix for each error, referencing the actual class/method involved

## Common error patterns in this codebase

- `cannot find symbol` on `SavedData` methods → check `TutorialSavedData.java`; API changed in MC 26.1.2 (`SavedDataType` pattern)
- `cannot find symbol` on `ServerPlayer.level()` → should be `ServerPlayer.serverLevel()`
- `KeyMapping` import errors → MC 26.1.2 moved this class; check `KeyMappings.java`
- Missing `@Override` / wrong return type on `StructurePlacer` implementations → check `SimulationStructureLoader` and `SchemLoader`

## Notes

- This is equivalent to running `./gradlew compileJava` yourself but with smarter error surfacing
- For a full build with JAR output use `./gradlew build`
- For runtime issues (not compile errors) use `/sim-status`
