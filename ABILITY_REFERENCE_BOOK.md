# Identity 2 Ability Reference Book (Draft v2)

## 1. Controls
- `V` = Primary ability.
- `B` = Secondary ability.
- `V` and `B` are defaults and can be rebound in Minecraft Controls.
- No active ability is tied to `W`, `A`, `S`, or `D`.
- Shulker only: while open, left click or right click fires a Shulker bullet.

## 2. Cooldown Rules
- Primary cooldown source: `common/src/main/resources/data/minecraft/identity2/identity_ability/*.json`.
- Primary total lockout is `cooldown + use_duration`.
- Secondary cooldown source:
- Elder Guardian uses `IdentitySettings.elderGuardianMiningFatigueCooldownTicks` (default `600`).
- Shulker teleport uses `IdentitySettings.shulkerTeleportCooldownTicks` (default `80`).
- Other secondary abilities default to the identity cooldown if a custom secondary cooldown is not defined.

## 3. Ability Catalog

| Morph | Primary (`V`) | Secondary (`B`) | Cooldown | How To Use |
|---|---|---|---|---|
| `blaze` | Fire small blaze fireball. | None. | Primary `60` | Aim and press `V`. |
| `breeze` | Fire wind projectile (Breeze Wind Charge or Wind Charge fallback). | None. | Primary `50` | Aim and press `V`. |
| `chicken` | Run configured command (`execute at @s run setblock ~ ~-1 ~ stone`). | None. | Primary `100` | Press `V`; behavior depends on datapack command. |
| `cow` | Cleanse self (remove active potion effects). | None. | Primary `100` | Press `V` to clear effects. |
| `creeper` | Explode at current position. | Harmless 30-tick fuse/hiss. | Primary/Secondary `200` | Press `V` to explode or `B` to hiss without exploding. |
| `elder_guardian` | Guardian laser beam attack. | Mining Fatigue pulse on nearby players. | Primary `40`, Secondary config default `600` | Laser: aim target and press `V`. Fatigue pulse: press `B`. |
| `ender_dragon` | Launch dragon fireball toward looked point or target. | Cosmetic backflip animation. | Primary/Secondary `120` | Aim and press `V`; press `B` to flip. |
| `enderman` | Teleport to looked location with safe-space check. | None. | Primary `100` | Look where you want to blink and press `V`. |
| `evoker` | Spawn a forward line of Evoker fangs. | None. | Primary `120` | Face enemy line and press `V`. |
| `ghast` | Launch large ghast fireball. | None. | Primary `40` + `use_duration 20` | Hold timing between uses; press `V` at target. |
| `guardian` | Guardian laser beam attack. | None. | Primary `40` | Keep crosshair on target and press `V`. |
| `iron_golem` | Heavy slam attack with knockback. | None. | Primary `40` | Get close or mid-range, then press `V`. |
| `llama` | Fire llama spit projectile. | None. | Primary `40` | Aim and press `V`. |
| `hoglin` | Ram attack with knockback and attack animation. | None. | Primary `120` | Face target and press `V`. |
| `zoglin` | Ram attack with knockback and attack animation. | None. | Primary `120` | Face target and press `V`. |
| `pufferfish` | None. | Puff to max size briefly. | Secondary `60` | Press `B` to puff up. |
| `ravager` | Ram attack with knockback and attack animation. | None. | Primary `120` | Face target and press `V`. |
| `trader_llama` | Same as llama spit projectile. | None. | Primary `40` | Aim and press `V`. |
| `shulker` | Toggle shell open or close state. | Random teleport. | Primary `10`, Secondary config default `80` | Press `V` to open. While open, left or right click shoots bullet. Press `V` to close. Press `B` to teleport. |
| `snow_golem` | Snowball burst volley. | None. | Primary `30` | Aim and press `V` for spread attack. |
| `villager` | Open villager trading UI. | None. | Primary `20` | Press `V` to trade. Sneak + look at workstation + `V` to acquire job. |
| `wandering_trader` | Open wandering trader trading UI. | None. | Primary `20` | Press `V` to trade. |
| `warden` | Delayed Sonic Boom strike with knockback; still plays beam/sound feedback on a miss. | None. | Primary `120` | Aim and press `V`. |
| `witch` | Throw random negative splash potion (harming, poison, slowness, weakness). | Heal self for 4 HP. | Primary/Secondary `100` | Aim and press `V`; press `B` to heal. |
| `wither` | Launch wither skull projectile. | None. | Primary `120` | Aim and press `V`. |

## 4. Elder Guardian Secondary Details
- Radius: `IdentitySettings.elderGuardianMiningFatigueRadius` (default `50.0`).
- Duration: `IdentitySettings.elderGuardianMiningFatigueDurationTicks` (default `1200`).
- Amplifier: `IdentitySettings.elderGuardianMiningFatigueAmplifier` (default `2`, zero-based).
- Cooldown: `IdentitySettings.elderGuardianMiningFatigueCooldownTicks` (default `600`).

## 5. Shulker Usage Notes
- `V` toggles open and closed.
- Open state no longer locks movement.
- While open, left click and right click both request Shulker bullet fire.
- `B` teleports and is not tied to movement keys.
- Teleport cooldown: `IdentitySettings.shulkerTeleportCooldownTicks` (default `80`).

## 6. Villager Job Acquisition
- Required flow: sneak, look at a workstation block within reach, press `V`.
- Supported workstation mapping includes:
- `blast_furnace` -> `armorer`
- `smoker` -> `butcher`
- `cartography_table` -> `cartographer`
- `brewing_stand` -> `cleric`
- `composter` -> `farmer`
- `barrel` -> `fisherman`
- `fletching_table` -> `fletcher`
- `cauldron` -> `leatherworker`
- `lectern` -> `librarian`
- `stonecutter` -> `mason`
- `loom` -> `shepherd`
- `smithing_table` -> `toolsmith`
- `grindstone` -> `weaponsmith`

## 7. Fallback Ability (All Other Morphable Mobs)
- If a mob has no dedicated ability entry, fallback active ability is used.
- Primary (`V`): short melee strike if a target is hit.
- Primary (`V`) fallback behavior when no target is hit: short forward dash.
- Default fallback cooldown: `20`.

## 8. Multiplayer and Sync Guarantees
- Client sends ability request packet.
- Server validates cooldown and state, then executes ability.
- Server syncs custom state and visuals to clients.
- Ability execution is server authoritative.
