# Runtime verification: Minecraft 1.21.11 and 26.2

The in-game results below predate the 2.2.4.2 legacy-system removal. The 2.2.4.2 update was checked with builds and packaged-artifact scans; no new in-game tests were requested.

Verified October 27, 2026 (America/Toronto). Eight failures reproduced in each unpatched client. After patching, each version passed all 16 checks, including an actual death and respawn.

Starting branches: `1.21.11` at `8420d27` and `26.2` at `f62122b`. Existing uncommitted authentication cleanup and build/version changes on `26.2` were preserved. No commits or publishing were performed.

The tests ran inside actual desktop Fabric clients with integrated Minecraft servers and separate disposable worlds named `New World`. A development-only harness exercised the connected player's real `ServerPlayer`, damage methods, attributes, dimension refresh, server ticks, and world collision checks. The death test used the client's normal respawn request. These were automated checks inside running games, supported by in-game chat screenshots.

| Check | Before, both versions | After, both versions |
|---|---|---|
| Death rule `NONE`, command feedback disabled | Effective rule became `WIPE_ALL` | Rule remains `NONE` |
| Cow morph health with +10 health and a 25% multiplier | Target 10; player received 25 | Player receives 10 |
| Selected morph with changed max health | Active entity 37; player received default cow health 10 | Player receives 37 |
| One sword attack as an iron golem | Two damage events | One damage event |
| Damage attributed to the player's own morph | Invulnerability check returned false | Check returns true |
| Warden exit collision probe five blocks away | Moved the player five blocks | Does not move the player |
| Chicken eye height after crouching and refreshing dimensions | Player eye 1.27; morph eye 0.644 | Both 0.644 |
| Flight grant reset while keeping a bat morph | `mayfly` stayed false after subsequent ticks | `mayfly` restored |
| Production unlock splitting: 1,000 variant references | Already passed: two entries; largest encoded payload 23,959 bytes | Still passes unchanged |

The selected-morph health check changed the active cow's max-health attribute to 37 to model variant-dependent attributes. This verifies preservation of the active entity's attributes, without establishing compatibility with any particular modded dragon or variant adapter.

Seven additional post-patch controls passed in both versions:

- An unarmed iron-golem morph produces one damage event.
- An ordinary zombie still damages the morphed player: 10 health becomes 8.
- Returning to human restores external health modifiers: 37.5 maximum health in this test.
- Returning to human restores the normal 1.62 eye height.
- Returning to human revokes the bat's flight grant.
- A completely obstructed Warden exit leaves the player hidden with collision disabled.
- With command feedback disabled and death rule `NONE`, actual death and client respawn produce a new server player that retains the cow morph.

The fixes affect six Java files per branch: `IdentitySettings`, `IdentityProgression`, `WardenBurrowManager`, `EntityMixin`, `LivingEntityMixin`, and `PlayerEntityMixin`. Unarmed eligible morphs use one identity attack; held weapons use the vanilla player attack. Warden probes test translated bounding boxes, and a blocked safe exit aborts before clearing the hidden state.

The earlier audit's packet-splitting claim was a false positive. The production path already calls `addChunkedUnlockSyncEntries` before sending. No packet fix was applied. An early diagnostic bypassed that builder and failed to capture Architectury's wire wrapper; those preliminary measurements are excluded. The corrected check uses the production entry builder and actual Minecraft packet codec inside the running server.

The earlier candidates concerning broader respawn attribute cleanup, server-to-client configuration synchronization, sounds, and the landing hook were not established as active bugs by this pass and were left unchanged. Feature additions were outside this patch.

| Runtime | Dependencies | Java |
|---|---|---|
| 1.21.11 | Fabric Loader 0.18.4; Fabric API 0.141.3+1.21.11; Architectury 19.0.1; GabousLibs 1.9 | Temurin 21.0.8.9 |
| 26.2 | Fabric Loader 0.19.3; Fabric API 0.154.2+26.2; Architectury 21.0.4; GabousLibs 1.9 | Temurin 25.0.3.9 |

Final production builds succeeded for Fabric and NeoForge on both branches with `:fabric:clean :fabric:build :neoforge:build --offline --console=plain`. Release jars were inspected to confirm audit classes and entrypoints were absent. NeoForge was build-verified; in-game sessions used Fabric. Dedicated-server multiplayer and additional modpacks were not exercised.

The temporary harness, original source snapshots, and disposable worlds remain under `G:/mods/identity2-1.21.11/build/runtime-audit`, outside release jars. Final evidence:

| Version | Before log | After log | Before screenshot | After screenshot |
|---|---|---|---|---|
| 1.21.11 | [8 failures](verification/2026-10-27/1.21.11-before.txt) | [16 passes](verification/2026-10-27/1.21.11-after.txt) | [In-game](verification/2026-10-27/1.21.11-before.png) | [In-game](verification/2026-10-27/1.21.11-after.png) |
| 26.2 | [8 failures](verification/2026-10-27/26.2-before.txt) | [16 passes](verification/2026-10-27/26.2-after.txt) | [In-game](verification/2026-10-27/26.2-before.png) | [In-game](verification/2026-10-27/26.2-after.png) |
