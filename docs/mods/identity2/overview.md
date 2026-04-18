# Identity2 Overview

This bundle documents the current `1.21.1` branch of `Identity2` (`mod_version=1.3.4`).

## What it does

Identity2 is a morph mod. Players can unlock supported entities, morph into them, keep selected variant data, and use identity-specific abilities.

## Main features

- Unlock and morph progression.
- Identity menu with search, filters, and variant selection.
- Built-in and datapack-driven identity abilities.
- Morph transitions, model/shape sync, and health scaling.
- Optional progression systems for charges, soul jars, and permanent morphs.
- Public addon API for abilities and variant adapters.

## Who it is for

- Players who want morph gameplay.
- Server admins who want progression and runtime config controls.
- Addon authors integrating custom abilities or variants.

## Dependencies

- Both supported loaders: `architectury`, `gaboulibs`
- Fabric only: `fabric-api`
- Java: `21`

## Official compatibility scope

- Supported Minecraft version in this branch: `1.21.1`
- Supported loaders in this branch: `Fabric`, `NeoForge`
- Official per-mod compatibility list: `UNKNOWN`

## Support notes

- Morphing can be gated by unlock requirements depending on config.
- The default player keybinds are `G` for the identity menu and `V` for the active ability.
- Some older config alias keys are still accepted on load but are normalized to newer canonical keys on save.
