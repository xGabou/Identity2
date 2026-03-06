# Identity2 Addon Dev Guide

This document covers the public addon-facing integration surface for Identity2 on Minecraft `1.21.11`.

It is aimed at code addons that want to:

- add code-backed morph abilities
- provide exact variant support for modded entities
- keep morph animation state in sync
- read the current morph and current variant
- update synced morph-side custom data

## Stability

Prefer the public API in [`net.Gabou.identity2.api`](./common/src/main/java/net/Gabou/identity2/api) over reaching into mixins or internal helpers.

The intended entrypoint is:

- `net.Gabou.identity2.api.IdentityApi`

Supporting public interfaces:

- `net.Gabou.identity2.api.ability.BuiltinIdentityAbility`
- `net.Gabou.identity2.api.variant.IdentityVariantAdapter`
- `net.Gabou.identity2.api.morph.IdentityMorphTickHandler`

## Registration Timing

Register addons during your normal mod initialization, before gameplay starts.

Typical examples:

- register builtin abilities during common setup / mod init
- register variant adapters during common setup / mod init
- register morph tick handlers during common setup / mod init

## Public API Overview

Main entrypoints on `IdentityApi`:

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

- `registerVariantAdapter(...)` stores one adapter per `EntityType`. Later registrations replace earlier ones.
- `registerMorphTickHandler(...)` accumulates handlers per `EntityType` in registration order.
- `getCurrentMorphId(...)` returns `null` when the entity is not morphed or is in base player form.
- `getCurrentMorphVariant(...)` returns the stored selected variant NBT, not a fresh live diff of the current entity.

## Code-Backed Abilities

### 1. Register a builtin ability in code

```java
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.api.ability.BuiltinIdentityAbility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

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
        // Called while ability cooldown/use visuals are ticking
    }

    @Override
    public void passiveTick(Entity player, boolean used) {
        // Called from the passive ability path
    }

    @Override
    public boolean overrideAttack(Entity player) {
        // Return true if you handled the attack path yourself
        return false;
    }
});
```

### 2. Bind it from data

Identity2 still supports data-driven ability definitions through the `identity2:identity_ability` registry.

Relevant JSON fields:

- `icon`
- `command`
- `cooldown`
- `use_duration`
- `predef`
- `override_attack`

Example:

```json
{
  "icon": "minecraft:fire_charge",
  "cooldown": 40,
  "use_duration": 20,
  "predef": "yourmod:my_ability"
}
```

Important:

- `predef` is the builtin ability id that resolves to your registered `BuiltinIdentityAbility`.
- The internal record field is named `bultinability` in code. That typo is internal. The JSON key you write is still `predef`.
- If you register with `registerBuiltinAbility(MY_ENTITY_TYPE, ...)`, the natural builtin id is that entity type id.
- If you want multiple code abilities not tied one-to-one to an entity id, register with an explicit `ResourceLocation`.

### 3. Ability resolution behavior

Identity2 resolves builtin abilities in this order:

- exact id from `predef`
- same path under `minecraft`
- same path under `identity2`
- generic fallback ability for non-player, non-`MISC` entities when no builtin is found

That means a custom registered builtin ability will override the generic fallback.

## Variant Adapters

Variant adapters are the correct way to support modded entity variants, especially when:

- variant NBT is not a simple vanilla-style `Variant` int
- the UI needs a curated set of variant entries
- the morph entity needs custom apply logic
- unlock extraction needs stable compact variant tokens

### Adapter Contract

`IdentityVariantAdapter` has three hooks:

### `extractVariantData(LivingEntity entity)`

Server-side extraction hook used when Identity2 captures the killed entity's variant for unlock/progression purposes.

Use it to write a stable, compact `CompoundTag` that uniquely identifies the variant.

Good examples:

- `Color = "blue"`
- `Skin = "verdant"`
- `StarbuncleColor = "cyan"`
- `StarbuncleAccessory = "flower_crown"`

Avoid:

- transient health or position
- cooldowns
- random runtime-only state
- data that should not affect identity selection

### `applyVariantData(Entity entity, CompoundTag variantNbt)`

Called when a morph entity is created and its stored variant NBT is being applied.

Use it to read the same keys you wrote in `extractVariantData(...)` and push them back into the morph entity.

### `discoverVariants(EntityType<?> type, Level level)`

Used by the identity selection UI.

Return exact `IdentityVariant` entries when the default heuristics are not enough.

Each `IdentityVariant` contains:

- `entityTypeId`
- `displayName`
- `variantNbt`

### Discovery Precedence

Identity2 now adds adapter-provided variants first, then continues with its built-in heuristic discovery.

That means:

- use an adapter when you want exact Ars variants in the menu
- heuristics can still contribute extra variants if they find valid additional states

### Example: Ars-style adapter

```java
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.api.variant.IdentityVariantAdapter;
import net.Gabou.identity2.identity.IdentityVariant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

IdentityApi.registerVariantAdapter(MY_STARBUNCLE_TYPE, new IdentityVariantAdapter() {
    @Override
    public CompoundTag extractVariantData(LivingEntity entity) {
        CompoundTag variant = new CompoundTag();
        variant.putString("StarbuncleColor", readStarbuncleColor(entity));
        variant.putString("StarbuncleAccessory", readStarbuncleAccessory(entity));
        return variant;
    }

    @Override
    public void applyVariantData(Entity entity, CompoundTag variantNbt) {
        applyStarbuncleColor(entity, variantNbt.getString("StarbuncleColor"));
        applyStarbuncleAccessory(entity, variantNbt.getString("StarbuncleAccessory"));
    }

    @Override
    public List<IdentityVariant> discoverVariants(EntityType<?> type, Level level) {
        return List.of(
            variant(type, "Starbuncle Blue", "blue", "none"),
            variant(type, "Starbuncle Green", "green", "none"),
            variant(type, "Starbuncle Cyan Flower Crown", "cyan", "flower_crown")
        );
    }

    private IdentityVariant variant(EntityType<?> type, String label, String color, String accessory) {
        CompoundTag tag = new CompoundTag();
        tag.putString("StarbuncleColor", color);
        tag.putString("StarbuncleAccessory", accessory);
        return new IdentityVariant(EntityType.getKey(type), label, tag);
    }
});
```

## Morph Tick Handlers

Morph tick handlers are for keeping the live morph entity in sync with the host player each tick.

Typical uses:

- mirror `isFallFlying`
- mirror a modded tracked flag used by animations
- mirror stance/state that the morph entity renderer expects

### Important runtime detail

Morph tick handlers currently run on the server-side morph tick path, before the morph entity itself ticks.

That is the right place for server-authoritative animation state, because dirty entity data is then broadcast normally.

### Example

```java
import net.Gabou.identity2.api.IdentityApi;

IdentityApi.registerMorphTickHandler(MY_WILDEN_STALKER_TYPE, (host, currentMorph) -> {
    currentMorph.setSharedFlag(7, host.isFallFlying());
});
```

If the visual state is not covered by normal tracked entity data, combine the tick handler with the sync helpers described below.

## Reading the Current Morph

### `IdentityApi.getCurrentMorph(Entity entity)`

Returns the live morph entity instance or `null`.

Use this when you need direct access to the current morph entity's methods, data tracker, or class.

### `IdentityApi.getCurrentMorphId(Entity entity)`

Returns the selected morph `ResourceLocation` or `null`.

Use this when you only care about identity type.

### `IdentityApi.getCurrentMorphVariant(Entity entity)`

Returns the stored selected variant `CompoundTag`.

Use this when your addon needs the current variant token but does not need the live morph instance.

### `IdentityApi.isMorphed(Entity entity)`

Convenience boolean for "is currently morphed into a non-player form".

## Syncing Extra Morph State

If your addon needs extra synced flags that are not part of vanilla entity tracked data, use:

- `IdentityApi.syncBoolean(...)`
- `IdentityApi.syncDouble(...)`
- `IdentityApi.syncString(...)`

These helpers:

- write into the player's synced custom data
- broadcast to the player
- broadcast to other players in the same server level
- no-op when the value is unchanged

Example:

```java
IdentityApi.syncBoolean(serverPlayer, "yourmod.wilden_wings_open", serverPlayer.isFallFlying());
IdentityApi.syncString(serverPlayer, "yourmod.current_form_style", "verdant");
IdentityApi.syncDouble(serverPlayer, "yourmod.render_scale_bonus", 0.15D);
```

## Updating the Active Variant at Runtime

If your addon changes the currently selected morph variant after morphing, call:

```java
IdentityApi.updateCurrentMorphVariant(serverPlayer, newVariantTag);
```

This persists the selected variant and syncs it to clients.

Use this for cases like:

- villager profession updates
- runtime form switching that should stay attached to the active morph

## Relevant Morph Data Keys

Identity2 stores most morph-related state in synced custom data.

Common keys addon authors may care about:

- `identity2.identity_type`
- `identity2.identity_variant`
- `identity2.previous_identity_type`
- `identity2.previous_identity_variant`
- `identity2.transition_start_tick`
- `identity2.transition_duration_ticks`
- `identity2.morph_damage_grace_end_tick`
- `model_override`
- `width_override`
- `height_override`

Behavior notes:

- `identity2.identity_type` is the canonical selected morph type key.
- `identity2.identity_variant` stores serialized variant NBT as a string.
- `model_override` remains as a compatibility/render path fallback.
- `width_override` and `height_override` control the synced morph collision shape.

## Best Practices

- Keep variant NBT stable and minimal.
- Use your own namespace for custom synced keys, for example `yourmod.some_flag`.
- Prefer `getCurrentMorphId(...)` when only the type matters.
- Prefer `getCurrentMorph(...)` when you need the live morph entity.
- Use a variant adapter instead of relying on heuristic NBT discovery for complex modded mobs.
- Use morph tick handlers for authoritative state mirroring.
- Use synced custom data only for values that are not already covered by the morph entity's normal tracked data.

## Recommended Addon Pattern

For a complex morph-capable modded entity:

1. Register a builtin ability if the form has a custom action.
2. Register a variant adapter if the form has curated or complex variants.
3. Register a morph tick handler if the renderer expects mirrored host state.
4. Use `getCurrentMorph(...)` or `getCurrentMorphVariant(...)` when integrating with your own systems.

## Troubleshooting

### My variants still do not appear in the menu

- Ensure your adapter is registered before the menu is opened.
- Ensure `discoverVariants(...)` returns the correct `EntityType` id.
- Ensure each returned variant has stable non-empty `variantNbt`.

### The morph has the right variant token but the wrong look

- Your `extractVariantData(...)` and `applyVariantData(...)` are not symmetric.
- Write the exact keys you need during extract, then consume the same keys during apply.

### The animation still does not sync

- First try setting normal tracked entity data inside your morph tick handler.
- If the renderer depends on addon-specific state, mirror it with `syncBoolean`, `syncDouble`, or `syncString`.

### My ability JSON points to a builtin id but nothing happens

- Make sure the builtin ability was registered in code.
- Make sure the JSON `predef` id matches the registered id exactly.

