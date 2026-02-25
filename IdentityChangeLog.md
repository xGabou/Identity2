# Identity2 Public Changelog

Date: 2026-02-25

## New in this update

- Variants now work much better for both vanilla and modded mobs.
- The game now tries to discover variant types automatically instead of relying only on hardcoded lists.
- Morph variant data is now saved and applied in a generic way, so special mob forms are more likely to stay correct.
- Variant choices are deduplicated, so you should not see repeated entries for the same form.

## What this means for players

- More mobs should show their different forms in the morph UI.
- Modded mobs with multiple forms are now far more likely to work without custom code.
- Existing vanilla variants (like sheep colors and other known forms) still work.

## Technical stability notes

- This update includes compatibility fallbacks for registry-based variant systems.
- Build verification passed for:
  - `:common:compileJava`
  - `:fabric:compileJava`
  - `:neoforge:compileJava`
