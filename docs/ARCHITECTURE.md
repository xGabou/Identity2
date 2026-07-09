# Identity 2 — Architecture Notes

Written while auditing the 1.21.1 branch (July 2026). Applies to `common` + the
thin `fabric`/`neoforge` loader modules; the 1.20.1 Forge branch shares the same
shape with older APIs.

## Module layout

- `common/` — all gameplay code. Built with Architectury; mixins in
  `identity2.mixins.json` (both sides) and `identity2.client.mixins.json`.
- `fabric/`, `neoforge/` — bootstrap + two platform interfaces only:
  `ModRegistryPlatform` (datapack registry declaration) and `ModClientPlatform`.
- `gabous-libs` (external CurseMaven dep) — auth challenge flow, networking
  helpers, misc utils. The auth packets live there, **not** in this repo
  (`ModPackets` deliberately no longer declares `auth_challenge*` ids).

## Morph state ("identity")

There is no capability/attachment: state is added to **every Entity** by
`EntityMixin` (`common/.../mixin/EntityMixin.java`), which implements
`util/EntityAccessor`:

- `currentIdentity` — a real, un-spawned `Entity` instance of the morph type,
  attached to the host (player). It has `id = -host.getId()` and
  `identityOf = host` set on it. It is *not* added to the world's entity list.
- `persistentData` — a `CompoundTag` saved under `identity2_custom_data` in the
  host's NBT (`saveWithoutId`/`load` injects). All morph metadata lives here:
  selected type/variant (`identity2.identity_type`, `identity2.identity_variant`),
  width/height overrides, transition data, unlock lists, ability animation
  countdowns, silverfish/shulker flags…
- Ability cooldowns are plain int fields on the mixin (not persisted).

Server side: `EntityMixin.identityFix` (Entity.tick TAIL) mirrors
position/motion/air/equipment into the identity, then ticks it via
`ServerLevel.tickNonPassenger` with `NoAi` forced on. Client side:
`ClientWorldMixin` ticks the identity after the host entity ticks and
`IdentityRenderStateHelper.syncIdentityVisualState` copies render-relevant
state per frame (`EntityRenderDispatcherMixin`).

The player's own dimensions/eye height are overridden from custom data
(`width_override`/`height_override`) in `getDimensions`/`setBoundingBox`
injects; the cached `Entity.eyeHeight` field is restored to the identity's eye
height after every `refreshDimensions` (pose changes would otherwise reset it —
this was the "thrown items spawn at player head height after crouching" bug).

## Persistence + sync

Everything syncs through three generic key/value payloads
(`CustomEntityDataS2CPacketPayload` double / `...String...` / `...Bool...`),
which write into the target entity's client-side custom data tag. Sync points:

- `PlayerManagerMixin.placeNewPlayer` — full custom-data dump to the joining
  player and everyone in their level; queues a delayed morph reapply (20t).
- `EntityTrackerEntryMixin (ServerEntity.addPairing)` — full dump to any player
  that starts tracking an entity, plus the identity's `SynchedEntityData`
  (sent with the negative entity id).
- `IdentityProgression.syncMorphData` / `IdentityApi.sync*` — incremental
  updates, broadcast to the player's current level.
- Unlocked identities go through `IdentityUnlockSyncS2CPacketPayload`, split
  into ≤24 kB chunks (`sendUnlockSyncPackets`).

On the client, `Identity2Client.onUpdateCustomData` applies values and calls
`setCurrentIdentity(...)` when the identity keys change; packets that arrive
before the target entity exists are queued and retried for up to 100 ticks.

**Assessment (internal issue "sync is manual and fragile"):** it is manual, but
the three sync points above cover join, respawn, dimension change and tracking
start, and string values are size-guarded now. Replacing this with
NeoForge Attachments + Forge Capabilities + a Fabric component would be a
three-way rewrite of the persistence layer with no behavioral win — the mixin
field approach is already loader-neutral. If it is ever done, the seam to cut
is `EntityAccessor.getCustomData()`: back that one method per loader
(NeoForge `AttachmentType<CompoundTag>`, Forge capability, Fabric
`cardinal-components`-style or a mixin field as today) and everything else
stays unchanged. Until a concrete need appears (e.g. another mod must attach
data), keep the current approach.

## Abilities

- Datapack registry `identity2:identity_ability`
  (`ModRegistries.IDENTITY_ABILITY_KEY`, JSON in `data/*/identity2/identity_ability/`)
  gives per-entity icon/cooldown/command/predef pointer.
  Registry declaration is per loader (`ModRegistryPlatform`); runtime contents
  are captured from the server's `RegistryAccess` on level load (Architectury
  `LifecycleEvent.SERVER_LEVEL_LOAD`) — a datagen `VanillaRegistries` lookup can
  never contain datapack entries and exists only as a legacy fallback.
- Built-in behaviors live in `PredefIdentityAbilities.predef`
  (`BuiltinIdentityAbility`: execute / executeSecondary / passiveTick /
  overrideAttack), with a generic fallback for any morphable mob.
- Flow: client keybind (`Identity2Client.onClientTickEnd`) → C2S
  `IdentityAbilityPacketPayload(actionCode)` → `ModPackets.handleIdentityAbilityPacket`.
  The client also sends a passive-tick packet **every tick** while morphed;
  that is what drives server-side `passiveTick`.
- Ability/animation countdowns are stored in custom data as an absolute
  **game-time** expiry plus a `.start` window key
  (`PredefIdentityAbilities.identity2$setSyncedTicks`). They must never be
  stored in `Entity.tickCount` units: tickCount differs between server player
  and every client view, which is what froze creeper swell/pufferfish states.

## Rendering

`PlayerEntityRendererMixin`/`EntityRenderDispatcherMixin` swap the renderer to
the identity entity's renderer. `IdentityRenderStateHelper` copies walk
animation, hurt/swing state, per-species quirks (bee roll, wolf shake, camel
sit, warden `AnimationState`s…). Species whose animation is self-driven by
their own `aiStep` (squid tentacles, rabbit hop cycle) rely on the client-side
identity tick — do not overwrite their animation fields per-frame from derived
formulas; trigger the vanilla cycle instead (see rabbit `handleEntityEvent(1)`).

## Loader specifics / gotchas

- **Never reflect on Minecraft method names in common code** — names are
  Mojmap at NeoForge runtime but intermediary at Fabric runtime. This is why
  the reflective variant discovery is being replaced by explicit variant
  tables (`IdentityVanillaVariantHelper`) type by type, and why
  `MFCheck.isMethodEmpty(..., "checkFallDamage")` only works on NeoForge — the
  `identity2:can_fly` / `identity2:slow_falling` tags are the reliable path.
- Reflection into *our own* classes (old NeoForge registry platform) is never
  needed; loader modules compile against `common` directly.
- Player motion is client-authoritative: movement tweaks (strider lava swim,
  slow-fall clamps) must run on the client, and after `travel()` if they need
  to survive gravity/friction (see `LivingEntityMixin.identity2$applyMorphMovementAssists`).

## Known deferred items

- `IdentitySelectionScreen` uses a hand-rolled button grid. Replacing it with
  `ObjectSelectionList<Entry>` would give free scrolling/keyboard nav, but it
  is a UI rewrite with visual-regression risk; do it in a dedicated change
  with in-game verification, not alongside gameplay fixes.
- Head-position tuning (ravager/hoglin/zoglin), iron golem swing offset and
  ridden-vehicle seat offsets carry `TODO(tuning)` markers — they need
  in-game visual iteration.
