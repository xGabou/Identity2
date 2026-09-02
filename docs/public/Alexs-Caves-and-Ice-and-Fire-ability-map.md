# Alex's Caves & Ice and Fire ability map (Minecraft 1.20.1)

## Scope and evidence

This document is the implementation inventory for the exact optional-mod artifacts used by this branch. It maps every selectable root living entity; projectiles, multipart body pieces, eggs, vehicles, and other helper entities are inventoried separately and are intentionally not morphs.

| Mod | Artifact | Exact mod version / source | Root morphs | Ability definitions |
| --- | --- | --- | ---: | ---: |
| Alex's Caves | CurseForge file [`5848216`](https://www.curseforge.com/minecraft/mc-mods/alexs-caves/files/5848216) | `2.0.2`; entity registry and behavior checked against the [official source](https://github.com/AlexModGuy/AlexsCaves/tree/94eeb081) and the [Alex's Caves Wiki](https://alexscaves.wiki.gg/wiki/Alex%27s_Caves_Wiki) | 43 | 43 |
| Ice and Fire | CurseForge file [`5633453`](https://www.curseforge.com/minecraft/mc-mods/ice-and-fire-dragons/files/5633453) | `2.1.13-1.20.1`, tag [`1.20.1-2.1.13-beta-5`](https://github.com/AlexModGuy/Ice_and_Fire/tree/1.20.1-2.1.13-beta-5), commit [`d3a6fda`](https://github.com/AlexModGuy/Ice_and_Fire/commit/d3a6fda1413942ab7d182a330b79d444e982765d); behavior cross-checked against the [Ice and Fire Wiki](https://ice-and-fire-mod.fandom.com/wiki/Ice_and_Fire_Mod_Wiki) | 30 | 30 |

The root/helper split comes from the entity registries in those exact sources and from the installed JARs, not from a current-version mob list. Source behavior and wiki descriptions establish what each native mob can do; the JSON and Java implementation in this repository establish what a player morph can safely do.

`Native` below describes the original mod. `Identity 2 mapping` is the implemented player-facing result. An action name in backticks is the data-driven action key, followed by its cooldown in ticks. `No special active` is intentional coverage, not a missing definition.

## Controls and common rules

- Primary Ability defaults to `V`. Every active below uses this key unless stated otherwise.
- Secondary Ability defaults to `B`. In this compatibility set it is used by the custom Underzealot ritual.
- Normal attack remains the player's ordinary hand/weapon attack. In particular, the Underzealot's slash is not given a redundant special key.
- `flight` uses Identity's controlled morph flight and still respects `enableFlight` and the configured flight advancement requirements. A player morphed as a flying mount flies directly; the implementation does not make the player ride their own morph.
- Aquatic traits provide water breathing and/or faster swimming as listed. `water_only` retains the aquatic out-of-water restriction. `lava_mobility`, `fire_immune`, `freeze_immune`, `climb`, `high_jump`, `slow_fall`, `fall_resistant`, and `knockback_resistant` have their literal morph meanings.
- All active effects are authoritative on the server. Allies are excluded. Players are excluded unless `enableModdedMorphFriendlyFire=true`, and ordinary PvP permission is still required.
- Generic direct damage is capped by `moddedMorphAbilityDamageCap` (default `8`). Dragon breath damage has its own `moddedDragonBreathDamageCap` (default `18`). Ranges, strengths, and durations are clamped by the data-driven loader.
- `enableModdedMorphWorldInteractions` (default `false`) gates generic block-changing actions. `enableModdedMorphInventoryInteractions` (default `false`) gates the Gingerbread Man theft mapping. `enableModdedMorphSummons` (default `false`) gates the Dread Lich summon.
- Boss mechanics are scaled into bounded player abilities. Unless a row explicitly says otherwise, attacks do not break terrain, create native boss arenas, reproduce tame/owner AI, or create permanent helper projectiles.

## Alex's Caves - 43 root morphs

| Identity ID | Native abilities / defining behavior | Identity 2 mapping | Safety and approximation notes | Status |
| --- | --- | --- | --- | --- |
| `alexscaves:teletor` | Floats and telekinetically controls magnetic tools/weapons, pulling or striking targets from range. | Primary `pull` (100): raycast pull, strength `1.1`, range `12`. Passive: flight, slow fall, fall resistance. | Reproduces telekinesis without spawning or stealing a magnetic weapon. | Implemented |
| `alexscaves:magnetron` | Builds a body from magnetic blocks, rolls/charges in a compact state, and reconfigures its shell. | Primary `shell_guard` (220): temporary defensive guard for 100 ticks. Passive: fall and knockback resistance. | Does not take blocks from the world or construct a persistent body. | Implemented |
| `alexscaves:boundroid` | Ceiling-bound machine paired with a winch; drops/slams on victims and retracts. | Primary `down_pounce` (100): downward slam/pounce, strength `1.3`, radius `6`. Passive: climb and fall resistance. | `boundroid_winch` remains a helper, not a second morph or spawned tether. | Implemented |
| `alexscaves:ferrouslime` | Floating magnetic slime that can split, merge, and change size. | Primary `air_dash` (70): short aerial impulse. Passive: flight, slow fall, fall resistance. | Split/merge duplication and persistent size multiplication are omitted. | Implemented |
| `alexscaves:notor` | Small flying scanner that surveys an area and alerts nearby magnetic mobs. | Primary `sonar` (160): reveals nearby living targets for 140 ticks in range `16`. Passive: flight, slow fall, fall resistance. | No autonomous alarm network or mob recruitment. | Implemented |
| `alexscaves:subterranodon` | Rideable cave pterosaur with full flight, aerial movement, and a bite. | Primary `upward_burst` (60): vertical launch, strength `0.7`. Passive: flight, slow fall, fall resistance. | Morph flight replaces self-riding; normal attack represents the bite. | Implemented |
| `alexscaves:vallumraptor` | Fast pack hunter with pounce/grab attacks, item stealing, stealthy behavior, and pack recovery. | Primary `pounce` (80): forward pounce, strength `1.4`, range `6`. Passive: fast land movement, high jump, fall resistance. | No inventory theft, target carrying, pack AI, or passive regeneration exploit. | Implemented |
| `alexscaves:grottoceratops` | Heavy ceratopsian using horn launches, charges, and tail defense. | Primary `knockback` (110): close radial horn/tail shove, strength `2`, range `5`. Passive: knockback resistance. | Condenses horn and tail attacks into a bounded displacement pulse. | Implemented |
| `alexscaves:trilocaris` | Amphibious trilobite-like animal with a close bite and strong swimming. | Primary `bite` (60): strength `3`, range `3`. Passive: water breathing and fast swim. | Direct, terrain-safe translation. | Implemented |
| `alexscaves:tremorsaurus` | Large predator with fear roar, grab-and-shake bite, powerful melee, and mount behavior. | Primary `gust` (240): roar-like damaging push, strength `2.2`, range `10`. Passive: fall and knockback resistance. | Does not seize/control another entity or reproduce fear AI; morph movement replaces riding. | Implemented |
| `alexscaves:relicheirus` | Giant clawed dinosaur that swipes and can topple trees while foraging/fighting. | Primary `swipe` (100): strength `7`, range `4`. Passive: knockback resistance. | Tree toppling and block destruction are omitted. | Implemented |
| `alexscaves:luxtructosaurus` | Volcanic boss with fire/lava immunity, kick, tail, stomp, leap, flame attacks, and roar. | Primary `fire_strike` (260): fiery close strike, strength `8`, range `12`, burn window `100`. Passive: fire immunity, lava mobility, fall and knockback resistance. | One capped strike represents the boss kit; no tephra, terrain destruction, or arena-scale attack. | Implemented |
| `alexscaves:atlatitan` | Enormous sauropod using kicks, tail sweeps, and stomps. | Primary `down_pounce` (180): heavy stomp, strength `1.8`, radius `8`. Passive: fall and knockback resistance. | A bounded stomp replaces block-crushing and broad body attacks. | Implemented |
| `alexscaves:nucleeper` | Charges and produces a native nuclear explosion; charged state increases the blast. | Primary custom Nucleeper blast (400): creates Alex's Caves' nuclear-explosion entity at the player and preserves charged-size scaling. | If `mobGriefing=false`, `setNoGriefing(true)` is applied. With `mobGriefing=true`, this intentionally retains the native explosion's world impact. Optional-mod access is reflective. | Implemented (custom predefinition) |
| `alexscaves:radgill` | Irradiated aquatic creature with acidic/radiation theming and a strong leap out of water. | Primary `water_exit_leap` (70): water-edge leap, strength `1.1`. Passive: water breathing, fast swim, aquatic restriction. | The locomotion signature is retained; no persistent acid or radiation is created. | Implemented |
| `alexscaves:brainiac` | Uses a long tongue, carries/throws waste drums, spreads acid, heals from radiation, and performs smash attacks. | Primary `tongue_pull` (110): pulls a target from range `10`, strength `1.2`. | The tongue is the safe signature action; drum spawning, acid blocks, healing loop, and smash are not stacked into one morph. | Implemented |
| `alexscaves:gammaroach` | Irradiated insect that rams/carries targets and emits a radiation gas cloud. | Primary `spore_cloud` (150): weakness cloud in radius `5` for 100 ticks. | Vanilla particles/effect safely approximate radiation gas; no target carrying. | Implemented |
| `alexscaves:raycat` | Seeks and absorbs Irradiated levels from nearby creatures, healing itself. | Primary `radiation_absorb` (160): removes or reduces the nearest Irradiated effect in range `9`, heals `5`, and grants regeneration for 80 ticks. | Fails without a nearby irradiated target; cannot manufacture radiation or drain health directly. | Implemented |
| `alexscaves:tremorzilla` | Tameable kaiju with bite/tail/stomp/roar, swimming and riding, fire immunity, and a destructive energy beam. | Primary `fire_strike` (360): capped beam-like fiery strike, strength `8`, range `24`, burn window `100`. Passive: fall resistance, fire immunity, water breathing, fast swim, lava mobility, knockback resistance. | Does not carve terrain, grow, charge crystals, or expose the native ride/beam subsystem. | Implemented |
| `alexscaves:lanternfish` | Small bioluminescent deep-water fish. | No special active. Passive: water breathing, fast swim, aquatic restriction. | Light is cosmetic; no placed-light griefing. | Implemented |
| `alexscaves:sea_pig` | Benthic deep-sea animal with seafloor locomotion and ambient foraging. | No special active. Passive: water breathing and aquatic restriction. | Ambient AI is not promoted into an ability. | Implemented |
| `alexscaves:hullbreaker` | Huge abyssal predator that accelerates into rams and can wreck submarines/structures. | Primary `charge` (120): strength `1.8`, reach `8`. Passive: water breathing, fast swim, aquatic restriction, knockback resistance. | Damages eligible entities only; never breaks a hull or terrain. | Implemented |
| `alexscaves:gossamer_worm` | Small fully aquatic abyssal worm with evasive swimming. | No special active. Passive: water breathing, fast swim, aquatic restriction. | Multipart/projectile behavior is not applicable. | Implemented |
| `alexscaves:tripodfish` | Benthic fish that stands and moves along the deep seafloor. | No special active. Passive: water breathing and aquatic restriction. | Native posture/ambient behavior remains visual only. | Implemented |
| `alexscaves:deep_one` | Amphibious society mob with scratch/bite combat, thrown attacks, barter/reputation, and disappearing behavior. | Primary `ink_cloud` (140): blindness cloud, radius `5`, duration `100`. Passive: water breathing and fast swim. | A defensive abyssal cloud is used instead of copying barter/reputation or disappearing from the world. | Implemented |
| `alexscaves:deep_one_knight` | Armored Deep One with bite/scratch and ranged trident throwing; participates in barter/reputation. | Primary `mud_shot` (120): slowing ranged strike, strength `6`, range `14`, duration `50`. Passive: water breathing and fast swim. | Particle/raycast shot replaces a persistent thrown trident. | Implemented |
| `alexscaves:deep_one_mage` | Floating caster with water bolts, waves/spin attack, trade behavior, and magical disappearance. | Primary `mud_shot` (120): slowing water-like ranged strike, strength `6`, range `16`, duration `80`. Passive: water breathing, fast swim, slow fall, fall resistance. | No wave helper, teleport/disappearance, or trade-state mutation. | Implemented |
| `alexscaves:mine_guardian` | Anchored aquatic guardian that detonates after its attack charge. | Primary `blast` (260): entity-only explosion pulse, strength `7`, radius `6`. Passive: water breathing and fast swim. | Uses particles, damage, and knockback; does not spawn its anchor/depth charge or damage blocks. | Implemented |
| `alexscaves:gloomoth` | Harmless flying moth attracted to light and repelled by mothballs; valid Underzealot sacrifice. | No special active. Passive: flight, slow fall, fall resistance. | It remains important to the ritual without receiving a fabricated combat power. | Implemented |
| `alexscaves:underzealot` | Normal slash attack, tactical burrowing, and cooperative worship ritual using a Gloomoth or Vesper. | Primary custom burrow (80). Secondary custom ritual (`B`; details below). Passive: fall resistance. | Exact compatibility behavior is documented in the dedicated section below; normal slash stays on normal attack. | Implemented (custom predefinition) |
| `alexscaves:watcher` | Ritual-created ethereal minion that possesses a victim/camera and immobilizes it while attacking. | Primary `constrict` (180): damages and briefly roots/slows one target, strength `4`, range `10`, duration `60`. Passive: flight, slow fall, fall resistance. | Deliberately does not take over another player's camera, input, or identity. | Implemented |
| `alexscaves:corrodent` | Underground predator that burrows through terrain to ambush or escape. | Primary `burrow_escape` (100): temporary escape state for 50 ticks. Passive: fall resistance. | Never tunnels or deletes blocks. | Implemented |
| `alexscaves:vesper` | Flying/hanging cave predator with diving bite attacks; valid Underzealot sacrifice. | Primary `pounce` (80): aerial pounce, strength `1.3`, range `6`. Passive: flight, slow fall, fall resistance. | Target grabbing and hanging AI are omitted. | Implemented |
| `alexscaves:forsaken` | Ritual-created boss/minion with bite/slashes, jump, ground smash, pickup, sonic attacks, and darkness-linked recovery. | Primary `gust` (240): sonic push, strength `2.5`, range `14`. Passive: fall and knockback resistance. | Represents the signature sonic kit; no pickup/control, summon chain, or darkness regeneration exploit. | Implemented |
| `alexscaves:sweetish_fish` | Colored candy fish hunted by matching Gummy Bears. | No special active. Passive: water breathing, fast swim, aquatic restriction. | Color/ecology is retained by the native morph data, not exposed as combat power. | Implemented |
| `alexscaves:caniac` | Fast candy humanoid with a long lunge and melee, sometimes controlled by a Licowitch. | Primary `pounce` (90): strength `1.5`, range `7`, duration `40`. Passive: fast land movement, high jump, fall resistance. | Does not reproduce Licowitch possession. | Implemented |
| `alexscaves:gumbeeper` | Charged candy attacker that fires gumballs and can burst/explode. | Primary `mud_shot` (100): sticky slowing ranged strike, strength `5`, range `14`, duration `50`. | A safe sticky shot replaces gumball entities and self/world explosion. | Implemented |
| `alexscaves:candicorn` | Tameable rideable candy unicorn with horn stab, buck, and sustained charge. | Primary `charge` (90): strength `1.8`, reach `7`. Passive: fast land movement and high jump. | Morph locomotion replaces mounting; no tame/owner meter is copied. | Implemented |
| `alexscaves:gum_worm` | Multipart boss/mount that burrows, leaps, bites around its mouth, and destroys Gobthumpers. | Primary `burrow_escape` (120): safe underground-themed escape for 60 ticks. Passive: fire immunity, water breathing, fall and knockback resistance. | Never creates segments, breaks blocks, destroys Gobthumpers, or enters native rider AI. | Implemented |
| `alexscaves:caramel_cube` | Bouncing slime that attacks on landing and leaves melted caramel. | Primary `mucus_trail` (120): slowing caramel-like area, radius `4`, duration `100`. Passive: fall resistance. | Status/particles replace persistent caramel blocks/entities. | Implemented |
| `alexscaves:gummy_bear` | Colored candy animal with swipe/maul, fishing/eating, dancing, sitting, and defensive family behavior. | Primary `cocoon_guard` (240): temporary resistance/immobility for 120 ticks. | Defensive guard is a bounded abstraction; it does not claim the mob natively makes a cocoon, and it omits fishing/breeding AI. | Implemented |
| `alexscaves:licowitch` | Teleporting spellcaster that summons/possesses candy mobs and uses multiple candy projectiles/spells. | Primary `void_shot` (140): weakening ranged spell, strength `6`, range `16`, duration `100`. | No teleport, crucible mutation, possession, or persistent summon/projectile swarm. | Implemented |
| `alexscaves:gingerbread_man` | Small fast mob that opens doors, fights/flees, steals tagged sweets, and stores stolen items. | Primary `steal` (200): steals one item in `alexscaves:gingerbread_man_steals` from an eligible targeted player in range `4`. Passive: fast land movement. | Disabled unless inventory interactions are enabled; creative players, allies, and PvP-protected players are excluded. No autonomous storage AI. | Implemented |

### Underzealot custom compatibility

The native code and the requested fan behavior agree on the important correction: **Gloomoth becomes Watcher; Vesper becomes Forsaken.** The repeated use of "Vesper" in the fan note was a typo.

Primary (`V`) is a tactical burrow. It requires solid ground. The player searches up to five blocks forward for a collision-free destination, vanishes into block particles, teleports there, and receives brief invisibility, resistance, and speed. The hidden Underzealot morph's `setBuried` animation state is driven reflectively, so Alex's Caves is still an optional dependency.

Secondary (`B`) is a cooperative ritual:

1. Look at an unmounted Gloomoth or Vesper within six blocks and press `B`. The sacrifice rides the player, the morph enters its carrying animation, and the player's starting position becomes the ritual anchor.
2. Native Underzealots within 30 blocks are recruited, up to ten. They move into a prayer ring, face the sacrifice, raise their hands using the native praying/worship animation, and return to normal navigation when the group ends.
3. Other players morphed as Underzealots can approach within 12 blocks and press `B` to join or leave. A praying player receives `0.02` food exhaustion each tick and is removed if their hunger reaches zero. The holder carries the target and does not pay this prayer cost.
4. The ritual advances only with at least three actual prayers. Base completion is 500 ticks; every prayer beyond three adds 10% speed, capped at ten counted prayers. More Underzealots therefore finish it faster as requested.
5. If a player holder moves more than six blocks from the anchor, changes morph, loses the passenger, or otherwise invalidates the session, the sacrifice dismounts, all animation flags are cleared, and recruited Underzealots return to wandering. Pressing `B` again also cancels if holding or leaves if praying.
6. Completion converts Gloomoth to Watcher or Vesper to Forsaken. A player-led result is persistent, tagged to its owner, placed on the owner's scoreboard team, and periodically targets a valid creature that hurt the owner or that the owner hurt. The successful holder receives a randomized ritual cooldown of 6,000-11,999 ticks.

The compatibility can also detect a nearby ritual already being held by a native Underzealot, allowing a morphed player to join as a praying participant. It never links against Alex's Caves classes directly; native animation methods and the native mob conversion are optional-mod-safe reflective calls.

## Ice and Fire - 30 root morphs

| Identity ID | Native abilities / defining behavior | Identity 2 mapping | Safety and approximation notes | Status |
| --- | --- | --- | --- | --- |
| `iceandfire:fire_dragon` | Five growth stages; bite/shake, tail, wing, tackle, roar, flight, riding when old enough, and fire breath/charge that burns and changes terrain. | Primary `dragon_fire_breath` (100): 24-block cone, base damage `2 x stage`, burning duration that increases with stage. Passive: flight, fall resistance, fire immunity. | Body/attributes and breath strength follow the selected stage. No terrain ignition/transformation or explosive fire charge; damage is capped separately. | Implemented |
| `iceandfire:ice_dragon` | Five stages with the same physical dragon kit, flight/riding, freezing ice breath/charge, and cold terrain conversion. | Primary `dragon_ice_breath` (100): 24-block cone, base damage `2.5 x stage`, increasing slowness and frozen ticks. Passive: flight, fall resistance, freeze immunity, water breathing, fast swim. | No block freezing/spikes or explosive charge. Damage is capped separately. | Implemented |
| `iceandfire:lightning_dragon` | Five stages with physical dragon attacks, flight/riding, and lightning breath/charge; native power is strongest during thunderstorms. | Primary `dragon_lightning_breath` (100): 24-block cone, base damage `3.5 x stage`, brief slowness and a visual lightning bolt on each hit. Passive: flight and fall resistance. | Lightning bolts are visual-only so they cannot ignite blocks or double-damage targets; no weather manipulation. | Implemented |
| `iceandfire:hippogryph` | Tameable high-speed flying mount with claw/bite/kick combat and strong ground jump. | Primary `kick` (60): strength `5`, range `4`. Passive: flight, fall resistance, high jump, fast land movement. | Player-controlled morph flight replaces mounting/saddling and owner AI. | Implemented |
| `iceandfire:gorgon` | Boss whose mutual gaze permanently turns unprotected creatures into stone; blindfolds and lost line of sight counter it. | Primary `petrifying_gaze` (600): range `16`; requires an unblindfolded, non-blind target looking back, then nearly immobilizes and weakens it for 100 ticks while granting stone-like resistance. | Temporary status lock replaces a permanent statue/entity conversion. It cannot petrify through blocks or bypass the blindfold. | Implemented |
| `iceandfire:pixie` | Flying trickster that steals held items; tamed pixies can grant color-dependent positive effects. | Primary `pixie_blessing` (240): self regeneration, Speed II, and Luck for 200 ticks. Passive: flight and fall resistance. | Uses a predictable beneficial kit; no item theft, jar capture, or color-dependent imbalance. | Implemented |
| `iceandfire:cyclops` | Giant boss with heavy melee/stomp attacks, sheep hunting, a vulnerable eye, and broad knockback. | Primary `cyclops_stomp` (120): strength `8`, radius `6`, damage plus strong outward/upward knockback. Passive: knockback resistance. | Entity-only stomp; no block destruction or sheep-griefing AI. | Implemented |
| `iceandfire:siren` | Aquatic singer that lures unprotected players toward it, then changes form and attacks; earplugs counter the song. | Primary `siren_song` (300): pulls nearby targets in range `24` and applies confusion/slowness for 100 ticks. Passive: water breathing and fast swim. | Earplugs/earmuff-like headgear and sneaking counter the mapping. It applies movement, never camera or input control. | Implemented |
| `iceandfire:hippocampus` | Tameable aquatic mount with fast underwater travel. | Primary `water_dash` (80): strength `1.6`, water-required burst. Passive: water breathing and fast swim. | Morph swimming replaces mounting and saddle/chest inventory. | Implemented |
| `iceandfire:deathworm` | Desert predator that tunnels, bursts from sand, lunges/bites, and has large/giant variants. | Primary `deathworm_burrow_burst` (120): safe escape state, upward burst, then strength-`6` slam in radius `6`. Passive: fall resistance. | Does not tunnel through or destroy sand; all three colors and normal/giant sizes are selectable. | Implemented |
| `iceandfire:cockatrice` | Tameable monster using a damaging withering stare during gaze combat plus close melee; blindfolding counters gaze. | Primary `cockatrice_gaze` (240): range `14`, damage `1`, wither, slowness, and confusion for 100 ticks. | Blindfold/blindness blocks it. Effects are temporary and line-of-sight raycasted; no permanent control. | Implemented |
| `iceandfire:stymphalian_bird` | Hostile flocking flyer that launches volleys of razor feathers and attacks from the air. | Primary `feather_volley` (100): damages up to five cone targets, strength `4`, range `20`. Passive: flight and fall resistance. | Ray/particles replace retrievable projectile entities and prevent projectile litter. | Implemented |
| `iceandfire:troll` | Large regenerating cave/night fighter with a heavy weapon, broad slam, and sunlight petrification. | Primary `troll_slam` (140): strength `8`, radius `6`, damage and knockback. Passive: knockback resistance. | Does not grant unlimited regeneration or create a permanent stone statue; no block breaking. | Implemented |
| `iceandfire:myrmex_worker` | Colony worker that forages/carries items and fights with bite/sting while navigating hive walls. | Primary `poison_sting` (80): strength `3`, range `4`, poison for 200 ticks. Passive: climb. | Colony reputation, carrying, digging, and hive construction are omitted. | Implemented |
| `iceandfire:myrmex_soldier` | Armored colony defender with stronger bite/sting and escort behavior. | Primary `poison_sting` (80): strength `6`, range `4`, poison for 200 ticks. Passive: climb. | No colony recruitment or escort AI. | Implemented |
| `iceandfire:myrmex_sentinel` | Heavy caste that hides underground and bursts out in an ambush before biting/stinging. | Primary `sentinel_ambush` (160): invisibility for 100 ticks, short resistance, and a pounce, strength `9`, range `6`. Passive: climb. | Does not enter blocks or create a hidden entity; temporary invisibility represents concealment. | Implemented |
| `iceandfire:myrmex_royal` | Winged reproductive caste with flight and venomous combat. | Primary `poison_sting` (90): strength `6`, range `4`, poison for 70 ticks. Passive: flight, fall resistance, climb. | Breeding/colony founding is not a player power. | Implemented |
| `iceandfire:myrmex_queen` | Huge colony boss that lays eggs/summons castes and uses powerful bite/sting attacks. | Primary `poison_sting` (140): strength `10`, range `5`, poison for 200 ticks. Passive: climb and knockback resistance. | Boss damage is capped; no egg laying, caste spawning, or hive construction. | Implemented |
| `iceandfire:amphithere` | Tameable flying serpent mount with bite and wing/gust attacks. | Primary `gust` (100): strength `3.5`, range `10`. Passive: flight, fall resistance, fast land movement. | Morph flight replaces self-riding; no arrow/helper entity or forced passenger control. | Implemented |
| `iceandfire:sea_serpent` | Scaled ocean boss with high-speed swimming, bite, surface leaps, and ranged bubble breath. | Primary `sea_serpent_bubbles` (100): targeted 24-block bubble beam, strength `6`, with knockback. Passive: water breathing, fast swim, fall resistance, fire immunity. | Ray/particles replace bubble and arrow helpers; no block/boat destruction. | Implemented |
| `iceandfire:myrmex_swarmer` | Small temporary winged caste that flies and attacks in a swarm. | Primary `poison_sting` (80): strength `4`, range `4`, poison for 70 ticks. Passive: flight and fall resistance. | No automatic swarm creation or forced lifetime. | Implemented |
| `iceandfire:dread_thrall` | Basic reanimated humanoid serving the Dread faction with equipped melee attacks. | Primary `swipe` (50): strength `2`, range `4`. Passive: freeze immunity. | Equipment/summoner allegiance is not fabricated. | Implemented |
| `iceandfire:dread_ghoul` | Faster clawing Dread undead with strong melee. | Primary `swipe` (70): strength `5`, range `5`. Passive: freeze immunity. | Direct bounded melee mapping. | Implemented |
| `iceandfire:dread_beast` | Fast quadrupedal Dread predator with a heavy bite. | Primary `bite` (60): strength `4`, range `4`. Passive: freeze immunity and fast land movement. | Direct bounded melee mapping. | Implemented |
| `iceandfire:dread_scuttler` | Large spider-like Dread creature that climbs and bites. | Primary `bite` (60): strength `7`, range `4`. Passive: freeze immunity and climb. | No web/block placement or summoned swarm. | Implemented |
| `iceandfire:dread_lich` | Dread commander using homing skull magic and summoning Dread minions. | Primary `dread_summon` (300): with summons disabled, fires a strength-`4` withering ranged substitute to range `12`; when enabled, creates a tagged allied Dread Thrall, capped at three owned summons and expiring after at least 30 seconds. Passive: freeze immunity. | Summoning is opt-in. Expiry persists through unloads/restarts and does not depend on the owner staying online. The fallback preserves a useful skull spell without adding entities. | Implemented |
| `iceandfire:dread_knight` | Heavily armored Dread warrior with strong weapon melee. | Primary `swipe` (60): strength `4`, range `4`. Passive: freeze and knockback resistance. | No free equipment or mount. | Implemented |
| `iceandfire:dread_horse` | Fast undead horse used as a Dread mount. | Primary `dash` (80): strength `1.4`. Passive: freeze immunity, fast land movement, high jump. | Player locomotion replaces mounting/taming. | Implemented |
| `iceandfire:hydra` | Multi-headed boss with bites and multi-target poison breath; severed heads regrow unless cauterized with fire. | Primary `hydra_venom_volley` (140): up to three cone targets in range `20`, strength `3`, Poison II for 100 ticks. Passive: knockback resistance. | Does not expose head-count duplication/regrowth or hydra multipart entities; fire remains a normal counter rather than a morph resource loop. | Implemented |
| `iceandfire:ghost` | Ethereal undead that phases visually, charges targets, and launches a ghost sword. | Primary `ghost_phase_charge` (120): brief invisibility/resistance and a damaging charge, strength `6`, range `10`. Passive: flight, fall resistance, fire immunity. | Never grants noclip or movement through blocks; no persistent ghost-sword projectile. | Implemented |

## Dragon growth and variant discovery

The old behavior that exposed only the default stage-one baby has been replaced by an optional-mod-safe dragon variant adapter. Ice and Fire stores the three relevant axes in ordinary entity NBT:

| NBT key | Meaning | Discovered values |
| --- | --- | --- |
| `AgeTicks` | Dragon age and therefore growth stage, size, and native attributes | Five canonical ages shown below |
| `Variant` | Element-specific color index | Four colors (`0`-`3`) per element |
| `Gender` | Sex (`false` female, `true` male) | Female and male |

Every combination is discovered: `4 colors x 2 sexes x 5 stages = 40` variants for each dragon type, or **120 selectable dragon variants** overall.

### Canonical growth stages

| Selectable stage | Native age interval | Stored canonical age | Native growth / handling significance |
| ---: | --- | ---: | --- |
| 1 | Days 0-24 | Day 0 (`0` ticks) | Baby; smallest/weakest, and a tamed baby can use the native shoulder behavior. |
| 2 | Days 25-49 | Day 25 (`600,000` ticks) | Juvenile with increased size and attributes. |
| 3 | Days 50-74 | Day 50 (`1,200,000` ticks) | Larger dragon; native tamed dragons are old enough for riding. A morph instead receives direct controlled flight. |
| 4 | Days 75-99 | Day 75 (`1,800,000` ticks) | Large adult; native roar behavior gains its higher-stage effects. |
| 5 | Day 100 onward | **Day 125 (`3,000,000` ticks)** | Full-size/full-strength adult representative. Day 125 is deliberately used rather than the lower boundary so the selected Stage 5 is visibly and mechanically mature. |

An acquired dragon's arbitrary age is normalized to the canonical entry for its stage. Applying a selected entry sets `AgingDisabled=true`, loads the NBT into the hidden native dragon entity, and refreshes its dimensions. The chosen stage therefore remains stable instead of aging into another menu variant, and the native renderer/body size follows that stage. Breath code independently reads `AgeTicks` from the active morph and computes stages at the native boundaries (`25`, `50`, `75`, and `100` days), so attack scaling cannot fall back to Stage 1 after selection.

### Complete color/sex matrix

| Dragon ID | Color index `0` | Color index `1` | Color index `2` | Color index `3` | Per-color entries |
| --- | --- | --- | --- | --- | ---: |
| `iceandfire:fire_dragon` | Red | Green | Bronze | Gray | Female Stage 1-5 + Male Stage 1-5 = 10 |
| `iceandfire:ice_dragon` | Blue | White | Sapphire | Silver | Female Stage 1-5 + Male Stage 1-5 = 10 |
| `iceandfire:lightning_dragon` | Electric | Amethyst | Copper | Black | Female Stage 1-5 + Male Stage 1-5 = 10 |

Thus, for example, `Fire Dragon Red Stage 1 Female` through `Fire Dragon Gray Stage 5 Male` are distinct stable definitions. The same full matrix is generated for Ice and Lightning Dragons. Variant IDs remain stable because they are derived from the entity ID plus normalized NBT rather than from discovery order.

### Other Ice and Fire variants and safe defaults

The same optional adapter exposes **41 additional stable variants**, bringing the Ice and Fire selector total to **161**. It also supplies non-zero canonical size/growth fields before native NBT loading, preventing entities whose readers treat absent keys as zero from becoming invisible or unusably small.

| Root identities | Selectable variants | Canonical safety state |
| --- | ---: | --- |
| Sea Serpent | 14: seven colors, each normal or ancient | Normal scale `3.5`; ancient scale `7.5` |
| Deathworm | 6: yellow/red/white, each normal or giant | Scale `0.425` or `1.7`; stable adult worm age |
| Hydra | 3 native texture variants | Three healthy heads; severed-head and per-head damage state reset |
| Dread Ghoul | 3 native variants | Scale `1.0`; scream state reset |
| Dread Beast | 2 native variants | Scale `1.0` |
| Dread Scuttler | 1 canonical variant | Scale `1.0` |
| Six Myrmex castes | 12: desert and jungle for every caste | Adult growth stage with stable dimensions |

### Stage-scaled combat and movement

- All three elements have controlled morph flight at every selectable stage. This is the useful player equivalent of dragon flying/riding; it does not create a mount entity beneath the player.
- Native size and stage-dependent entity state come from the selected `AgeTicks`, while breath damage scales linearly by stage. Fire uses `2 x stage`, Ice `2.5 x stage`, and Lightning `3.5 x stage` before the separate dragon cap.
- Fire applies burning; Ice applies increasing frozen ticks and slowness; Lightning uses an electric particle beam, damage/slowness, and visual-only lightning.
- The cone is line-of-sight bounded, has a maximum range of 24 blocks and at most eight targets, rejects allies, and obeys the friendly-fire setting.
- Native dragon charge projectiles, block conversion, fire spread, ice placement, lightning ignition, lair AI, target grabbing/shaking, roar buffs/debuffs, and terrain destruction are not reproduced by the active. Normal morph combat and native body/attributes still cover ordinary close fighting.

## Excluded helper inventories

These IDs are explicitly classified so a version drift cannot silently turn a projectile or multipart child into a missing root morph.

### Alex's Caves - 40 exclusions

- Multipart/attached children: `boundroid_winch`, `gum_worm_segment`.
- Vehicles: `boat`, `chest_boat`, `submarine`.
- Moving blocks, area effects, anchors, and falling helpers: `moving_metal_block`, `falling_tree_block`, `crushed_block`, `nuclear_explosion`, `nuclear_bomb`, `mine_guardian_anchor`, `floater`, `falling_guano`, `falling_frostmint`, `melted_caramel`.
- Weapons, projectiles, spell effects, and thrown helpers: `magnetic_weapon`, `quarry_smasher`, `seeking_arrow`, `tephra`, `limestone_spear`, `extinction_spear`, `dinosaur_spirit`, `thrown_waste_drum`, `cinder_brick`, `ink_bomb`, `water_bolt`, `wave`, `depth_charge`, `guano`, `beholder_eye`, `desolate_dagger`, `burrowing_arrow`, `dark_arrow`, `gumball`, `spinning_peppermint`, `sugar_staff_hex`, `thrown_ice_cream_scoop`, `candy_cane_hook`, `soda_bottle_rocket`, `frostmint_spear`.

### Ice and Fire - 28 exclusions

- Multipart children: `dragon_multipart`, `multipart`, `hydra_multipart`, and the upstream registry's exact misspelled ID `cylcops_multipart`.
- Eggs, remains, statues, and ties: `dragon_egg`, `dragon_skull`, `hippogryph_egg`, `stone_statue`, `deathworm_egg`, `cockatrice_egg`, `myrmex_egg`, `chain_tie`, `mob_skull`.
- Projectiles, charges, breath, and weapon helpers: `dragon_arrow`, `fire_dragon_charge`, `ice_dragon_charge`, `lightning_dragon_charge`, `stymphalian_feather`, `stymphalian_arrow`, `amphithere_arrow`, `sea_serpent_bubbles`, `sea_serpent_arrow`, `pixie_charge`, `tide_trident`, `dread_lich_skull`, `hydra_breath`, `hydra_arrow`, `ghost_sword`.

`stone_statue` is excluded because it is a generic result/container created by petrification, not an independently playable living species. The three dragon charges are likewise attacks belonging to their root dragons, not separate identities.

## Implementation architecture and status

| Area | Implemented result |
| --- | --- |
| Root coverage | 43/43 Alex's Caves and 30/30 Ice and Fire roots have exact-ID definitions. Together with the previous Naturalist and Alex's Mobs work, the validator now inventories 194 roots across four optional mods. |
| Data-driven actions | New signature actions cover dragon breath, temporary petrification, Siren song, Cockatrice gaze, feather volley, Myrmex ambush, Sea Serpent bubbles, Hydra venom, Dread summon/fallback, ghost charge, safe blast, radiation absorption, and tagged theft. Existing safe primitives cover the remaining kits. |
| Custom abilities | Nucleeper retains its native explosion through guarded reflection. Underzealot has a server-ticked primary burrow and multi-actor secondary ritual with native animation compatibility. |
| Variants | Lazy adapters keyed only by `iceandfire` entity IDs and vanilla NBT discover and apply all 120 dragon combinations plus 41 Sea Serpent, Deathworm, Hydra, Dread, and Myrmex variants. |
| Optional-mod safety | Common and Forge compile against both mods only as `compileOnly`. Forge adds them at runtime only with `-PwithCaveAndDragonMods=true`. Fabric and an ordinary Forge runtime do not require either mod. Compatibility code does not import their classes. Hidden Alex's Caves/Ice and Fire render entities skip native AI ticks so a menu/morph preview cannot run native terrain destruction, multipart spawning, or network side effects. |
| Startup validation | Exact root and helper inventories, action vocabulary, trait vocabulary, numeric bounds, missing/stale definitions, newly unclassified installed entities, expected variant counts, and unique variant labels/NBT are checked by `ModdedMobAbilityCoverage`. |
| Server policy | Ability enablement, flight, PvP/friendly fire, direct-damage caps, world interaction, inventory interaction, summon permission, and `mobGriefing` are enforced at execution time. Preflight-known invalid uses such as a water dash on land are rejected before cooldown; target-dependent failures give safe feedback. |

### Verification status and release gate

Completed from the final implementation worktree:

- all 73 compatibility JSON files parse: 43 Alex's Caves and 30 Ice and Fire;
- a clean multi-loader Gradle build succeeds for common, Fabric, and Forge (this project currently has no Java test sources);
- an ordinary Forge dedicated server reaches `Done` without either optional mod and reports `loaded=none, definitions=194/194`;
- the Forge present-mod profile reaches `Done` with both exact artifacts and reports `loaded=alexscaves,iceandfire, definitions=194/194`;
- the present-mod startup validator discovers exactly 40 variants for each dragon and the expected 41 additional Ice and Fire variants, with unique labels and NBT tokens;
- both final loader JARs contain all 73 new compatibility definitions.

Manual in-client acceptance remains recommended for visual and interaction behavior that a headless startup cannot exercise:

- every dragon color, sex, and stage appears once in the variant selector, with Stage 5 visibly larger than Stage 1;
- changing dragon stage changes breath damage while respecting `moddedDragonBreathDamageCap`;
- Gorgon cannot permanently replace a target and respects mutual gaze/blindfold protection;
- Siren song respects ear protection, sneaking, allies, friendly-fire policy, and walls/range;
- a player holding a ritual sacrifice cancels by walking more than six blocks away, recruited Underzealots clear their prayer state, and a hungry player joining prayer consumes hunger;
- Gloomoth converts to Watcher, Vesper converts to Forsaken, and the resulting player-owned minion defends its owner;
- Nucleeper honors `mobGriefing=false`, Dread Lich honors `enableModdedMorphSummons=false`, Gingerbread Man honors the inventory-interaction gate, and no safe approximation leaves helper entities or changed blocks behind.
