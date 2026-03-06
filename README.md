# Identity2

Identity2 is an Architectury morph/identity mod for Minecraft `1.21.11` targeting both Fabric and NeoForge.

It lets players unlock entities, morph into them, use identity-specific abilities, and keep variant data (for example sheep color) so rendering matches the selected variant.

## Highlights

- Morph system with unlock progression (`kill -> unlock -> morph`).
- Client identity menu with filtering and search.
- Dynamic variant discovery with safe fallback.
- Known variant support for sheep, axolotl, and cat.
- Generic numeric variant probing for modded entities (safe bounded scan).
- Ability registry/data driven identity abilities (`identity2:identity_ability`).
- Identity shape sync (`width_override`, `height_override`) and eye height sync.
- Flight handling for fly-capable identities without removing existing creative/spectator behavior.
- Networked morph/custom data sync for all tracking players.

## Controls

Default client keybinds:

- Open identity menu: `G`
- Use identity ability: `V`

## Commands

Main command namespace:

- `/identity morph <identity_id>`
- `/identity clear`
- `/identity list`

Notes:

- Morphing checks that the identity is morphable.
- If `requireUnlockedIdentityForMorph` is enabled, non-operators must have unlocked the identity first.

## Identity Variants

Variants are stored and synced as:

- Base type: `identity2.identity_type`
- Variant payload: `identity2.identity_variant`

When a variant is selected, its NBT is applied to the spawned identity instance so model/texture/color matches the selected variant.

If no variants can be discovered, the UI safely falls back to a single default entry.

## Build Requirements

- Java `21`
- Gradle Wrapper (included)

Quick build:

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

## Run In Dev

Typical tasks:

```bash
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```

## Dependencies

Core:

- Architectury API
- Fabric API (Fabric side)
- gabous-libs

Fabric profiling helper:

- Fabric runtime expects local jar: `libs/spark-1.10.165-fabric.jar`
- It is wired as `modRuntimeOnly` in `fabric/build.gradle`.

## Project Layout

- `common/` shared gameplay logic, packets, mixins, UI, progression
- `fabric/` Fabric bootstrap and platform bindings
- `neoforge/` NeoForge bootstrap and platform bindings

## Addon Development

Addon API and integration documentation:

- [`docs/public/ADDON_DEV_GUIDE.md`](./docs/public/ADDON_DEV_GUIDE.md)

Documentation folders:

- [`docs/public`](./docs/public/README.md)
- [`docs/Gabou's thing`](./docs/Gabou's%20thing/README.md)

## Current Defaults (Code)

Important defaults are in:

- `common/src/main/java/net/Gabou/identity2/IdentitySettings.java`

Examples include unlock-on-kill behavior, swap restrictions, nametag behavior, flight toggles, and ability-related settings.

## Troubleshooting

`Unsupported identity: <id>`

- The target id is not considered morphable by `IdentityProgression.isMorphableIdentity(...)`.
- Or the identity is not unlocked and unlocked-only morphing is enabled.

Identity menu appears empty

- Ensure unlock cache is populated for the player (kill/unlock flow or server sync).
- On existing worlds, rejoining after unlock sync should repopulate entries.

Low FPS while morphed

- Profile first (`spark` on Fabric is supported via local jar above).
- Check client tick hooks and packet processing pressure.

## License

All rights reserved. See `LICENSE.txt`.
