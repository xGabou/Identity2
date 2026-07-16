# Naturalist & Alex's Mobs ability map (Minecraft 1.20.1)

## Scope and evidence

This is the implementation inventory for the versions actually declared by this branch:

| Mod | Installed artifact | Mod version | Root morphs |
| --- | --- | ---: | ---: |
| Naturalist | CurseForge file `6863943` | `5.0pre5` | 31 |
| Alex's Mobs | CurseForge file `5698791` | `1.22.9` | 90 |

The entity lists were read from each installed JAR's entity registry, not copied from a newer mod page. That is important: an online list may include mobs that do not exist in this 1.20.1 dependency. The behavioural descriptions were cross-checked against the [Naturalist 1.20.1 release page](https://www.curseforge.com/minecraft/mc-mods/naturalist/files/6863943), [Naturalist source archive](https://github.com/starfish-studios/Naturalist), [Alex's Mobs 1.20.1 listing](https://modrinth.com/mod/alexs-mobs?version=1.20.1), and the [Alex's Mobs mob index](https://alexs-mobs-unofficial.fandom.com/wiki/Mobs). Individual wiki entries are especially useful for complex mechanics such as [Mantis Shrimp](https://alexs-mobs-unofficial.fandom.com/wiki/Mantis_Shrimp), [Underminer](https://alexs-mobs-unofficial.fandom.com/wiki/Underminer), [Cachalot Whale](https://alexs-mobs-unofficial.fandom.com/wiki/Cachalot_Whale), and [Void Worm](https://alexs-mobs-unofficial.fandom.com/wiki/Void_Worm).

**What “ability” means here.** This map covers player-relevant locomotion, attacks, defences, special states, and world interaction. It deliberately does **not** turn ambient AI, breeding, drops, taming, or a mob's inventory into player powers unless that mechanic creates a meaningful morph ability. Every ability in the `Morph mapping` column is either passive or a deliberate active input; no mob is omitted merely because it has no suitable active ability.

**Status legend.** `Implemented` means the exact root entity ID has an explicit data-driven definition. The human-readable mapping below describes the intended player-facing kit; the JSON definition is the source of truth for its action, cooldown, strength, range, duration, and reusable traits. Safety-normalised approximations are called out below.

**Implementation notation.** `Passive:` runs while morphed. `Active:` is a cooldown-controlled ability. `No special active` is an intentional decision, not a missing entry. `Cosmetic/AI only` means retain the normal Identity movement/body rules only.

## Naturalist — 31 root morphs

| Identity ID | In-mod abilities / defining mechanics | Morph mapping | Status |
| --- | --- | --- | --- |
| `naturalist:alligator` | Amphibious ambush predator; powerful bite; protects nest/young. | Passive: strong swim and water breathing. Active: short water-only lunge that bites/briefly pulls a hit target. | Implemented |
| `naturalist:bass` | Small schooling fish; fully aquatic. | Passive: water breathing and fish swim. No special active. | Implemented |
| `naturalist:bear` | Omnivore; sleeps; gathers food; defends cubs with melee swipes. | Passive: heavy knockback resistance. Active: two-handed paw swipe/nearby knockback. | Implemented |
| `naturalist:bluejay` | Small flocking bird; flight and perch behaviour. | Passive: controlled bird flight. No special active. | Implemented |
| `naturalist:boar` | Defensive herd animal; melee tusk attack when provoked. | Passive: knockback resistance. Active: short tusk charge. | Implemented |
| `naturalist:butterfly` | Flight; pollinates flowers and can accelerate nearby crop growth. | Passive: controlled insect flight. Active: pollen pulse that applies a small bonemeal-style crop growth effect. | Implemented |
| `naturalist:canary` | Small flocking bird; flight and perch behaviour. | Passive: controlled bird flight. No special active. | Implemented |
| `naturalist:cardinal` | Small flocking bird; flight and perch behaviour. | Passive: controlled bird flight. No special active. | Implemented |
| `naturalist:caterpillar` | Crawls; forms a cocoon and metamorphoses into a butterfly. | Passive: low profile/crawling. Active: cocoon guard—brief resistance and immobility; no actual identity change. | Implemented |
| `naturalist:catfish` | Bottom-dwelling aquatic fish. | Passive: water breathing and fish swim. No special active. | Implemented |
| `naturalist:coral_snake` | Small venomous snake; poisonous bite. | Passive: low profile. Active: short-range venomous bite. | Implemented |
| `naturalist:deer` | Timid herd animal; fast evasive movement. | Passive: reduced fall damage. Active: evasive forward leap. | Implemented |
| `naturalist:dragonfly` | Fast insect flight; hovers above water/vegetation. | Passive: controlled insect flight. Active: rapid short air dash. | Implemented |
| `naturalist:duck` | Flies and swims; lays eggs. | Passive: bird flight, water breathing, efficient swim. No special active. | Implemented |
| `naturalist:elephant` | Large herd animal; trunk/melee knockback and defensive charge. | Passive: high knockback resistance. Active: trunk shove/cone knockback. | Implemented |
| `naturalist:firefly` | Nocturnal flight; emits light; hides in grass by day. | Passive: controlled insect flight and small personal light (client visual only unless light placement is explicitly approved). No special active. | Implemented |
| `naturalist:finch` | Small flocking bird; flight and perch behaviour. | Passive: controlled bird flight. No special active. | Implemented |
| `naturalist:giraffe` | Tall browsing herbivore; defensive long-range kick. | Passive: tall reach only if hitbox policy allows it. Active: backward/nearby kick with knockback. | Implemented |
| `naturalist:hippo` | Territorial amphibious animal; bites and attacks boats. | Passive: water breathing and swim. Active: jaw bite with strong knockback; do not reproduce boat-targeting AI. | Implemented |
| `naturalist:lion` | Pride predator; coordinated prey hunt and melee attack. | Passive: low-light stalking. Active: pounce that damages the first hit target. | Implemented |
| `naturalist:lizard` | Wall climbing; can shed a tail to distract a threat. | Passive: wall climbing. Active: shed-tail decoy/brief speed escape; must use a safe temporary entity or particles, never a persistent duplicate. | Implemented |
| `naturalist:moose` | Large defensive ungulate; antler/melee attack. | Passive: knockback resistance. Active: antler charge. | Implemented |
| `naturalist:rattlesnake` | Rattle warning; venomous bite. | Passive: low profile. Active: short-range poison bite. | Implemented |
| `naturalist:rhino` | Defensive target acquisition; prepares then performs a damaging charge. | Passive: high knockback resistance. Active: telegraphed straight-line horn charge. | Implemented |
| `naturalist:robin` | Small flocking bird; flight and perch behaviour. | Passive: controlled bird flight. No special active. | Implemented |
| `naturalist:snail` | Slow movement; retreats into shell for protection. | Passive: slow movement profile. Active: shell retreat—strong temporary resistance/slowness. | Implemented |
| `naturalist:snake` | Small snake with melee bite; ground movement. | Passive: low profile. Active: short-range bite without poison. | Implemented |
| `naturalist:sparrow` | Small flocking bird; flight and perch behaviour. | Passive: controlled bird flight. No special active. | Implemented |
| `naturalist:tortoise` | Slow amphibious reptile; retracts into shell; lays eggs. | Passive: water breathing and reduced fall damage. Active: shell retreat—temporary resistance/slowness. | Implemented |
| `naturalist:vulture` | High-altitude flight; searches for carrion and attacks weak prey. | Passive: controlled flight and slow-fall. Active: diving peck. | Implemented |
| `naturalist:zebra` | Timid herd herbivore; fast escape. | Passive: reduced fall damage. Active: forward sprint/leap. | Implemented |

## Alex's Mobs — 90 root morphs

| Identity ID | In-mod abilities / defining mechanics | Morph mapping | Status |
| --- | --- | --- | --- |
| `alexsmobs:grizzly_bear` | Defensive claw swipes; protects cubs; raids honey; can be tamed/mounted after trust. | Passive: knockback resistance. Active: claw swipe/nearby knockback. | Implemented |
| `alexsmobs:roadrunner` | Very fast runner; evasive; hunts small arthropods. | Passive: high land speed. Active: long forward sprint. | Implemented |
| `alexsmobs:bone_serpent` | Multipart lava swimmer; hostile bite; can be placated with bones. | Passive: lava swimming/fire immunity. Active: lava-only forward surge/bite. | Implemented |
| `alexsmobs:gazelle` | Timid herd animal; rapid bounding escape. | Passive: reduced fall damage. Active: high forward leap. | Implemented |
| `alexsmobs:crocodile` | Amphibious ambusher; bites, drags prey, lays eggs. | Passive: water breathing/fast swim. Active: bite that pulls a raycast target. | Implemented |
| `alexsmobs:fly` | Flying nuisance; harasses undead and circles targets. | Passive: controlled insect flight. Active: tiny forward buzz-dash; no damage by default. | Implemented |
| `alexsmobs:hummingbird` | Hovering flight; uses feeders and pollinates. | Passive: precise controlled flight. Active: vertical burst. | Implemented |
| `alexsmobs:orca` | Fast swimmer; hunts aquatic prey; assists swimmers. | Passive: water breathing and fast swim. Active: water-only ram/dash. | Implemented |
| `alexsmobs:sunbird` | Flying defensive bird; solar/fire-themed attack and healing interaction. | Passive: controlled flight and fire immunity. Active: solar flare that ignites hostile hit targets. | Implemented |
| `alexsmobs:gorilla` | Heavy melee; chest beating/display; can use items and defend troop. | Passive: knockback resistance. Active: ground-pound cone knockback. | Implemented |
| `alexsmobs:crimson_mosquito` | Flies; drains blood, heals from it; can become a Warped Mosco through rare interaction. | Passive: controlled flight. Active: short blood-drain raycast that damages and heals the player. | Implemented |
| `alexsmobs:rattlesnake` | Defensive warning rattle and venomous bite. | Passive: low profile. Active: short-range poison bite. | Implemented |
| `alexsmobs:endergrade` | End flying creature; agile aerial movement. | Passive: controlled flight plus ender-style fall protection. Active: air dash. | Implemented |
| `alexsmobs:hammerhead_shark` | Aquatic predator; bites targets; can be placated. | Passive: water breathing and strong swim. Active: water-only bite charge. | Implemented |
| `alexsmobs:lobster` | Aquatic crustacean; claw melee and variant shells. | Passive: water breathing. Active: claw pinch with brief knockback. | Implemented |
| `alexsmobs:komodo_dragon` | Large lizard; poisonous bite. | Passive: low profile. Active: poison bite. | Implemented |
| `alexsmobs:capuchin_monkey` | Climbs; picks up and throws items; can be tamed. | Passive: wall climbing. Active: harmless item/projectile toss only when a safe ammo rule is defined. | Implemented |
| `alexsmobs:centipede_head` | Cave Centipede root; multipart wall/ceiling crawler; melee bite; placatable. | Passive: wall and ceiling climbing. Active: forward bite/lunge. | Implemented |
| `alexsmobs:warped_toad` | Amphibious mountable/tameable creature; long tongue attack. | Passive: water breathing and jump boost. Active: tongue pull against a raycast target. | Implemented |
| `alexsmobs:moose` | Large defensive animal; antler attack/charge. | Passive: knockback resistance. Active: antler charge. | Implemented |
| `alexsmobs:mimicube` | Hostile cube that disguises itself as nearby blocks and attacks. | Passive: no invisible/block disguise—too deceptive for Identity. Active: surprise leap/knockback. | Implemented |
| `alexsmobs:raccoon` | Steals items; washes food; tamed utility behaviour. | Active: pilfer one loose item or use existing random-inventory drop only if griefing is enabled; default should be disabled. | Implemented |
| `alexsmobs:blobfish` | Slow fragile deep-sea fish. | Passive: water breathing and slow swim. No special active. | Implemented |
| `alexsmobs:seal` | Amphibious swimmer; social/playful behaviour. | Passive: water breathing and fast swim. Active: short belly-slide on ice/water edge. | Implemented |
| `alexsmobs:cockroach` | Fast scavenger; picks up food; dances to maracas; carries hats/items. | Passive: small-body mobility. Active: dance emote only; no combat effect. | Implemented |
| `alexsmobs:shoebill` | Large wading bird; bill strike; hunts aquatic prey. | Passive: water wading/swim. Active: downward bill strike with brief stun/knockback. | Implemented |
| `alexsmobs:elephant` | Powerful trunk shove; can carry chest/mount; herd defence. | Passive: high knockback resistance. Active: trunk shove/cone knockback. | Implemented |
| `alexsmobs:soul_vulture` | Nether flying scavenger; attacks with soul/fire themed effects. | Passive: controlled flight, fire immunity. Active: soul siphon that heals the player only when a hostile target is hit. | Implemented |
| `alexsmobs:snow_leopard` | Snowy predator; stealthy pounce and climbing. | Passive: wall climbing and powder-snow safety. Active: pounce. | Implemented |
| `alexsmobs:spectre` | Ethereal flying mount; tameable with soul hearts. | Passive: controlled flight and slow-fall. Active: ethereal dash; never phase through blocks. | Implemented |
| `alexsmobs:crow` | Flying scavenger; tamed crows collect items and deposit them in chests. | Passive: controlled flight. Active: upward flight burst; exclude autonomous chest/item automation from morphs. | Implemented |
| `alexsmobs:alligator_snapping_turtle` | Ambushes with lure and powerful bite; aquatic/armoured. | Passive: water breathing and resistance while still. Active: jaw snap with strong single-target damage. | Implemented |
| `alexsmobs:mungus` | Mushroom creature; interacts with mushrooms and can release a spore blast. | Passive: mycelium-themed visual only. Active: short spore cloud that weakens/slows nearby hostile mobs. | Implemented |
| `alexsmobs:mantis_shrimp` | Super-fast heated punches knock targets back; tameable and can break a selected block type. | Passive: water breathing. Active: charged punch with fire and knockback; exclude autonomous block breaking. | Implemented |
| `alexsmobs:guster` | Hostile sand elemental; creates gusts and throws sand. | Passive: fall-damage immunity. Active: gust cone that pushes entities and applies brief blindness. | Implemented |
| `alexsmobs:warped_mosco` | Boss: flight, heavy punches, hemolymph/fire attacks, rushes. | Passive: controlled flight/fire immunity. Active: heavy punch or hemolymph shot; boss-level damage must be scaled down. | Implemented |
| `alexsmobs:straddler` | Lava walker/mount; ranged spit attack; breeds Stradpoles. | Passive: lava walking/swimming and fire immunity. Active: fireball-like spit with griefing disabled. | Implemented |
| `alexsmobs:stradpole` | Small lava-swimming juvenile; can be carried/raised. | Passive: lava swimming/fire immunity. No special active. | Implemented |
| `alexsmobs:emu` | Fast bird; defensive kick; lays eggs. | Passive: high land speed/reduced fall damage. Active: kick/short dash. | Implemented |
| `alexsmobs:platypus` | Amphibious, lays eggs; males use a venomous spur; can seek insects. | Passive: water breathing. Active: close venom-spur strike. | Implemented |
| `alexsmobs:dropbear` | Hostile ceiling climber; drops onto targets. | Passive: wall/ceiling climbing and fall-damage immunity. Active: downward pounce only when airborne. | Implemented |
| `alexsmobs:tasmanian_devil` | Defensive animal; spinning/bite attack and fast rush. | Passive: knockback resistance. Active: spin dash that knocks back nearby targets. | Implemented |
| `alexsmobs:kangaroo` | Exceptional hop; boxing kick; pouch/mount mechanics. | Passive: reduced fall damage and high jump. Active: boxing kick. | Implemented |
| `alexsmobs:cachalot_whale` | Massive ocean swimmer; echolocation, ramming, sleeps vertically; rescues reward ambergris. | Passive: water breathing/fast swim. Active: water-only sonar pulse that reveals nearby mobs or disorients hostile aquatic targets; no terrain destruction. | Implemented |
| `alexsmobs:leafcutter_ant` | Carries leaves; builds/uses colony chambers; defensive swarm bite. | Passive: small-body wall climbing. Active: tiny bite; exclude colony construction and block harvesting. | Implemented |
| `alexsmobs:enderiophage` | Flying End parasite; attacks End creatures; fires a rocket/uses teleport-like mobility. | Passive: controlled flight and ender fall protection. Active: small ender rocket/short blink; avoid block damage. | Implemented |
| `alexsmobs:bald_eagle` | Tameable raptor; flies, perches on glove, grabs/tackles prey. | Passive: controlled flight. Active: aerial tackle that lightly damages and pushes a target. | Implemented |
| `alexsmobs:tiger` | Stalking apex predator; pounce and melee. | Passive: reduced fall damage. Active: pounce. | Implemented |
| `alexsmobs:tarantula_hawk` | Flying wasp; venomous sting paralyses prey; buries prey for larvae. | Passive: controlled insect flight. Active: poison/paralysis sting; exclude burying/larvae logic. | Implemented |
| `alexsmobs:void_worm` | End boss: homing void crystals, bite, portals, destructible multipart body that splits. | Passive: controlled flight and void/fall immunity. Active: scaled void-crystal shot **or** bite. Explicitly exclude splitting, portals, and terrain breaking. | Implemented |
| `alexsmobs:frilled_shark` | Deep-ocean shark; fast bite attack. | Passive: water breathing and strong swim. Active: water-only bite charge. | Implemented |
| `alexsmobs:mimic_octopus` | Aquatic camouflage that mimics nearby mobs; ink/defensive reactions. | Passive: water breathing and swim. Active: ink cloud that blinds/slows nearby hostile mobs; visual disguise only, never entity impersonation. | Implemented |
| `alexsmobs:seagull` | Flying scavenger; steals food/items and can carry them. | Passive: controlled flight. Active: swoop emote; exclude inventory theft by default. | Implemented |
| `alexsmobs:froststalker` | Cold biome pack predator; icy melee/freezing effect. | Passive: powder-snow/frost protection. Active: frost bite that applies short slowness/freezing. | Implemented |
| `alexsmobs:tusklin` | Large Nether boar; strong tusk charge/knockback; fire adapted. | Passive: fire immunity and knockback resistance. Active: horn charge. | Implemented |
| `alexsmobs:laviathan` | Huge aquatic creature; water travel and mount-like movement. | Passive: water breathing and very fast swim. Active: water-only burst. | Implemented |
| `alexsmobs:cosmaw` | Tameable cosmic flyer; evolves into Cosmodelphia; can carry/assist owner. | Passive: controlled flight and slow-fall. Active: long aerial dash; no evolution from a morph. | Implemented |
| `alexsmobs:toucan` | Flying tropical bird; consumes fruit and plants saplings. | Passive: controlled flight. Active: seed/sapling placement only if block-placement abilities are enabled; otherwise no special active. | Implemented |
| `alexsmobs:maned_wolf` | Timid predator; hunts small prey and uses high pounce. | Passive: reduced fall damage. Active: pounce. | Implemented |
| `alexsmobs:anaconda` | Multipart amphibious snake; grabs and constricts prey. | Passive: water breathing and swim. Active: targeted constriction—brief root/slow plus damage. | Implemented |
| `alexsmobs:anteater` | Long tongue eats ants/insects; digs/forages. | Passive: no special passive. Active: tongue lash that damages arthropod targets; no block harvesting. | Implemented |
| `alexsmobs:rocky_roller` | Hostile stone creature; rolls rapidly and attacks by impact. | Passive: high knockback resistance. Active: rolling charge with self speed and contact damage. | Implemented |
| `alexsmobs:flutter` | Flower spirit; flies, pollinates, and changes its flower form. | Passive: controlled flight. Active: pollen pulse that lightly grows nearby crops; cosmetic form changes only. | Implemented |
| `alexsmobs:gelada_monkey` | Herd primate; grazing/foraging and defensive melee. | Passive: no special passive. Active: short melee shove. | Implemented |
| `alexsmobs:jerboa` | Tiny desert jumper; grants a nearby-player desert buff in the mod. | Passive: high jump/reduced fall damage. Active: sand hop; do not grant broad potion buffs without balance approval. | Implemented |
| `alexsmobs:terrapin` | Aquatic turtle; hides/retracts and lays eggs. | Passive: water breathing and reduced fall damage. Active: shell retreat resistance. | Implemented |
| `alexsmobs:comb_jelly` | Drifting bioluminescent aquatic creature; colour variants. | Passive: water breathing and gentle swim. No special active. | Implemented |
| `alexsmobs:cosmic_cod` | Exotic fish; schools and has cosmic visual effects. | Passive: water breathing and swim. No special active. | Implemented |
| `alexsmobs:bunfungus` | Rabbit–fungus creature; jumps; interacts with fungal transformation. | Passive: high jump/reduced fall damage. Active: fungus hop; exclude transforming other entities. | Implemented |
| `alexsmobs:bison` | Herd animal; heavy charge and headbutt. | Passive: high knockback resistance. Active: telegraphed horn charge. | Implemented |
| `alexsmobs:giant_squid` | Large deep-sea squid; tentacle grapples, ink defence, attacks whales. | Passive: water breathing and fast swim. Active: water-only tentacle pull **or** ink cloud; choose one keybind action. | Implemented |
| `alexsmobs:sea_bear` | Secret/joke ocean attacker enabled by special settings; anti-Sea-Bear circle interaction. | Passive: water breathing. Active: none by default—gate identity behind the same special-setting compatibility flag. | Implemented |
| `alexsmobs:devils_hole_pupfish` | Small fish that eats moss and follows water currents. | Passive: water breathing and swim. No special active. | Implemented |
| `alexsmobs:catfish` | Large catfish seeks food/lanterns and swims. | Passive: water breathing and swim. No special active. | Implemented |
| `alexsmobs:flying_fish` | Aquatic fish that leaps out and glides. | Passive: water breathing plus short glide after leaving water. Active: water-exit leap. | Implemented |
| `alexsmobs:skelewag` | Hostile skeletal aquatic creature; melee bite. | Passive: water breathing and swim. Active: water-only bite charge. | Implemented |
| `alexsmobs:rain_frog` | Small frog; burrows and emerges with rain-related behaviour. | Passive: high jump/reduced fall damage. Active: short burrow-style escape (particles + brief invisibility), no actual clipping. | Implemented |
| `alexsmobs:potoo` | Nocturnal bird that camouflages as a branch and flies. | Passive: controlled flight. Active: still/perch camouflage—brief invisibility only while motionless. | Implemented |
| `alexsmobs:mudskipper` | Amphibious fish; walks on land and spits mud balls. | Passive: water breathing and land movement. Active: mud-ball projectile with weak slowness, no block damage. | Implemented |
| `alexsmobs:rhinoceros` | Defensive large animal; horn charge and knockback. | Passive: high knockback resistance. Active: telegraphed horn charge. | Implemented |
| `alexsmobs:sugar_glider` | Small climber; long controlled glide. | Passive: wall climbing and slow-fall/glide. Active: launch into glide. | Implemented |
| `alexsmobs:farseer` | Hostile flying End creature; opens portals and attacks from the air. | Passive: controlled flight and ender fall protection. Active: short air dash/void shot; exclude portals. | Implemented |
| `alexsmobs:skreecher` | Hostile ceiling-hanger; drops/attacks prey from above. | Passive: wall/ceiling climbing and fall immunity. Active: downward ambush pounce. | Implemented |
| `alexsmobs:underminer` | Ghostly mineshaft mob; phases through walls, hides, mines to reveal ore; magic-sensitive. | Passive: low-light visual only. Active: ore-sense pulse that highlights nearby ore for the player; explicitly exclude phasing and automatic mining. | Implemented |
| `alexsmobs:murmur` | Hostile multipart cave creature; buried body with independently attacking head/tendons. | Passive: no special passive. Active: short ground lash/target pull; exclude multipart spawning and underground clipping. | Implemented |
| `alexsmobs:skunk` | Defensive spray creates stink/fart cloud and repels attackers. | Passive: no special passive. Active: rear cone stink cloud that weakens/slows hostile mobs; never harm allies by default. | Implemented |
| `alexsmobs:banana_slug` | Slow passive slug; produces slippery mucus/slime interaction. | Passive: slow movement. Active: mucus trail with a short, non-griefing slowness effect on hostile mobs only. | Implemented |
| `alexsmobs:blue_jay` | Flying bird; follows feeders/raccoons and handles small items. | Passive: controlled flight. No special active. | Implemented |
| `alexsmobs:caiman` | Amphibious crocodilian; bite/ambush, eggs and nest defence. | Passive: water breathing and fast swim. Active: water-only jaw lunge/pull. | Implemented |
| `alexsmobs:triops` | Small freshwater crustacean; digs and lays eggs. | Passive: water breathing and swim. Active: no special active; do not reproduce digging/block conversion. | Implemented |

## Cross-cutting implementation rules

1. **Preserve player safety and server authority.** Every active attack, dash, pull, projectile, and world interaction must be initiated and validated server-side. Raycast target, range, line of sight, cooldown, permissions, and gamerule/config checks belong on the server.
2. **Do not expose destructive or autonomous mob AI as a player power by default.** This excludes Void Worm portals/splitting/terrain damage, Underminer mining/phasing, Mantis Shrimp and Toucan autonomous block work, item theft, nest/colony construction, entity transformation, and all drops/breeding logic.
3. **Use existing generic primitives where possible.** The current `PredefIdentityAbilities` already has patterns for dash, water dash, raycast poison/leech/pull, nearby knockback, teleport, and random morph. Generalise those primitives instead of implementing one-off entity-class code.
4. **Make movement traits declarative.** Flight, water breathing, lava/fire immunity, wall climb, slow fall, high jump, and fall-damage resistance should be reusable capability flags, not 121 bespoke tick handlers.
5. **One active input per identity initially.** A multi-attack boss should have one balanced signature ability in the first release. Secondary attacks can arrive later as a configurable cycle or alternate input after the core system is proven.
6. **Respect optional dependencies.** Naturalist and Alex's Mobs must remain safe to absent-load on Fabric, Forge, and NeoForge. Register by resource location and runtime `Platform.isModLoaded`, as the current Alex's Mobs implementation does.

## Implemented architecture

1. **Complete data inventory.** The data pack contains 31 Naturalist and 90 Alex's Mobs definitions—one for every root morph and none for helper, projectile, or multipart body entity types. An explicit `action: "none"` suppresses both the active HUD and the legacy generic fallback.
2. **Reusable action model.** Definitions declare `action`, `strength`, `range`, `duration`, and `traits` in addition to the existing icon/cooldown fields. The shared executor implements movement, melee, status, pull, guard, cloud, ranged, sense, camouflage, escape, dance, crop, and inventory families without linking either optional mod's Java classes.
3. **Declarative passives.** Shared hooks implement flight, water breathing/water-only breathing, fast swimming, fire immunity, lava mobility, wall climbing, slow fall, fall resistance, land speed, jump boost, freeze immunity, and knockback resistance.
4. **Server authority and safety.** The server re-resolves the current identity and definition, rejects dead/spectator users, validates cooldown and preconditions, clamps data values and damage, block-clamps target raycasts, checks line of sight/team/PvP policy, and sends authoritative movement. Sonar is the only intentional through-wall entity sense. No action creates optional-mod entities or damages terrain.
5. **Coverage guard.** Startup validation checks all 121 definitions even when both mods are absent. When a namespace is installed it additionally compares the live entity registry with the root/helper inventory and reports missing, obsolete, unclassified, or invalid entries.
6. **Optional dependency profiles.** Fabric can run without Naturalist via `-PwithoutNaturalist=true`. Forge can run with both target mods via `-PwithMobMods=true`; the default profile keeps them absent. Runtime code remains resource-location-only.

## Decisions applied

- World interaction (`crop_growth`) and inventory interaction (`inventory_drop`) are server-configurable and disabled by default. Rejected preconditions do not start a cooldown.
- Modded fliers use Identity2's existing flight policy and key behaviour.
- Each morph has one primary action. Secondary/passive C2S ability packets are not used by data-driven definitions.
- Boss-derived attacks use the same bounded primitives as ordinary mobs and are capped by `moddedMorphAbilityDamageCap` (default `8.0`). Friendly fire is separately disabled by default.
- Complex source mechanics are deliberately represented safely: ceiling attachment is wall-climb plus an active downward pounce; tail decoy and burrowing are particles/status escape effects; projectiles are server raycasts with particles; personal light, portals, phasing, terrain breaking, autonomous item/block work, multipart spawning, and entity transformation are excluded.

## Verification status

- Common, Fabric, and Forge Java compilation passes.
- Fabric and Forge dedicated servers start with both target mods absent and report `definitions=121/121`.
- Forge starts with Naturalist `5.0pre5` and Alex's Mobs `1.22.9` present and reports `loaded=naturalist,alexsmobs, definitions=121/121`.
- Naturalist `5.0pre5` itself loads a client-only Minecraft class from its Fabric initializer on a dedicated server, so the Fabric-present dedicated-server matrix is blocked inside that dependency before Identity2 runs. Fabric absent-load remains verified.
- NeoForge sources are not included by this branch's Gradle settings, so no NeoForge runtime claim is made here.
