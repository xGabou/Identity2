# Identity2 1.21.1 Fixes
- Fixed server crash

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
