# Identity2 Feature Inventory (Current Code)

This document lists what is **currently implemented and wired** in the active Identity2 code (`common/`, `fabric/`, `neoforge/`).

## 1. Core Morph & Progression

- Morph system supports `kill -> unlock -> morph` progression.
- Morphable identities are:
  - All entity types except `MobCategory.MISC`
  - Plus explicit allowlist: `minecraft:player`, `minecraft:iron_golem`, `minecraft:snow_golem`, `minecraft:villager`, `minecraft:wandering_trader`
- Unlocks are stored per-player and synced to client caches.
- Variant unlocks are tracked per identity (tokenized variant NBT).
- Killing entities unlocks their identity/variant (`enableIdentityKillUnlocks`, `identityKillsRequired`).
- Kill-based unlock progression is enforced on actual morph usage as well as UI visibility / command suggestion paths.
- Killing players can unlock:
  - Their current morphed identity variant, if they were morphed
  - Otherwise a `minecraft:player` identity variant carrying victim skin UUID/name
- Morph state persists and is restored on login/respawn.
- Runtime safety: identities that fail to instantiate are temporarily disabled for that runtime and rejected on morph attempts.

## 2. Morph State, Shape, and Stats

- Morph selection syncs as:
  - `identity2.identity_type`
  - `identity2.identity_variant`
- Previous morph and transition state syncs as:
  - `identity2.previous_identity_type`
  - `identity2.previous_identity_variant`
  - `identity2.transition_start_tick`
  - `identity2.transition_duration_ticks`
- Collision box width/height and standing eye height are synced to identity shape.
- In-wall suffocation checks for morphed players use real collision-shape overlap to reduce false corner/side suffocation.
- Health scaling is implemented (`scalingHealth`) with max cap (`maxHealth`).
- Sheep morph has custom width scaling adjustment.
- Generic morph attribute sync intentionally does not copy raw `Attributes.MOVEMENT_SPEED` onto players, because many mob movement-speed values are not semantically compatible with player movement.

## 3. Ability System

- Data-driven registry: `identity2:identity_ability`
- Ability definition fields:
  - `icon`, `command`, `cooldown`, `use_duration`, `predef`, `override_attack`
- Ability input key: `V`
- Ability cooldown HUD icon is rendered from the ability `icon` item.
- Ability packets support:
  - Execute
  - Cooldown tick callbacks
  - Passive tick callbacks
  - Attack override callbacks

### 3.1 Explicit Ability Definitions (datapack JSON)

- `minecraft:blaze` (60): shoots small fireball
- `minecraft:chicken` (100): command ability (`execute at @s run setblock ~ ~-1 ~ stone`)
- `minecraft:cow` (100): clears own effects + drink sound
- `minecraft:creeper` (200): self explosion (powered morph stronger)
- `minecraft:elder_guardian` (600): guardian beam behavior (elder effects)
- `minecraft:ender_dragon` (120): launches dragon fireball projectile; secondary triggers cosmetic backflip
- `minecraft:enderman` (100): targeted teleport
- `minecraft:evoker` (120): evoker fangs line attack
- `minecraft:ghast` (40, use_duration 20): large fireball + charge/warning timing
- `minecraft:guardian` (600): beam-like hit, fatigue + damage
- `minecraft:hoglin` (120): ram attack with attack animation
- `minecraft:iron_golem` (40): heavy melee strike/knockback
- `minecraft:llama` (40): llama spit projectile
- `minecraft:pufferfish` (60): secondary max puff
- `minecraft:ravager` (120): ram attack with attack animation
- `minecraft:shulker` (10, override_attack false): open/close + bullet fire while open + secondary teleport
- `minecraft:snow_golem` (30): snowball burst volley
- `minecraft:trader_llama` (40): llama spit projectile
- `minecraft:witch` (100): random negative splash potion throw
- `minecraft:wither` (120): wither skull projectile
- `minecraft:zoglin` (120): ram attack with attack animation

### 3.2 Generic Fallback Ability

For morphs with no specific predef/command ability, non-`MISC` entities get a fallback active ability:

- Short-range strike (damage based on identity attack attribute, clamped)
- Or forward dash if no target is hit

## 4. UI, Controls, and Variant Selection

- Identity menu key: `G`
- Menu supports:
  - Search by id
  - Filters: All / Unlocked / Locked
  - Scrollable list with per-entry entity preview rendering
  - Preview pane with larger live entity render
  - Return to original form button
- Variant selection screen opens when identity has multiple variants.
- Variant lock enforcement is respected when unlock requirement is enabled.
- Favorite morph slots:
  - Morph keys: `F6`, `F7`, `F8`
  - Save current morph keys: `F9`, `F10`, `F11`
- Favorites store both identity id and variant NBT.

## 5. Variant Discovery

- Known variant support:
  - Sheep colors
  - Axolotl variants
  - Cat string variants (sample-based)
- Generic numeric variant probing:
  - Candidate keys scanned: `Color`, `Variant`, `variant`, `Type`, `type`, `Skin`, `skin`, `Form`, `form`
  - Tries bounded numeric range and validates by load/save roundtrip
- Safe fallback to one default variant if discovery fails.

## 6. Rendering and Visual Effects

- Render substitution: players/entities render as their current morph identity.
- Morph transition renderer can blend previous/current identities during transition window.
- Transition blending is suppressed while mounted or in swimming/crawling pose to avoid duplicate render states during special movement states.
- Optional transition particles (`enableMorphTransitionParticles`).
- Unlock acquisition tendril particles (`enableMorphAcquisitionTendrils`, `morphAcquisitionAnimationTicks`).
- First-person hand rendering attempts to map hand/arm from morph model where possible.
- Player morphing into `minecraft:player` variant can render target player skin (UUID/name based).
- FOV clamp for morphed players to avoid extreme spikes.
- Hidden model parts can be controlled via custom data keys prefixed `hidden_parts.`.

## 7. AI, Traits, Movement, and Interaction Behavior

- Identity-aware targeting:
  - During target searches, AI can evaluate against current morph identity shape/type
  - Hostile-vs-hostile ignore behavior is implemented behind `hostilesIgnoreHostileIdentityPlayer`
- Passive trait tags supported:
  - `identity2:can_fly`
  - `identity2:can_breathe_underwater`
  - `identity2:burns_in_daylight`
  - `identity2:slow_falling`
- Runtime trait overrides supported through config lists:
  - Added/removed flying entities
  - Added/removed aquatic entities
  - Arbitrary tag assignment adds/removes (`tag=entity` format)
- Flight grant for fly-capable identities preserves vanilla creative/spectator behavior.
- Aquatic identities keep player air replenished.
- Daylight-burning and slow-falling traits are applied passively to morphed players.
- Sound substitution for hurt/death uses identity sounds when enabled (`useIdentitySounds`).
- Villager trade interaction with morphed players:
  - Right-clicking a player morphed as villager/wandering trader triggers server-side villager trade interaction
  - Self-trade controlled by `canTradeWithHimSelf`

## 8. Commands

Primary command root: `/identity`

- `/identity morph <identity_id>`
- `/identity clear`
- `/identity list`
- `/identity unlock <identity_id> [target]` (admin)
- `/identity unlock all [target]` (admin)
- `/identity ability list`
- `/identity ability info <identity_id>`
- `/identity ability current`
- `/identity config list` (admin)
- `/identity config get <key>` (admin)
- `/identity config set <key> <value>` (admin, runtime only)
- `/identity config add <key> <value>` (admin, list fields)
- `/identity config remove <key> <value>` (admin, list fields)
- `/identity config clear <key>` (admin, list fields)

Notes:

- Morph requests and commands enforce swap permissions (`enableSwaps`, `allowedSwappers`, operator bypass).
- If `requireUnlockedIdentityForMorph` is true, non-ops need unlocks (and variant unlocks for variant morph packets).
- Config command edits are runtime-only (no persistence writeback in current code).

## 9. Networking and Multiplayer Sync

- Custom sync payloads for string/double/bool custom data.
- Morph model + shape + transition data are synced to player and tracking players.
- Server entity tracker sends current custom data on pairing.
- Client queues pending sync packets and applies them when entities become available.
- Identity entity data uses negative entity IDs for dedicated sync lane.

## 10. Inventory/Drop Behavior

- Soulbound effect integration (`prevent_equipment_drop`) is respected on death drops:
  - Player inventory drop filtering
  - Equipment drop filtering
- A soulbound enchantment datapack entry exists (`data/minecraft/enchantment/soulbound.json`).

## 11. Lifecycle Hooks and Advanced Custom-Data Hooks

- On join/leave, function tags can run if present:
  - `#identity2:on_before_player_join`
  - `#identity2:on_before_player_leave`
- Entity custom-data command hooks:
  - `on_tick`
  - `on_removed`
  - `on_removed_<reason>` (e.g. `killed`, `discarded`, `dimension_change`)
- Custom movement modifiers via custom data:
  - `land_speed_multiplier_override`
  - `horizontal_collision_speed_multiplier_override`

## 12. Death Handling Options (Implemented)

- `loseAllMorphsOnDeath`: clears unlocked identities and current morph.
- `revokeIdentityOnDeath`: clears only current morph.

## 13. Cross-Loader Support

- Architectury-based shared core with Fabric + NeoForge loaders.
- Identity ability registry is registered/synced on both platforms.

## 14. Present in Settings but Not Wired in Current Runtime Logic

The following config fields exist but are not referenced by active gameplay logic (outside `/identity config` reflection listing):

- `overlayIdentityUnlocks`
- `overlayIdentityRevokes`
- `identitiesEquipItems`
- `identitiesEquipArmor`
- `hostilesForgetNewHostileIdentityPlayer`
- `wolvesAttackIdentityPrey`
- `ownedWolvesAttackIdentityPrey`
- `villagersRunFromIdentities`
- `foxesAttackIdentityPrey`
- `playAmbientSounds`
- `hearSelfAmbient`
- `enableFlight`
- `hostilityTime`
- `advancementsRequiredForFlight`
- `enableClientSwapMenu`
- `showPlayerNametag`
- `renderOwnNametag`
- `forceChangeNew`
- `forceChangeAlways`
- `logCommands`
- `flySpeed`
- `killForIdentity`
- `requiredKillsForIdentity`
- `wardenIsBlinded`
- `wardenBlindsNearby`
- `forcedIdentity`

## 15. Legacy/Non-Active Code Notes

- Root-level `ability/` and `screen/` trees are legacy and not part of active module source sets.
- `common/src/main/java/net/Gabou/identity2/mixin/PlayerMorphInteractionMixin.java` is explicitly excluded from compilation.
- `AccelerateToPosCommand` and `RunMultipleCommand` classes exist but are not registered in `ModCommands`.
