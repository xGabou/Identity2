# Identity2 Public Changelog

---

## 2026-03-16

### Highlights

- Restored animation playback for a wide range of morphs instead of leaving them visually static.
- Improved Forge stability so the mod starts correctly and no longer disconnects on certain animated morphs.

### Fixes

- Fixed missing morph animations for many identities, including Ravager, Warden, Naturalist mobs, and many Alex's Mobs identities such as crocodile, gorilla, emu, and tasmanian_devil.
- Fixed client-side morph state syncing so attack, hurt, movement, and other visual animations update correctly.
- Fixed morphs failing to initialize tracked animation data correctly when joining a world or changing form.
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
