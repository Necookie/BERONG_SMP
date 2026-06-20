# sim-status

Quick sanity check: compile the mod and scan the latest server log for warnings/errors. Use this before a full build or when something feels broken.

## Steps

1. Run `./gradlew compileJava` and capture output
2. If compile fails: surface the first error with file path and line number; stop here
3. If compile succeeds: read `run/logs/latest.log` (last 100 lines) and scan for:
   - `ERROR` or `WARN` lines from `net.necookie.disastersim`
   - Any `NullPointerException` or `ClassCastException` stack traces
   - Structure placement failures ("Failed to load structure", "Could not read")
   - Session state errors ("No session found", "endSimulation called with no active session")
4. Summarise findings in a short list:
   - Compile: PASS / FAIL (N errors)
   - Runtime log: CLEAN / N issues found (list them)
5. If all clean, say so in one line

## Notes

- Log location: `run/logs/latest.log` (relative to project root)
- The server must have been run at least once for the log to exist
- Use `/build-check` if you only want the compile step without log scanning
