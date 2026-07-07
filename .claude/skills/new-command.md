# new-command

Scaffold a new Brigadier command with correct boilerplate for this project. `ModCommands` is a thin registration shell — actual commands live in `RegistrationCommands`, `ItemCommands`, `SimulationCommands`, or `BfpAdminCommands`; pick the file whose domain matches the new command (or discuss if none fits).

## Usage

Call with the command name and optional details:
- `new-command <name>` — basic no-arg command, no permission restriction
- `new-command <name> op` — requires game master (op level 2+)
- `new-command <name> arg:<type>` — command with one argument; type is `double`, `int`, or `string`

## Steps

1. Read `src/main/java/net/necookie/disastersim/command/ModCommands.java` to see the delegation, then read the sub-file that matches the new command's domain (items -> ItemCommands, sim control -> SimulationCommands, /bfp admin -> BfpAdminCommands, registration -> RegistrationCommands)
2. Add the new command block inside that file's `register()` following the existing style:
   - Use `Commands.literal("<name>")` as the root node
   - If `op`: chain `.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))`
   - If argument: use `Commands.argument("<argName>", <TypeArgumentType>.type())` and retrieve with `<TypeArgumentType>.get<Type>(context, "<argName>")`
   - `.executes()` lambda: get the player with `context.getSource().getPlayer()`, guard with `if (!source.isPlayer()) return 0;`, return `1` on success
   - Add a short comment above the block describing what the command does and any permission rationale
3. Report the exact lines added and remind the user to test with `/build-check`

## Existing commands for reference

| Command | Permission | Args | Purpose |
|---|---|---|---|
| `/sim_fire` | none | — | Start fire simulation |
| `/sim_earthquake` | none | optional double magnitude | Start quake simulation |
| `/sim_magnitude` | op | double value | Override quake magnitude |
| `/sim_stop` | none | — | End current simulation |
| `/spawn_lspu` | op | — | Place LSPU structure |
| `/get_extinguisher` | none | — | Give fire extinguisher item |

## Notes

- Do not register commands on the client — `ModCommands.register()` is called only on the server via `RegisterCommandsEvent`
- For commands that need world mutation, ensure they're called on the server thread (they are, since `CommandSourceStack` is always server-side)
