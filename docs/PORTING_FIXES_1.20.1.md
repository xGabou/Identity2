# Identity 2 July 2026 Fixes and 1.20.1 Port Guide

This document covers the fixes made while continuing
`2026-07-10-112643-investigate-and-fix-the-following-identity-2-issu.txt`, plus the
later 1.20.1 reports that exposed inventory, variant, Shulker interaction, and
Strider movement problems. It also records the later respawn-health correction
and the 1.21.1-only Armadillo ability restoration.

The code changes are implemented on `1.21.1-v2` for Fabric and NeoForge. Both
remapped loader builds and the common variant tests pass. The gameplay checklist
at the end still needs an in-game pass before release.

## Intentional Decisions

- Warden secondary burrowing remains disabled. Both the client key path and the
  server packet handler reject it. This is intentional and should be preserved
  when porting.
- Warden primary sonic boom behavior was not removed.
- Passive morph traits remain active when `enableMorphAbilities=false`; only
  primary, secondary, and override-attack actions are disabled.
- Modded variants must use `IdentityApi.registerVariantAdapter`. Automatic
  discovery by reflecting literal mapped method names was removed because those
  names are not stable in an obfuscated production client.

## Critical 1.20.1 Differences

Do not copy 1.21.1 targets or resource paths blindly.

1. Minecraft 1.21 uses `data/<mod>/tags/entity_type/` (singular). Minecraft
   1.20.1 uses `data/<mod>/tags/entity_types/` (plural). Do not port the tag
   directory rename to 1.20.1.
2. In 1.21.1, `AbstractFish.aiStep()` invokes
   `LivingEntity.makeSound(SoundEvent)`. The old 1.20.1 code used the
   `playSound(SoundEvent, float, float)` call. Verify the 1.20.1 bytecode and keep
   its actual invocation target while porting only the throttle logic.
3. The 1.21 custom payload API uses `CustomPacketPayload` and `StreamCodec`.
   Implement the same config snapshot with the 1.20.1 Forge/Fabric networking
   API instead of copying the packet class verbatim.
4. NeoForge client events in this branch do not have the same package/API as
   Forge 1.20.1. Use the Forge 1.20.1 block-overlay event if present, or a
   verified loader-specific mixin target.
5. Wolf biome variants and their dynamic registry are newer than 1.20.1. Omit
   that typed Wolf-registry branch on 1.20.1; keep the normal baby Wolf handling.
6. Registry holder APIs and `CompoundTag` getter return types differ across
   versions. Use the 1.20.1 typed APIs and the branch's `NbtCompat`; never fall
   back to `getMethod("methodName")`.

## Fix Catalog

### A. Keep-inventory armor and held-item loss

**Root cause**

Morph presentation settings were implemented by intercepting real player
equipment access. The proxy entity could then become the apparent equipment
authority, and a death-time copy wrote proxy slots back to the player. The
keep-inventory helper also tried to discover a nonexistent game-rule registry by
reflection, so it could report false even when the gamerule was enabled.

**1.21.1 changes**

- `KeepInventoryHelper` now directly reads:

  ```java
  serverPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
  ```

- Removed `Player.getItemBySlot`, `LivingEntity.hasItemInSlot`, and Mob equipment
  interception that hid or replaced the real inventory.
- Removed the death-time copy from morph proxy equipment to the player.
- The player inventory is always authoritative. `identitiesEquipItems` and
  `identitiesEquipArmor` now affect only which copied stacks are rendered on the
  proxy.
- Deleted `MobEquipmentMixin` and `IdentityEquipmentHelper` and removed the
  former from `identity2.mixins.json`.

**Files**

- `identity/KeepInventoryHelper.java`
- `mixin/EntityMixin.java`
- `mixin/LivingEntityMixin.java`
- `mixin/PlayerEntityMixin.java`
- `client/IdentityRenderStateHelper.java`
- `mixin/MobEquipmentMixin.java` (deleted)
- `util/IdentityEquipmentHelper.java` (deleted)
- `identity2.mixins.json`

**1.20.1 port**

Use `GameRules.RULE_KEEPINVENTORY` directly. Remove all mixins that change what
the real player returns from equipment accessors. Copy player equipment to the
morph only for rendering, and use `ItemStack.EMPTY` on the proxy when a render
config is disabled. Never copy proxy slots back on death.

### B. Obfuscation-safe babies and variants

**Root cause**

Variant discovery/application used reflection with development names such as
`getVariant`, `setVariant`, `getVillagerData`, `setType`, `saveWithoutId`, and
`load`. It could work in a mapped dev run and fail in an obfuscated client.
Command discovery and menu discovery also used separate implementations, which
caused missing babies and tropical fish in commands.

**1.21.1 changes**

- `EntityNbtIoCompat` calls `Entity.saveWithoutId` and `Entity.load` directly.
- `IdentityVanillaVariantHelper` uses typed entity classes and typed registries
  for Sheep, Slime/Magma Cube, Cat, Wolf, Frog, Axolotl, Horse, Goat, Villager,
  Zombie, and general `AgeableMob` state.
- `IdentityVariantDiscovery` delegates to the shared `IdentityApi` rather than
  probing getter/setter names.
- Commands pass the active `Level` into shared discovery so dynamic registries
  are available where required.
- Frog Baby and Wandering Trader Baby remain explicitly invalid.
- Valid command babies use stable NBT (`IsBaby` and `Age`) rather than reflected
  setters.
- Removed the automatic `UntamedWildsCompat` adapter because it reflected
  literal mod method names. A mod integration must register a typed
  `IdentityVariantAdapter` through the public API.
- Removed the dead reflective Villager/registry variant block from `EntityMixin`
  and reflective variant extraction from `IdentityProgression`.

**Files**

- `api/IdentityApi.java`
- `client/identity/IdentityVariantDiscovery.java`
- `commands/IdentityCommand.java`
- `identity/IdentityProgression.java`
- `identity/IdentityVanillaVariantHelper.java`
- `identity/IdentityVariantNbtHelper.java` (shared application path)
- `util/EntityNbtIoCompat.java`
- `mixin/EntityMixin.java`
- `compat/UntamedWildsCompat.java` (deleted)
- `api/IdentityVariantDiscoveryTest.java`

**1.20.1 port**

Use direct 1.20.1 calls and types. It is acceptable to use stable entity NBT or
an accessor mixin when a method is not public. It is not acceptable to use
`Class#getMethod("mappedName")`, `getDeclaredMethod`, or a helper that scans for
a mapped name. Mod-specific variants should have a compile-time integration or
register an adapter from the mod's own code.

### C. Tropical fish command variants

**Root cause**

The menu had tropical fish enumeration, but command discovery did not share it.

**Fix**

`IdentityTropicalFishVariants.discover` enumerates
`TropicalFish.COMMON_VARIANTS`, writes each packed ID to `Variant`, and is used
by shared API discovery for both commands and menus. A regression test checks
that packed variants are nonempty and unique.

**Files**

- `identity/IdentityTropicalFishVariants.java` (new)
- `api/IdentityApi.java`
- `client/identity/IdentityVariantDiscovery.java`
- `commands/IdentityCommand.java`

### D. Live client config sync and ability disable config

**Root cause**

`enableMorphAbilities` was server-authoritative, but the client still consumed
ability keys, applied cooldowns, and canceled override attacks. Other client-side
settings could also disagree with a dedicated server until reconnect/restart.

**Fix**

- Added `enableMorphAbilities` to `IdentitySettings`.
- The server rejects primary, secondary, and override-attack packets when false.
- Passive/passive-used ticks remain allowed.
- Added `IdentityClientConfigS2CPacketPayload`, sent on login and after every
  `/identity config set/add/remove/clear`.
- The snapshot includes menu, nametag, sound, abilities, flight, flight speed,
  transition particle/timing, acquisition particle/timing, equipment rendering,
  and unlock-all-variants settings used by client code.
- Client keys are drained without sending abilities while disabled, preventing
  stale clicks from firing if the config is later re-enabled.

**Files**

- `IdentitySettings.java`
- `ModPackets.java`
- `Identity2Client.java`
- `packets/IdentityClientConfigS2CPacketPayload.java` (new)
- `commands/IdentityCommand.java`
- `mixin/PlayerManagerMixin.java`
- `mixin/client/ClientPlayerInteractionManagerMixin.java`

**1.20.1 port**

Serialize the same fields using the existing 1.20.1 packet channel. Send the
snapshot after player join and after config mutation. Keep server packet checks
even though the client is synced; client checks are UX, not security.

### E. Shulker item and block use

**Root cause**

An open Shulker morph intercepted `interact`, `useItem`, `useItemOn`, and
`interactAt`, fired a bullet, and returned success. That swallowed eating,
drinking, block placement, bucket use, and entity interaction.

**Fix**

Shulker bullets remain bound to attack/override-attack. All right-click Shulker
interceptions were removed. Normal vanilla item and block interaction now runs.
Ability-disabled mode also stops the attack override.

**File**

- `mixin/client/ClientPlayerInteractionManagerMixin.java`

### F. Strider lava floating

**Root cause**

The morph used manually selected Y velocities based on jump/crouch input. This
did not match `Strider.floatStrider` and was unreliable at lava surfaces.

**Fix**

The after-travel assist now mirrors vanilla:

```java
CollisionContext context = CollisionContext.of(player);
BlockPos pos = player.blockPosition();
if (context.isAbove(LiquidBlock.STABLE_SHAPE, pos, true)
        && !player.level().getFluidState(pos.above()).is(FluidTags.LAVA)) {
    player.setOnGround(true);
} else {
    player.setDeltaMovement(player.getDeltaMovement().scale(0.5D).add(0.0D, 0.05D, 0.0D));
}
```

The duplicate manual helpers in `EntityMixin` were removed.

**Files**

- `mixin/LivingEntityMixin.java`
- `mixin/EntityMixin.java`

### G. NeoForge Creeper puff state

**Root cause**

The client did not capture the datapack-backed Identity ability registry on
NeoForge login, so it could miss ability metadata/state used by the Creeper
secondary animation.

**Fix**

`ModRegistries.captureIdentityAbilityRegistry(RegistryAccess)` is shared by the
server-level load hook and NeoForge client login event.

The later delayed-fuse implementation is also completed:

- Creeper primary starts a 30-tick synced fuse and explodes when it expires.
- Creeper secondary only plays the priming sound, matching its current intended
  behavior.
- The missing typed `GameEvent.PRIME_FUSE` import, fuse visual updater, and
  explosion helper were added.
- `IdentityRenderStateHelper` reads the synced fuse expiry so local and remote
  clients render the same swelling as the server proxy.

**Files**

- `ModRegistries.java`
- `neoforge/client/Identity2NeoForgeClient.java`
- `PredefIdentityAbilities.java`
- `client/IdentityRenderStateHelper.java`
- `mixin/CreeperAccessor.java`

**1.20.1 port**

Use the active client/server registry access available in Forge 1.20.1. Do not
use the datagen `VanillaRegistries` snapshot as the runtime registry.
For the delayed fuse, verify the 1.20.1 Creeper field/accessor mappings and the
`Level.explode` overload, then port the same absolute-game-time countdown. Do
not resolve the swelling fields by reflected development names.

### H. Flying and slow-falling morph detection

**Root cause**

Minecraft 1.21 renamed data tag directories to singular. The files remained in
`tags/entity_types`, so `can_fly` and `slow_falling` silently did not load.

**Fix**

Moved all entity-type tags to `data/identity2/tags/entity_type/`, including
`can_fly`, `slow_falling`, `burns_in_daylight`, `can_breathe_underwater`, and
`ram_attack_ability`.

**1.20.1 port**

No directory change is needed. Keep `tags/entity_types` in 1.20.1. Port only any
missing tag contents.

### I. NeoForge silverfish first-person burrow vision

**Root cause**

Fabric reaches `ScreenEffectRenderer.getViewBlockingState`, where the common
mixin suppresses the in-wall block overlay. NeoForge patches the renderer and
uses its block screen-effect event instead, so the common injection was not on
the active NeoForge path.

**Fix**

NeoForge cancels `RenderBlockScreenEffectEvent` only for the BLOCK overlay and
only when `SilverfishBurrowManager.shouldSuppressBlockOverlay` says the player is
burrowed. Fabric keeps the verified common mixin.

**Files**

- `identity/SilverfishBurrowManager.java`
- `mixin/client/ScreenEffectRendererMixin.java`
- `neoforge/client/Identity2NeoForgeClient.java`

### J. Snow-layer cave x-ray

**Root cause**

Partial snow layers are not view-blocking. A Silverfish or Endermite eye can sit
inside the layer, so vanilla draws no overlay and exposes terrain below.

**Fix**

At the head of `ScreenEffectRenderer.renderScreenEffect`, the client checks the
eye position against the actual snow-layer height and draws that block's particle
texture through a verified invoker. The burrow suppression helper does not
suppress the normal block overlay when a snow layer covers the burrow.

**Files**

- `mixin/client/ScreenEffectRendererMixin.java`
- `mixin/client/ScreenEffectRendererInvoker.java` (new)
- `identity2.client.mixins.json`
- `identity/SilverfishBurrowManager.java`

### K. Morph transition duration and squid/glow squid animation

**Root cause**

The previous identity was discarded at 65 percent of the configured transition,
so the visible transition lasted roughly one second. Swimming pose also bypassed
transition selection, which excluded Squid/Glow Squid use cases.

**Fix**

The previous/current transition selection remains active for the full configured
duration. Swimming pose is no longer an early exit; passengers still bypass it
to avoid mounted rendering conflicts.

**File**

- `client/transition/MorphTransitionHelper.java`

### L. Fish flop sound spam

**Root cause**

The redirect targeted an old sound call. In 1.21.1, `AbstractFish.aiStep()` calls
`makeSound(SoundEvent)`, so the mixin never intercepted the flop.

**Fix**

The redirect now targets the verified 1.21.1 `makeSound` invocation. Morph proxy
fish only sound while their owning player is moving, and the sound is throttled
to once per 20 ticks.

**File**

- `mixin/AbstractFishMixin.java`

### M. Small morph riding height

**Root cause**

The old calculation compared morph height with a player whose dimensions had
already been changed to the morph, producing little or no correction.

**Fix**

The offset compares against `Player.STANDING_DIMENSIONS.height()` and applies a
proportional upward correction after vehicle positioning (50 percent for boats,
35 percent for other vehicles).

**File**

- `mixin/EntityMixin.java`

### N. Blaze/Enderman water hurt sound duplication

**Root cause**

`Player` overrides `LivingEntity.getHurtSound`. The existing LivingEntity mixin
therefore did not replace the client damage-event player's drowning sound, even
though the morph hurt sound was also played.

**Fix**

`PlayerEntityMixin` injects into the verified Player override and returns the
morph LivingEntity hurt sound whenever `useIdentitySounds` is enabled.

**File**

- `mixin/PlayerEntityMixin.java`

### O. Ambient sound frequency

**Root cause**

The previous per-tick random check and short fixed guard did not follow vanilla
mob ambient scheduling and could sound too frequent or uneven.

**Fix**

The morph now uses vanilla's increasing `ambientSoundTime` probability and
resets to the negative identity ambient interval after a sound.

**File**

- `mixin/LivingEntityMixin.java`

### P. Creative players still targeted through proxies

**Root cause**

Entity lookup mixins can substitute the morph proxy for the player. Vanilla and
Identity checks then see a non-Player target and miss creative/spectator state.

**Fix**

`TargetPredicateMixin` resolves `EntityAccessor.getIdentityOwner()` and rejects
the target when the owner is spectator or has `instabuild`.

**File**

- `mixin/TargetPredicateMixin.java`

### Q. Camel and Sniffer secondary cooldowns

**Fix**

Server and client now use one resolver in `ModPackets`:

- Camel: 60 ticks
- Sniffer: 600 ticks
- Elder Guardian and Shulker retain their configured cooldown fields
- Other abilities use datapack/default cooldowns

**Files**

- `ModPackets.java`
- `Identity2Client.java`

### R. Excess max health after dying while morphed

**Root cause**

Health scaling calculated its additive modifier from the attribute's base value.
Other legitimate max-health modifiers therefore remained on top of the configured
morph cap. For example, a capped 40-health morph plus an external 8-health
modifier rendered as 48 health, or 24 hearts. The morph-attribute cleanup also
skipped max health, movement speed, and flying speed, allowing Identity-owned
modifiers from older builds to survive longer than intended.

**1.21.1 changes**

- Health scaling removes its previous modifier, reads the effective unscaled max
  health, and solves the new `ADD_VALUE` amount after accounting for
  `ADD_MULTIPLIED_BASE` and `ADD_MULTIPLIED_TOTAL` modifiers. The final effective
  value now respects `maxHealth` instead of stacking external modifiers above it.
- `clearMorphAttributes` removes Identity-owned morph modifiers from every
  attribute. It still leaves all modifiers owned by Minecraft or other mods
  untouched.
- The replacement `ServerPlayer` created during respawn is cleaned before the
  death morph rule restores or clears the morph. This also repairs stale
  Identity modifiers carried by a world previously run with an older jar.

**Files**

- `identity/IdentityProgression.java`
- `mixin/PlayerManagerMixin.java`

**1.20.1 port**

Port the effective-value calculation and all-attribute Identity modifier cleanup.
Minecraft 1.20.1 identifies attribute modifiers by UUID, so retain that branch's
UUID constructor and `removeModifier(UUID)` calls rather than copying the 1.21.1
`ResourceLocation` signatures. Calculate from `AttributeInstance.getValue()`,
not `getBaseValue()`, and preserve modifiers whose IDs are not owned by Identity.

### S. Armadillo ability and locomotion animation

**Root cause**

The Armadillo prebuilt ability and its proxy shell-state application were omitted
from `1.21.1-v2`, so it fell through to the generic mob ability. Its player-driven
walk animation also ran twice as fast as requested.

**1.21.1 changes**

- Added an explicit `minecraft:armadillo` ability definition using the Armadillo
  Scute icon.
- Primary ability toggles a synced `identity2.armadillo_shell` boolean.
- `EntityMixin` applies that state through typed `Armadillo.rollUp()` and
  `Armadillo.rollOut()` calls on both server and client proxy ticks.
- `IdentityRenderStateHelper` scales Armadillo walk-animation position and speed
  to `0.5`. The roll-up state animation remains at vanilla timing so it stays
  synchronized with Armadillo's internal state transition.
- No method-name reflection is used.

**Files**

- `PredefIdentityAbilities.java`
- `mixin/EntityMixin.java`
- `client/IdentityRenderStateHelper.java`
- `data/minecraft/identity2/identity_ability/armadillo.json`

**1.20.1 port**

Do not port this section. Armadillos were introduced after Minecraft 1.20.1.

### T. Server-owned variant references and bounded packet sync

**Root cause**

The old protocol put serialized variant NBT directly in morph requests, active
morph state, and unlock entries. Unlock tokens also Base64-encoded SNBT, adding
roughly one third to their size. Minecraft 1.21.1 limits a serverbound custom
payload and `STRING_UTF8` field to 32,767 bytes. The unlock packet splitter only
split between identity entries, so one identity with one large modded variant
could still be emitted as an oversized packet. The existing size check happened
after packet decoding on the server and therefore could not prevent a client
disconnect.

A second packet bug was loader/environment-specific: dirty proxy
`SynchedEntityData` was broadcast by `mixin/client/EntityTrackerEntryMixin`.
That mixin runs on an integrated client but is absent from a dedicated server,
so remote proxy animation/state metadata could stop updating there.

**1.21.1 changes**

- `IdentityVariantRegistry` normalizes variant NBT and derives a stable UUID from
  the entity type plus a canonical, recursively key-sorted representation.
- The server caches definitions per player. A C2S morph request now contains
  only the entity type and variant UUID. An empty UUID means the default
  variant.
- The server resolves a request only from its own cache, the player's persistent
  unlock data, the current morph, or server-side typed variant discovery.
  Unknown UUIDs are rejected and the unlock snapshot is resent. The client
  cannot submit arbitrary NBT through the morph request.
- Unlock sync entries now contain UUID strings instead of `CompoundTag` values.
  Large lists are split within an identity as well as between identities.
- Variant definitions use `IdentityVariantDefinitionS2CPacketPayload`. SNBT is
  split into 20,000-byte fragments, with a 4 MiB per-definition memory guard.
  Each packet remains safely below the 32,767-byte boundary.
- The client reassembles fragments, recalculates the stable UUID, and caches the
  definition only when it matches. Morph-state packets wait in the existing
  pending queue until every referenced definition is available.
- Active and previous morph state use `identity2.identity_variant_ref` and
  `identity2.previous_identity_variant_ref` on the wire. Persistent player data
  still stores real normalized variant NBT, so no world-save migration is
  required.
- Login and tracking dumps skip raw morph variant strings and send a definition
  snapshot followed by references. Player-skin variants use the same registry.
- Dirty proxy metadata broadcasting moved into the common server
  `EntityTrackerEntryMixin`; the client-only tracker mixin was removed from the
  client mixin configuration.
- No baby or variant path reflects mapped method names. Typed APIs, stable NBT,
  and registered `IdentityVariantAdapter` implementations remain required.

**Files**

- `identity/IdentityVariantRegistry.java` (new)
- `packets/IdentityVariantDefinitionS2CPacketPayload.java` (new)
- `packets/IdentityMorphRequestC2SPacketPayload.java`
- `packets/IdentityUnlockSyncEntry.java`
- `Identity2Client.java`
- `ModPackets.java`
- `identity/IdentityProgression.java`
- `api/IdentityApi.java`
- `client/screen/IdentitySelectionScreen.java`
- `client/screen/IdentityVariantSelectionScreen.java`
- `mixin/PlayerManagerMixin.java`
- `mixin/EntityTrackerEntryMixin.java`
- `identity2.client.mixins.json`

**1.20.1 port**

Keep the protocol and validation rules, but rewrite packet plumbing for the
1.20.1 loader APIs. Fabric 1.20.1 uses its `PacketByteBuf` networking callbacks;
Forge 1.20.1 should register equivalent messages on the branch's `SimpleChannel`
and encode/decode with `FriendlyByteBuf`. Do not copy 1.21.1
`CustomPacketPayload` or `StreamCodec` classes.

Use these port invariants:

1. Keep the saved unlock-token and selected-variant NBT formats unchanged.
2. Compute UUIDs from normalized, canonical NBT on both sides; include the
   entity type in the hash material.
3. Send only `identityId + variantId` in a morph request and reject references
   the server cannot resolve from authoritative data.
4. Fragment definition bytes at 20,000 bytes or less and enforce a bounded total
   reassembly size before allocation/copying.
5. Send definition fragments before unlock or active-state packets that use the
   reference. Queue the state packet if the definition is not ready yet.
6. Split large UUID lists inside one identity entry. Do not assume an outer list
   splitter can make an individual entry safe.
7. Exclude selected/previous raw variant strings from generic login/tracking
   custom-data dumps.
8. Put both initial proxy metadata and dirty proxy metadata sync in a common
   server mixin. Verify the mapped 1.20.1 `ServerEntity` pairing and dirty-data
   method descriptors with bytecode before choosing injection targets.
9. Use direct 1.20.1 APIs or accessor mixins. Never locate baby/variant or
   entity-NBT methods by development-name reflection.

## Verified 1.21.1 Mixin Targets

The following were checked with `javap -p -s` against the project's mapped
Minecraft 1.21.1 merged jar.

| Mixin | Target method | Descriptor | Injection used | Verification note |
|---|---|---|---|---|
| `AbstractFishMixin` | `AbstractFish.aiStep` | `()V` | redirect inherited `LivingEntity.makeSound(SoundEvent)` invocation | Bytecode contains the invocation at the flop branch before `WaterAnimal.aiStep` |
| `LivingEntityMixin` | `LivingEntity.aiStep` | `()V` | `TAIL` | Movement assist and ambient scheduler run after vanilla travel/tick work |
| `PlayerEntityMixin` | `Player.getHurtSound` | `(DamageSource)SoundEvent` | cancellable `HEAD` | Player declares the override that bypassed the LivingEntity replacement |
| `EntityMixin` | `Entity.positionRider` | `(Entity, Entity.MoveFunction)V` | `TAIL` | Protected two-argument positioning method exists in 1.21.1 |
| `TargetPredicateMixin` | `TargetingConditions.test` | `(LivingEntity, LivingEntity)Z` | cancellable `HEAD` | Exact two-entity predicate exists |
| `ScreenEffectRendererMixin` | `ScreenEffectRenderer.getViewBlockingState` | `(Player)BlockState` | cancellable `HEAD` | Private static vanilla path used by Fabric |
| `ScreenEffectRendererMixin` | `ScreenEffectRenderer.renderScreenEffect` | `(Minecraft, PoseStack)V` | `HEAD` | Public static render entry exists |
| `ScreenEffectRendererInvoker` | `ScreenEffectRenderer.renderTex` | `(TextureAtlasSprite, PoseStack)V` | invoker | Private static texture renderer exists |
| `ClientPlayerInteractionManagerMixin` | `MultiPlayerGameMode.attack` | `(Player, Entity)V` | cancellable `HEAD` | Override attack remains on left click only |
| `ClientPlayerInteractionManagerMixin` | `MultiPlayerGameMode.interact` | `(Player, Entity, InteractionHand)InteractionResult` | cancellable `HEAD` | Kept only for villager-morph trading; Shulker interception removed |
| `PlayerManagerMixin` | `PlayerList.placeNewPlayer` | `(Connection, ServerPlayer, CommonListenerCookie)V` | `TAIL` | Config snapshot is sent after join setup |
| `EntityTrackerEntryMixin` | `ServerEntity.addPairing` | `(ServerPlayer)V` | `TAIL` | Sends reference-based morph snapshot and initial proxy metadata |
| `EntityTrackerEntryMixin` | `ServerEntity.sendDirtyEntityData` | `()V` | `HEAD` | Broadcasts dirty proxy metadata on dedicated and integrated servers |

NeoForge's patched block-overlay path is intentionally handled by its event,
not by pretending the Fabric mixin is reached.

For 1.20.1, repeat this verification against the 1.20.1 mapped jar before every
mixin edit. In particular, do not copy the 1.21.1 fish redirect target.

## Automated Verification

Commands run on the 1.21.1 branch:

```text
gradlew.bat :common:test
gradlew.bat :fabric:build :neoforge:build
```

Results:

- Seven regression tests pass: four baby/variant discovery tests, the packaged
  Armadillo-ability test, and two stable-reference normalization tests.
- Fabric remapped build passes.
- NeoForge remapped build passes.
- Both release jars contain singular 1.21 entity-type tags,
  `IdentityTropicalFishVariants`, and `IdentityClientConfigS2CPacketPayload`.
- Removed `MobEquipmentMixin`, `IdentityEquipmentHelper`, and
  `UntamedWildsCompat` classes are absent from both release jars.
- Variant/baby discovery, application, and packet code contains no reflected
  development method names. `IdentityProgression` still has unrelated
  advancement/attribute compatibility reflection, and
  `ApotheosisAttributeCompat` still reflects its optional API; neither is used
  to discover or apply babies or variants.

## Manual Test Checklist

Run each applicable check on both Fabric and NeoForge unless marked otherwise.

1. Creeper secondary: hold/use the secondary ability and confirm swelling is
   smooth and matches on client/server. NeoForge is the regression target.
2. Flight tags: morph into Bat and Bee; confirm takeoff/landing and slow falling.
   Crouching should accelerate only while already moving downward.
3. Silverfish burrow: confirm first-person block overlay is suppressed on both
   loaders, the player does not suffocate, and a snow layer above does not expose
   caves. NeoForge must exercise its event path.
4. Transition: morph several times and confirm animation lasts the configured
   ticks. Repeat with Squid and Glow Squid while swimming.
5. Fish on land: stand still for 30 seconds with no repeated flop sound, then
   move and confirm a throttled sound is heard.
6. Riding: ride a boat on land/water and a Horse using Chicken, Wolf,
   Silverfish, and Endermite morphs. Confirm no ground clipping, drowning, or
   suffocation.
7. Water hurt sounds: damage Blaze and Enderman morphs with water/rain and
   confirm only the morph hurt sound plays when `useIdentitySounds=true`.
8. Ability config: set `enableMorphAbilities=false`; primary, secondary, and
   override attack must stop, normal attacks/items must work, and passive traits
   must continue. Re-enable live without reconnecting and retest.
9. Ambient sounds: listen to Zombie and Cat morphs for several minutes. Confirm
   vanilla-like spacing and no double playback. Toggle sound configs live.
10. Snow layers: walk Silverfish and Endermite under partial snow in first
    person. The snow overlay should prevent cave x-ray without affecting normal
    third-person rendering.
11. Creative targeting: provoke hostile/neutral mobs while morphed in creative;
    they must not acquire or retain the proxy/player as a target.
12. Cooldowns: verify Camel secondary is 60 ticks and Sniffer secondary is 600
    ticks on both HUD and server behavior.
13. Tropical fish: compare command suggestions and G-menu variants; morph into
    several entries and confirm color/pattern match.
14. Babies: test Chicken Baby, Panda Baby, Cat babies, Horse babies, Zombie Baby,
    Frog, and Wandering Trader. Valid babies must work through command and menu;
    Frog Baby and Wandering Trader Baby must not appear.
15. Obfuscated production jar: launch the remapped release jar, not a dev run,
    and repeat baby/variant tests. This specifically guards against mapped-name
    reflection regressions.
16. Keep inventory: enable the gamerule, equip armor/main hand/offhand, morph,
    die, and confirm every stack and count remains. Repeat with
    `identitiesEquipItems` and `identitiesEquipArmor` both true and false.
17. Normal inventory loss: disable keep inventory, die while morphed, and confirm
    vanilla drops occur once with no duplicate proxy drops.
18. Items as morphs: place blocks, place/pick up fluids, eat, drink, use tools,
    and interact with entities. Repeat while Shulker shell is open.
19. Shulker: open the shell, left-click air and an entity to fire under the
    intended cooldown, and verify right click always performs the held item's
    normal action.
20. Strider: submerge in a deep lava column, confirm upward floating, surface
    grounding, and movement out onto a solid edge.
21. Warden: confirm primary sonic boom still works and secondary does nothing
    without applying a cooldown. This is the intentional result.
22. Respawn health: with `scalingHealth=true` and `maxHealth=40`, add another
    mod's additive or multiplicative max-health modifier, die while morphed, and
    confirm the retained morph never exceeds 20 hearts. Repeat with every death
    morph rule and confirm unmorphed health keeps only the non-Identity modifiers.
23. Armadillo: press primary to roll up, press it again to roll out, and verify
    the state matches for the local player and a remote observer. Walk while
    unrolled and confirm locomotion plays at half the previous speed without the
    roll-up animation snapping midway.
24. Variant packet protocol: test a modded variant whose serialized NBT exceeds
    32,767 bytes. Confirm definition fragments stay connected, the menu and
    morph render correctly, and an invented/unknown UUID is rejected by the
    server. Repeat on dedicated Fabric and NeoForge servers with a remote
    observer to verify dirty proxy metadata updates.

## Port Order for 1.20.1

1. Port inventory authority and direct keep-inventory access first because it
   prevents item loss.
2. Port respawn attribute cleanup and effective-value health scaling using the
   1.20.1 UUID modifier API.
3. Port shared typed variant discovery/application and command/menu unification.
4. Port the server-owned variant-reference registry, bounded definition
   fragments, unlock-reference packets, and common server tracker sync.
5. Add the 1.20.1 config snapshot packet and ability gating.
6. Remove Shulker right-click interception.
7. Port common gameplay fixes: Strider, ambient scheduler, riding offset, hurt
   sound, targeting, cooldowns, and tropical fish.
8. Port loader-specific hooks only after checking Forge 1.20.1 APIs.
9. Keep the 1.20.1 plural entity tag directory.
10. Verify every 1.20.1 mixin descriptor from its mapped jar, then build the
   obfuscated/remapped Forge and Fabric artifacts and run the checklist above.
