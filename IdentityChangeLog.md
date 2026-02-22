# Identity Rework Update

### What This Rework Includes
- Fully reworked morph changing with a dynamic transition animation instead of instant form swaps.
- Morph-inspired acquisition visuals when you unlock an identity from kills.
- New projectile behavior for offensive abilities so attacks are visible in flight:
- Ghast ability now launches a traveling fireball from the player.
- Ender Dragon ability now launches a real dragon fireball instead of spawning breath instantly at target.
- Improved multiplayer sync for morph transition state so other players see smoother transformations.
- Better transition cleanup and fallback handling when morph setup fails.

### New Config Options
- `morphTransitionTicks` to control transition duration.
- `enableMorphTransitionParticles` to toggle transition particles while keeping model transition active.
- `enableMorphAcquisitionTendrils` to toggle acquisition tendril visuals.
- `morphAcquisitionAnimationTicks` to control acquisition animation duration.

---
