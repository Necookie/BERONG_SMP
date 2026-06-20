# run-server

Start the NeoForge development server and monitor its output for errors or ready state.

## Steps

1. Run `./gradlew runServer --console=plain` in the background via Bash with `run_in_background: true`
2. Tell the user the server is starting and they should watch for "Done" in the console output, or use `! tail -f run/logs/latest.log` to follow the log
3. Remind them the working directory is `run/` and the config is at `run/config/berongsmp-common.toml`
4. Note that in-game commands available for testing are: `/sim_fire`, `/sim_earthquake [magnitude]`, `/sim_magnitude <value>`, `/sim_stop`, `/spawn_lspu`, `/get_extinguisher`

## Notes

- The server takes ~60–90 seconds to start on first run (downloading MC assets)
- If it crashes on startup, the most common causes are: compile errors (run `/build-check` first), missing structure NBT files, or a `SavedData` API mismatch
- World saves are in `run/world/`
- To stop cleanly: type `stop` in the server console, or kill the Gradle process
