# Identity2 Addon Dev Guide

This document describes the public addon-facing integration surface for Identity2 on Minecraft `1.21.11`.

Use the public API in `net.Gabou.identity2.api` instead of reaching into internal mixins or helper classes.

## Public Entry Points

Main API:

- `net.Gabou.identity2.api.IdentityApi`

Supporting interfaces:

- `net.Gabou.identity2.api.ability.BuiltinIdentityAbility`
- `net.Gabou.identity2.api.variant.IdentityVariantAdapter`
- `net.Gabou.identity2.api.morph.IdentityMorphTickHandler`

## Registration

Register addon hooks during your normal common setup or mod initialization.

Typical usage:

- register builtin abilities during startup
- register variant adapters during startup
- register morph tick handlers during startup

## IdentityApi Overview

Available methods:

- `registerBuiltinAbility(ResourceLocation id, BuiltinIdentityAbility ability)`
- `registerBuiltinAbility(EntityType<?> type, BuiltinIdentityAbility ability)`
- `registerVariantAdapter(EntityType<?> type, IdentityVariantAdapter adapter)`
- `registerMorphTickHandler(EntityType<?> type, IdentityMorphTickHandler handler)`
- `getCurrentMorph(Entity entity)`
- `getCurrentMorphId(Entity entity)`
- `getCurrentMorphVariant(Entity entity)`
- `isMorphed(Entity entity)`
- `updateCurrentMorphVariant(ServerPlayer player, CompoundTag variantNbt)`
- `syncBoolean(ServerPlayer player, String key, boolean value)`
- `syncDouble(ServerPlayer player, String key, double value)`
- `syncString(ServerPlayer player, String key, String value)`

Behavior notes:

- Variant adapters are stored one per `EntityType`; later registrations replace earlier ones.
- Morph tick handlers accumulate per `EntityType` in registration order.
- `getCurrentMorphId(...)` returns `null` when the entity is not morphed or is in base player form.
- `getCurrentMorphVariant(...)` returns the stored selected variant NBT.

## Builtin Abilities

Example:

```java
IdentityApi.registerBuiltinAbility(MY_ENTITY_TYPE, new BuiltinIdentityAbility() {
    @Override
    public void execute(Entity player) {
        // Primary ability
    }

    @Override
    public void executeSecondary(Entity player) {
        // Secondary ability
    }

    @Override
    public void tick(Entity player, int cooldown) {
        // Cooldown-driven tick hook
    }

    @Override
    public void passiveTick(Entity player, boolean used) {
        // Passive tick hook
    }

    @Override
    public boolean overrideAttack(Entity player) {
        return false;
    }
});
```

Data-driven abilities still bind through `identity2:identity_ability` JSON. The `predef` field is the builtin ability id that resolves to your registered code ability.

Resolution order:

- exact id from `predef`
- same path under `minecraft`
- same path under `identity2`
- generic fallback ability for non-player, non-`MISC` entities

## Variant Adapters

Implement `IdentityVariantAdapter` to support exact extraction, application, or discovery for modded entity variants.

Example:

```java
IdentityApi.registerVariantAdapter(MY_ENTITY_TYPE, new IdentityVariantAdapter() {
    @Override
    public CompoundTag extractVariantData(LivingEntity entity) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Variant", 1);
        return tag;
    }

    @Override
    public void applyVariantData(Entity entity, CompoundTag variantNbt) {
        // Apply the variant to the live morph entity
    }
});
```

## Morph Tick Handlers

Use `IdentityMorphTickHandler` when a morph needs additional server-side ticking beyond the normal mirrored morph entity tick.

Example:

```java
IdentityApi.registerMorphTickHandler(MY_ENTITY_TYPE, (host, currentMorph) -> {
    // Keep custom animation or synced state updated here
});
```

## Synced Custom Data

Use `syncBoolean`, `syncDouble`, and `syncString` to write morph-side custom data and broadcast it to tracking players.

These methods:

- update the host entity custom data
- skip redundant writes
- send the matching packet payload to the owner and nearby tracking players

## Recommended Boundaries

- Prefer the public API over mixin accessors.
- Keep addon data keys namespaced, for example `yourmod.some_flag`.
- Treat the public API as stable; internal helper names may change between versions.
