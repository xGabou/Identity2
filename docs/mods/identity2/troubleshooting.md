# Identity2 Troubleshooting

## Game does not start

Check these first:

- Minecraft version is `1.21.1`
- Java version is `21`
- Loader is Fabric or NeoForge
- Required dependencies are installed

Common causes:

- Missing `gaboulibs`
- Missing `architectury`
- Missing `fabric-api` on Fabric
- Wrong loader or wrong Minecraft branch jar

## `/identity` command is missing

Likely causes:

- The mod did not load
- Dependencies are missing
- The wrong loader jar was installed

## Identity menu is empty

Common causes:

- The player has not unlocked any identities yet
- Unlock-restricted morphing is enabled
- You are testing an older build affected by respawn unlock cache desync

What to check:

- `/identity list`
- Config keys related to unlocks
- `known-issues.json` for older-version respawn cache behavior

## `Unsupported identity: <id>`

This usually means one of these:

- The entity is not considered morphable by current code
- The identity was temporarily disabled after a runtime load failure
- The id is invalid for the current mod pack

## Config changes do not behave as expected

Check:

- You edited `config/identity2.json`
- You used canonical key names, not legacy alias names
- The key is actually persisted by the current config manager

Important current limitations:

- Legacy alias keys are accepted on load but normalized on save
- `deathMorphRule` exists in code but is not currently part of `identity2.json`

## Villager trade command fails

Check:

- You are a player, not console
- For self-trade, `canTradeWithHimSelf` is enabled
- The target player is in the same dimension
- The acting morph is `villager` or `wandering_trader`

## When to check known issues

Check `data/mcp/mods/identity2/known-issues.json` when:

- A config key exists in code but not in the generated config file
- An older build loses visible unlock state after respawn
- A legacy config key seems to disappear after saving
