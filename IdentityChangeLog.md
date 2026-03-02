# Identity2 Public Changelog

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
