# Identity2 Public Changelog

---
## 2026-04-29

### Ability and Behavior Fixes

- Fixed and added several identity abilities and behavior hooks across supported branches.
- Restored Ender Dragon animation handling and destructive behavior.
- Fixed Iron Golem health synchronization.
- Fixed Ravager, Iron Golem, and Hoglin attack animations.
- Unlocked baby variants where supported.
- Restored sunlight burning damage behavior.
- Added Shulker teleportation and attack behavior.
- Added Guardian attack behavior.
- Added several additional ability fixes and behavior improvements.

### Notes

- For the full changelog, check previous Discord posts.

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

**Is this 1.20.5?** Yes.  
**Is it stable?** It should be, but it is still in beta.

This build is currently available only for **1.20.5**.  
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