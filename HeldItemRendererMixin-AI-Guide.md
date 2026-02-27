# HeldItemRendererMixin AI Guide (1.20.5 context)

## Scope
This document is for future AI/code agents working on:

- `common/src/main/java/net/Gabou/identity2/mixin/client/HeldItemRendererMixin.java`
- First-person arm/hand rendering while morphed.

## Current known issue

- On Fabric: morphed arm/hand can be missing in first person.
- On NeoForge: vanilla player arm can show instead of morph arm.

User requirement:

- When morphed, always show the entity arm/hand, never the player arm.

## Important status

- A previous attempt to add extra `@Redirect` targets (Neo overloads) and reflection fallback was reverted.
- Reason: injector target resolution errors (`Cannot resolve any target instructions`, `Could not resolve @At target`, `There are no possible signatures for this injector`).
- `HeldItemRendererMixin.java` is currently back to its prior state.

## Why this is tricky

Method call sites differ by version/loader/mappings patch set:

- Some environments call:
  - `PlayerRenderer.renderRightHand(PoseStack, MultiBufferSource, int, ResourceLocation, boolean)`
  - `PlayerRenderer.renderLeftHand(PoseStack, MultiBufferSource, int, ResourceLocation, boolean)`
- Others call overloads with an extra player argument.
- In newer branches (ex: 1.21.11), `PlayerRenderer`/`MultiBufferSource` names can change (ex: `AvatarRenderer`, `SubmitNodeCollector`), so hardcoded descriptors break.

## Rules for future fixes

1. Do not add new redirect descriptors blindly.
2. Before editing mixin targets, verify exact invoked descriptors in the runtime target jar for that branch.
3. Keep `require = 0` on fragile redirects.
4. If adding a fallback render path, ensure it does not re-enable vanilla arm rendering during morph.
5. Keep changes loader-safe (Fabric + NeoForge) and version-aware.

## Recommended workflow

1. Inspect current bytecode call sites:
   - `ItemInHandRenderer.renderPlayerArm(...)`
   - `ItemInHandRenderer.renderMapHand(...)`
2. Confirm the exact invoke targets used in this branch.
3. Align `@Redirect` signatures only to confirmed targets.
4. Test both loaders:
   - Morphed arm visible in normal item render.
   - Morphed arm visible with map.
   - Vanilla arm visible only when not morphed.
5. Re-test spider/odd-geometry entities to ensure arm placement is still acceptable.

## Fast sanity checklist

- No mixin apply crash at startup.
- No injector warnings for `HeldItemRendererMixin`.
- Fabric: no missing morph arm.
- NeoForge: no vanilla player arm while morphed.
- Non-morphed player still renders vanilla arms correctly.

## Related files to inspect if issue persists

- `common/src/main/java/net/Gabou/identity2/mixin/client/PlayerEntityRendererMixin.java`
- `common/src/main/java/net/Gabou/identity2/mixin/client/EntityRenderDispatcherMixin.java`
- `common/src/main/java/net/Gabou/identity2/client/transition/MorphTransitionHelper.java`
- `common/src/main/resources/identity2.client.mixins.json`

## Note for 1.21.8 / 1.21.9 / 1.21.11 ports

- Do not assume method/class names or descriptors match 1.20.5.
- Re-derive targets from the branch’s own mapped runtime jars before porting this mixin logic.
