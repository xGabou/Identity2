# Identity2 1.20.1 Public Changelog
---
## 2.2.3

- Removed the legacy auth folder.

---
## 2.2
- Fixed keep-inventory deaths removing equipped armor and held items.
- Improved baby and mob variant discovery, commands, menus, previews, unlocks, and autocomplete, including Tropical Fish variants.
- Fixed base forms such as Cow appearing locked after being unlocked and filtered transient entity data from variant saves.
- Removed variant reflection that depended on unobfuscated method names, improving production-client compatibility.
- Added live server-to-client config synchronization and an option to disable morph abilities.
- Fixed Shulker morphs blocking normal item use and block placement.
- Improved Strider movement and surfacing in lava.
- Fixed Creeper swelling visuals on NeoForge.
- Corrected flying and slow-falling identity detection and movement behavior.
- Improved Silverfish burrow vision and prevented small morphs from seeing caves through snow layers or nearby blocks.
- Fixed short-lived morph animations and restored continuous Squid and Glow Squid movement animation.
- Reduced repeated fish flop and morph ambient sounds.
- Adjusted riding height so small morphs do not sink into mounts or boats.
- Removed duplicate player drowning sounds when Blaze or Enderman morphs are hurt by water.
- Prevented mobs from targeting morphed Creative players.
- Increased Camel and Sniffer ability cooldowns.
- Added the missing Armadillo ability and reduced its movement animation speed by 50%.
- Fixed morph health scaling, stale attribute modifiers, excess hearts after respawning, and silent health loss while morphed.
- Reworked variant networking around server-owned references, bounded packet fragments, and dedicated-server tracking to prevent oversized packets and invalid client-submitted variant data.
- Improved unlock and variant synchronization across morphing, reconnecting, death, and respawn.

Warden burrowing remains intentionally unsupported.
---

## 1.3.4


### Gameplay Fixes (Forge 1.20.1)

- Fixed `killToUnlock` enforcement so kill-based unlock progression now blocks locked identities consistently in the morph menu, morph packet handling, and command suggestions/validation.
- Fixed riding/crawling duplicate morph visuals by suppressing previous-identity transition rendering while the host is mounted or in the swimming/crawling pose.
- Fixed false corner/side suffocation for morphed players by requiring real collision-box overlap with a suffocating block shape before treating the morph as inside a wall.
- Removed broken generic player sync for raw `Attributes.MOVEMENT_SPEED`, which was making many morphs dramatically too fast because mob movement-speed values are not directly compatible with player movement.
- Kept the newer generic attribute sync for other supported attributes, but movement speed now stays on the stable player path until a dedicated morph-speed mapping is introduced.

---

## 1.3.1

### Gameplay Fixes (Forge 1.20.1)

- Fixed `killToUnlock` enforcement so kill-based unlock progression now blocks locked identities consistently in the morph menu, morph packet handling, and command suggestions/validation.
- Fixed riding/crawling duplicate morph visuals by suppressing previous-identity transition rendering while the host is mounted or in the swimming/crawling pose.
- Fixed false corner/side suffocation for morphed players by requiring real collision-box overlap with a suffocating block shape before treating the morph as inside a wall.
- Removed broken generic player sync for raw `Attributes.MOVEMENT_SPEED`, which was making many morphs dramatically too fast because mob movement-speed values are not directly compatible with player movement.
- Kept the newer generic attribute sync for other supported attributes, but movement speed now stays on the stable player path until a dedicated morph-speed mapping is introduced.

### Compatibility

- Replaced Identity2 client render interception in `EntityRenderDispatcher` from `@Redirect` to `@WrapOperation` (MixinExtras) to reduce hard conflicts with mods patching the same `EntityRenderer.render(...)` invoke.
- Morph render path now safely falls back to the original render operation when no valid resolved identity renderer is available.
- Cleaned up `EntityRenderDispatcherMixin` shadow usage as part of the render interception refactor.

### Morph Stat / Combat Sync (Forge 1.20.1)

- Reworked player morph attribute syncing to copy supported attributes directly from the current identity instance (including modded attributes when present on both sides).
- Added centralized attribute sync/cleanup flow so morph stats are reapplied reliably after morph refreshes and cleared cleanly when returning to player form.
- Fixed flying morph wall/ceiling false-positive damage by ignoring in-wall suffocation checks for fly-capable identities.
- Disabled fall damage for fly-capable player morphs to prevent unintended damage while airborne.
- Kept unarmed attack path routed through identity attack logic, with held-item attacks still using normal player attack flow.

### Notes

- Tide `2.0.3` (`bTaXAgQ8`) still uses a hard `@Redirect` on the same invoke, so final compatibility can still depend on mixin application order.
- If needed, the next fallback is moving Identity2 interception earlier to `EntityRenderDispatcher.render(...)` `@Inject(HEAD, cancellable = true)` with guarded re-dispatch.

## Side note
Don't forget to check out my new channel https://discord.com/channels/1363919699745837118/1483554094495961100 to see what the status of the mod is.
Also, I'll be hosting a server with a random modpack that yall choose. Get the @MinecraftServer role in the discord to vote for the modpack and get updates about the server. The server will be up in a few weeks, so stay tuned!
---

## 2026-03-18

### Highlights

- Custom Attribute system re-ported from `1.21.11`.
- Apotheosis compatibility added (**alpha**).
- Default config changed to disable the progression system for more casual players.

### Fixes / Adjustments

- Fixed issues mentioned by Alex.
- Rewired attack behavior so identity attack is used when no item is selected.
- This attack rewiring still needs more tuning and is not fully behaving as intended yet.

---

## 2026-03-16

### Highlights

- Restored animation playback for a wide range of morphs instead of leaving them visually static.
- Improved Forge stability so the mod starts correctly and no longer disconnects on certain animated morphs.

### Fixes

- Fixed missing morph animations for many identities, including Ravager, Warden, Naturalist mobs, and many Alex's Mobs identities such as crocodile, gorilla, emu, and tasmanian_devil.
- Fixed client-side morph state syncing so attack, hurt, movement, and other visual animations update correctly.
- Fixed morphs failing to initialize tracked animation data correctly when joining a world or changing form.
- Fixed a Forge startup crash in published builds caused by the production mixin/refmap configuration not being packaged correctly.
- Fixed a Forge disconnect caused by animated morphs receiving Citadel animation packets on the player entity instead of the morph identity.
- Fixed Forge startup/runtime dependency issues affecting MidnightLib, GeckoLib, and related Forge development/runtime compatibility.

### Added / Improved Behavior

- Illager morphs can now ride Ravagers.
- Ravager morphs now have a roar-based rage ability for nearby knockback behavior, closer to the Ravager shield-stun rage burst.

---

## 2026-03-03

### Known Issues

- Player skin sync is currently bugged: you will see your own correct skin locally, but other players on the server may see the default Steve skin instead.
- A fix for this skin sync issue is planned in an upcoming update.

### Added Entity Abilities

- Naturalist: bear.
- Alex's Mobs: anaconda, bald_eagle, bone_serpent, cockroach, crimson_mosquito, crocodile, crow, dropbear, elephant, emu, enderiophage, fly, giant_squid, gorilla, grizzly_bear, guster, hummingbird, kangaroo, komodo_dragon, mimicube, moose, orca, raccoon, rattlesnake, roadrunner, skunk, snow_leopard, soul_vulture, spectre, sunbird, tarantula_hawk, tasmanian_devil, tiger, void_worm, warped_mosco.

---

## 2026-03-02

### Highlights

- New option: `unlockAllVariantsOnFirstUnlock` (unlocking an identity can unlock all its variants).
- Spider and Cave Spider identities now move normally through cobwebs.
- Variant unlocks are now more consistent and reliable.
- Better automatic variant detection for more mobs.

### Fixes

- Fixed an issue where returning to human could leave you with very low hearts after tiny morphs (like salmon).
- Improved overall morph stability in gameplay.

---

## 2026-02-26

### Respawn Progression Sync Fix

#### Fixes

- Added a proper custom data merge from the old player entity to the new respawned entity.
- Rebuilt the unlock cache on respawn and forced a fresh packet sync.
- Synced both `identity2.unlocked_identities_cache` and `identity2.unlocked_identity_variants_cache` after respawn so the client UI stays aligned with server progression data.

#### Why this was needed

- Unlocks could appear to reset after death or respawn even though the data still existed server side.
- The morph UI reads cached values, so stale or missing cache sync could show an empty unlock list.

---

## 2026-02-25

### Important

**Is this 1.20.1?** Yes.  
**Is it stable?** It should be, but it is still in beta.

This build is currently available only for **1.20.1**.  
Other versions will be re released shortly.

---

### New in this update

- Variant support has been heavily improved for both vanilla and modded mobs.
- The game now attempts to automatically discover variant types instead of relying only on hardcoded lists.
- Morph variant data is now saved and applied generically, improving compatibility with special mob forms.
- Variant entries are deduplicated, preventing repeated forms in the UI.
- Illusioner abilities are now fully integrated into the morph system.
- Removed an accidental duplicate mixin.

---

### Fixes

- Fixed Ender Dragon dying when morphing into it.
- Fixed missing config file generation.
- Fixed entities not being properly immune in Creative mode.
- Fixed entities slowly descending when not holding the jump key.
- Began addressing the `loseAllMorphsOnDeath` issue. This fix is still in progress and remains in beta.

---

### What this means for players

- More mobs should correctly display their different forms in the morph UI.
- Modded mobs with multiple forms are now far more likely to work without custom integration.
- Existing vanilla variants such as sheep colors and other known forms continue to function properly.
