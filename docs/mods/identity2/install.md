# Identity2 Install

## Prerequisites

- Minecraft `1.21.1`
- Java `21`
- One supported loader:
  - Fabric
  - NeoForge

## Required dependencies

### Fabric

- Fabric Loader `0.18.4+`
- Fabric API
- Architectury API `13.0.8+`
- gaboulibs `1.4+`

### NeoForge

- NeoForge `21+`
- Architectury API `13.0.8+`
- gaboulibs `1.4+`

## Install steps

1. Use a `1.21.1` game instance.
2. Install the correct loader for that instance.
3. Add the required dependencies for that loader.
4. Add the matching `Identity2` jar for the same Minecraft version and loader.
5. Start the game once to let the config file generate.

## Basic verification

- The game reaches the main menu without dependency errors.
- `config/identity2.json` is created after first launch.
- The `/identity` command is registered in game.
- The default identity menu keybind is `G`.

## Install mistakes to avoid

- Do not use Forge for this branch. The current branch ships Fabric and NeoForge metadata only.
- Do not mix jars built for another Minecraft version branch.
- Do not skip `gaboulibs` or `architectury`.
- On Fabric, do not skip `fabric-api`.
