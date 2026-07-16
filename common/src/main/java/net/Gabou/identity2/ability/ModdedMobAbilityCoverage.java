package net.Gabou.identity2.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;

/**
 * Validates the Naturalist and Alex's Mobs entity/ability inventories without
 * linking to either optional mod. Call this only after the synced ability
 * registry has been created and populated.
 */
public final class ModdedMobAbilityCoverage {
    public static final String NATURALIST_NAMESPACE = "naturalist";
    public static final String ALEXS_MOBS_NAMESPACE = "alexsmobs";

    public static final int MAX_COOLDOWN_TICKS = 20 * 60 * 60;
    public static final int MAX_USE_DURATION_TICKS = 20 * 60;
    public static final double MIN_STRENGTH = 0.0D;
    public static final double MAX_STRENGTH = 10.0D;
    public static final double MIN_RANGE = 0.5D;
    public static final double MAX_RANGE = 24.0D;
    public static final int MIN_DURATION_TICKS = 1;
    public static final int MAX_DURATION_TICKS = 20 * 60;

    public static final Set<ResourceLocation> NATURALIST_ROOTS = resourceIds(
            NATURALIST_NAMESPACE,
            "alligator",
            "bass",
            "bear",
            "bluejay",
            "boar",
            "butterfly",
            "canary",
            "cardinal",
            "caterpillar",
            "catfish",
            "coral_snake",
            "deer",
            "dragonfly",
            "duck",
            "elephant",
            "firefly",
            "finch",
            "giraffe",
            "hippo",
            "lion",
            "lizard",
            "moose",
            "rattlesnake",
            "rhino",
            "robin",
            "snail",
            "snake",
            "sparrow",
            "tortoise",
            "vulture",
            "zebra"
    );

    public static final Set<ResourceLocation> NATURALIST_HELPER_EXCLUSIONS = resourceIds(
            NATURALIST_NAMESPACE,
            "duck_egg",
            "lizard_tail"
    );

    public static final Set<ResourceLocation> ALEXS_MOBS_ROOTS = resourceIds(
            ALEXS_MOBS_NAMESPACE,
            "grizzly_bear",
            "roadrunner",
            "bone_serpent",
            "gazelle",
            "crocodile",
            "fly",
            "hummingbird",
            "orca",
            "sunbird",
            "gorilla",
            "crimson_mosquito",
            "rattlesnake",
            "endergrade",
            "hammerhead_shark",
            "lobster",
            "komodo_dragon",
            "capuchin_monkey",
            "centipede_head",
            "warped_toad",
            "moose",
            "mimicube",
            "raccoon",
            "blobfish",
            "seal",
            "cockroach",
            "shoebill",
            "elephant",
            "soul_vulture",
            "snow_leopard",
            "spectre",
            "crow",
            "alligator_snapping_turtle",
            "mungus",
            "mantis_shrimp",
            "guster",
            "warped_mosco",
            "straddler",
            "stradpole",
            "emu",
            "platypus",
            "dropbear",
            "tasmanian_devil",
            "kangaroo",
            "cachalot_whale",
            "leafcutter_ant",
            "enderiophage",
            "bald_eagle",
            "tiger",
            "tarantula_hawk",
            "void_worm",
            "frilled_shark",
            "mimic_octopus",
            "seagull",
            "froststalker",
            "tusklin",
            "laviathan",
            "cosmaw",
            "toucan",
            "maned_wolf",
            "anaconda",
            "anteater",
            "rocky_roller",
            "flutter",
            "gelada_monkey",
            "jerboa",
            "terrapin",
            "comb_jelly",
            "cosmic_cod",
            "bunfungus",
            "bison",
            "giant_squid",
            "sea_bear",
            "devils_hole_pupfish",
            "catfish",
            "flying_fish",
            "skelewag",
            "rain_frog",
            "potoo",
            "mudskipper",
            "rhinoceros",
            "sugar_glider",
            "farseer",
            "skreecher",
            "underminer",
            "murmur",
            "skunk",
            "banana_slug",
            "blue_jay",
            "caiman",
            "triops"
    );

    public static final Set<ResourceLocation> ALEXS_MOBS_HELPER_EXCLUSIONS = resourceIds(
            ALEXS_MOBS_NAMESPACE,
            "bone_serpent_part",
            "mosquito_spit",
            "shark_tooth_arrow",
            "tossed_item",
            "centipede_body",
            "centipede_tail",
            "cockroach_egg",
            "sand_shot",
            "gust",
            "hemolymph",
            "straddleboard",
            "emu_egg",
            "cachalot_echo",
            "enderiophage_rocket",
            "void_worm_part",
            "void_worm_shot",
            "void_portal",
            "ice_shard",
            "anaconda_part",
            "vine_lasso",
            "pollen_ball",
            "squid_grapple",
            "mud_ball",
            "murmur_head",
            "tendon_segment",
            "fart"
    );

    public static final Set<ResourceLocation> ALL_ROOTS = union(NATURALIST_ROOTS, ALEXS_MOBS_ROOTS);
    public static final Set<ResourceLocation> ALL_HELPER_EXCLUSIONS = union(
            NATURALIST_HELPER_EXCLUSIONS,
            ALEXS_MOBS_HELPER_EXCLUSIONS
    );

    public static final Set<String> KNOWN_ACTIONS = strings(
            "none",
            "dash",
            "air_dash",
            "upward_burst",
            "leap",
            "glide_launch",
            "pounce",
            "down_pounce",
            "water_dash",
            "lava_dash",
            "charge",
            "roll",
            "spin",
            "swipe",
            "bite",
            "kick",
            "punch",
            "bill_strike",
            "venom_spur",
            "poison_bite",
            "poison_sting",
            "frost_bite",
            "fire_strike",
            "solar_flare",
            "pull",
            "tongue_pull",
            "tentacle_pull",
            "jaw_pull",
            "constrict",
            "leech",
            "soul_leech",
            "knockback",
            "gust",
            "shell_guard",
            "cocoon_guard",
            "spore_cloud",
            "ink_cloud",
            "stink_cloud",
            "mucus_trail",
            "mud_shot",
            "void_shot",
            "hemolymph_shot",
            "sonar",
            "ore_sense",
            "camouflage",
            "crop_growth",
            "tail_decoy",
            "burrow_escape",
            "dance",
            "water_exit_leap",
            "inventory_drop"
    );

    public static final Set<String> KNOWN_TRAITS = strings(
            "climb",
            "fall_resistant",
            "fast_land",
            "fast_swim",
            "fire_immune",
            "flight",
            "freeze_immune",
            "high_jump",
            "knockback_resistant",
            "lava_mobility",
            "slow_fall",
            "water_breathing",
            "water_only"
    );

    private static final List<NamespaceInventory> INVENTORIES = List.of(
            new NamespaceInventory(NATURALIST_NAMESPACE, NATURALIST_ROOTS, NATURALIST_HELPER_EXCLUSIONS),
            new NamespaceInventory(ALEXS_MOBS_NAMESPACE, ALEXS_MOBS_ROOTS, ALEXS_MOBS_HELPER_EXCLUSIONS)
    );
    private static final Comparator<ResourceLocation> ID_ORDER = Comparator.comparing(ResourceLocation::toString);

    private ModdedMobAbilityCoverage() {
    }

    /** Performs validation without producing log output. */
    public static CoverageReport validate(
            Registry<EntityType<?>> entityRegistry,
            Registry<IdentityAbilityDefinition> abilityRegistry
    ) {
        Objects.requireNonNull(entityRegistry, "entityRegistry");
        Objects.requireNonNull(abilityRegistry, "abilityRegistry");

        Set<ResourceLocation> entityIds = supportedIds(entityRegistry.keySet());
        Set<ResourceLocation> abilityIds = supportedIds(abilityRegistry.keySet());
        Set<String> presentNamespaces = new LinkedHashSet<>();
        Set<ResourceLocation> missingDefinitions = sortedIds();
        Set<ResourceLocation> obsoleteDefinitions = sortedIds();
        Set<ResourceLocation> unclassifiedEntities = sortedIds();
        Set<ResourceLocation> missingExpectedEntities = sortedIds();

        for (NamespaceInventory inventory : INVENTORIES) {
            Set<ResourceLocation> namespaceEntities = idsInNamespace(entityIds, inventory.namespace());
            Set<ResourceLocation> namespaceAbilities = idsInNamespace(abilityIds, inventory.namespace());
            boolean present = !namespaceEntities.isEmpty();
            addDifference(missingDefinitions, inventory.roots(), namespaceAbilities);
            if (present) {
                presentNamespaces.add(inventory.namespace());
                addDifference(missingExpectedEntities, inventory.roots(), namespaceEntities);
            }

            for (ResourceLocation entityId : namespaceEntities) {
                if (!inventory.roots().contains(entityId) && !inventory.helperExclusions().contains(entityId)) {
                    unclassifiedEntities.add(entityId);
                }
            }
            for (ResourceLocation abilityId : namespaceAbilities) {
                if (!inventory.roots().contains(abilityId)) {
                    obsoleteDefinitions.add(abilityId);
                }
            }
        }

        Map<ResourceLocation, List<String>> invalidDefinitions = new LinkedHashMap<>();
        for (ResourceLocation abilityId : abilityIds.stream().sorted(ID_ORDER).toList()) {
            IdentityAbilityDefinition definition = abilityRegistry.get(abilityId);
            List<String> problems = validateDefinition(definition);
            if (!problems.isEmpty()) {
                invalidDefinitions.put(abilityId, problems);
            }
        }

        return new CoverageReport(
                presentNamespaces,
                missingDefinitions,
                obsoleteDefinitions,
                unclassifiedEntities,
                missingExpectedEntities,
                invalidDefinitions,
                abilityIds.size()
        );
    }

    /** Performs validation and writes a concise result through Identity2's logger. */
    public static CoverageReport validateAndLog(
            Registry<EntityType<?>> entityRegistry,
            Registry<IdentityAbilityDefinition> abilityRegistry
    ) {
        return validateAndLog(entityRegistry, abilityRegistry, Identity2.LOGGER);
    }

    /** Performs validation and writes a concise result through the supplied logger. */
    public static CoverageReport validateAndLog(
            Registry<EntityType<?>> entityRegistry,
            Registry<IdentityAbilityDefinition> abilityRegistry,
            Logger logger
    ) {
        CoverageReport report = validate(entityRegistry, abilityRegistry);
        log(report, logger);
        return report;
    }

    /** Logs an existing report. */
    public static void log(CoverageReport report, Logger logger) {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(logger, "logger");

        String namespaces = report.presentNamespaces().isEmpty()
                ? "none"
                : String.join(",", report.presentNamespaces());
        if (report.isClean()) {
            logger.info(
                    "Modded mob ability coverage is clean: loaded={}, definitions={}/{}.",
                    namespaces,
                    report.checkedDefinitionCount(),
                    ALL_ROOTS.size()
            );
            return;
        }

        logger.warn(
                "Modded mob ability coverage found {} problem(s): loaded={}, definitions={}, missing={}, obsolete={}, unclassified={}, missing_entities={}, invalid={}.",
                report.problemCount(),
                namespaces,
                report.checkedDefinitionCount(),
                report.missingDefinitions().size(),
                report.obsoleteDefinitions().size(),
                report.unclassifiedEntities().size(),
                report.missingExpectedEntities().size(),
                report.invalidDefinitions().size()
        );
        logIds(logger, "Missing ability definitions", report.missingDefinitions());
        logIds(logger, "Obsolete ability definitions", report.obsoleteDefinitions());
        logIds(logger, "Unclassified namespace entities", report.unclassifiedEntities());
        logIds(logger, "Expected root entities absent from loaded registry", report.missingExpectedEntities());
        if (!report.invalidDefinitions().isEmpty()) {
            logger.warn("Invalid modded ability definitions: {}", formatDefinitionProblems(report.invalidDefinitions(), 12));
        }
    }

    /** Returns immutable validation messages for one ability definition. */
    public static List<String> validateDefinition(IdentityAbilityDefinition definition) {
        List<String> problems = new ArrayList<>();
        if (definition == null) {
            problems.add("registry value is null");
            return List.copyOf(problems);
        }
        if (definition.icon() == null) {
            problems.add("icon is missing");
        }
        if (definition.cooldown() < 0 || definition.cooldown() > MAX_COOLDOWN_TICKS) {
            problems.add("cooldown must be 0.." + MAX_COOLDOWN_TICKS);
        }
        if (definition.useduration() < 0 || definition.useduration() > MAX_USE_DURATION_TICKS) {
            problems.add("use_duration must be 0.." + MAX_USE_DURATION_TICKS);
        }
        if (!Double.isFinite(definition.strength())
                || definition.strength() < MIN_STRENGTH
                || definition.strength() > MAX_STRENGTH) {
            problems.add("strength must be finite and " + MIN_STRENGTH + ".." + MAX_STRENGTH);
        }
        if (!Double.isFinite(definition.range())
                || definition.range() < MIN_RANGE
                || definition.range() > MAX_RANGE) {
            problems.add("range must be finite and " + MIN_RANGE + ".." + MAX_RANGE);
        }
        if (definition.duration() < MIN_DURATION_TICKS || definition.duration() > MAX_DURATION_TICKS) {
            problems.add("duration must be " + MIN_DURATION_TICKS + ".." + MAX_DURATION_TICKS);
        }

        String action = normalize(definition.action());
        boolean hasLegacyAction = hasResourceLocation(definition.bultinability())
                || (definition.command() != null && !definition.command().isBlank());
        if (action.isEmpty()) {
            if (!hasLegacyAction) {
                problems.add("action is blank and no predef/command fallback exists");
            }
        } else if (!KNOWN_ACTIONS.contains(action)) {
            problems.add("unknown action '" + definition.action() + "'");
        }

        Set<String> seenTraits = new LinkedHashSet<>();
        List<String> traits = definition.traits();
        if (traits == null) {
            problems.add("traits list is null");
        } else {
            for (String rawTrait : traits) {
                String trait = normalize(rawTrait);
                if (trait.isEmpty()) {
                    problems.add("trait is blank");
                } else if (!KNOWN_TRAITS.contains(trait)) {
                    problems.add("unknown trait '" + rawTrait + "'");
                } else if (!Objects.equals(rawTrait, rawTrait.trim())) {
                    problems.add("trait has surrounding whitespace '" + rawTrait + "'");
                }
                if (!trait.isEmpty() && !seenTraits.add(trait)) {
                    problems.add("duplicate trait '" + rawTrait + "'");
                }
            }
        }
        return List.copyOf(problems);
    }

    private static boolean hasResourceLocation(ResourceLocation id) {
        return id != null && !"null".equals(id.getPath());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<ResourceLocation> supportedIds(Set<ResourceLocation> ids) {
        Set<ResourceLocation> supported = sortedIds();
        for (ResourceLocation id : ids) {
            if (id != null && isSupportedNamespace(id.getNamespace())) {
                supported.add(id);
            }
        }
        return supported;
    }

    private static boolean isSupportedNamespace(String namespace) {
        return NATURALIST_NAMESPACE.equals(namespace) || ALEXS_MOBS_NAMESPACE.equals(namespace);
    }

    private static Set<ResourceLocation> idsInNamespace(Set<ResourceLocation> ids, String namespace) {
        Set<ResourceLocation> matching = sortedIds();
        for (ResourceLocation id : ids) {
            if (namespace.equals(id.getNamespace())) {
                matching.add(id);
            }
        }
        return matching;
    }

    private static void addDifference(
            Set<ResourceLocation> destination,
            Set<ResourceLocation> expected,
            Set<ResourceLocation> actual
    ) {
        for (ResourceLocation id : expected) {
            if (!actual.contains(id)) {
                destination.add(id);
            }
        }
    }

    private static Set<ResourceLocation> sortedIds() {
        return new TreeSet<>(ID_ORDER);
    }

    private static void logIds(Logger logger, String label, Set<ResourceLocation> ids) {
        if (!ids.isEmpty()) {
            logger.warn("{}: {}", label, formatIds(ids, 20));
        }
    }

    private static String formatIds(Set<ResourceLocation> ids, int limit) {
        List<String> values = ids.stream().map(ResourceLocation::toString).sorted().toList();
        if (values.size() <= limit) {
            return String.join(", ", values);
        }
        return String.join(", ", values.subList(0, limit)) + " ... +" + (values.size() - limit);
    }

    private static String formatDefinitionProblems(Map<ResourceLocation, List<String>> problems, int limit) {
        List<String> entries = problems.entrySet().stream()
                .map(entry -> entry.getKey() + " [" + String.join("; ", entry.getValue()) + "]")
                .toList();
        if (entries.size() <= limit) {
            return String.join(", ", entries);
        }
        return String.join(", ", entries.subList(0, limit)) + " ... +" + (entries.size() - limit);
    }

    private static Set<ResourceLocation> resourceIds(String namespace, String... paths) {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        for (String path : paths) {
            ResourceLocation id = new ResourceLocation(namespace, path);
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Duplicate resource location: " + id);
            }
        }
        return Collections.unmodifiableSet(ids);
    }

    @SafeVarargs
    private static Set<ResourceLocation> union(Set<ResourceLocation>... sets) {
        LinkedHashSet<ResourceLocation> values = new LinkedHashSet<>();
        for (Set<ResourceLocation> set : sets) {
            values.addAll(set);
        }
        return Collections.unmodifiableSet(values);
    }

    private static Set<String> strings(String... values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Collections.addAll(result, values);
        return Collections.unmodifiableSet(result);
    }

    private record NamespaceInventory(
            String namespace,
            Set<ResourceLocation> roots,
            Set<ResourceLocation> helperExclusions
    ) {
    }

    public record CoverageReport(
            Set<String> presentNamespaces,
            Set<ResourceLocation> missingDefinitions,
            Set<ResourceLocation> obsoleteDefinitions,
            Set<ResourceLocation> unclassifiedEntities,
            Set<ResourceLocation> missingExpectedEntities,
            Map<ResourceLocation, List<String>> invalidDefinitions,
            int checkedDefinitionCount
    ) {
        public CoverageReport {
            presentNamespaces = immutableSet(presentNamespaces);
            missingDefinitions = immutableSet(missingDefinitions);
            obsoleteDefinitions = immutableSet(obsoleteDefinitions);
            unclassifiedEntities = immutableSet(unclassifiedEntities);
            missingExpectedEntities = immutableSet(missingExpectedEntities);
            invalidDefinitions = immutableProblems(invalidDefinitions);
            if (checkedDefinitionCount < 0) {
                throw new IllegalArgumentException("checkedDefinitionCount cannot be negative");
            }
        }

        public boolean isClean() {
            return missingDefinitions.isEmpty()
                    && obsoleteDefinitions.isEmpty()
                    && unclassifiedEntities.isEmpty()
                    && missingExpectedEntities.isEmpty()
                    && invalidDefinitions.isEmpty();
        }

        public int problemCount() {
            return missingDefinitions.size()
                    + obsoleteDefinitions.size()
                    + unclassifiedEntities.size()
                    + missingExpectedEntities.size()
                    + invalidDefinitions.size();
        }

        public boolean isNamespacePresent(String namespace) {
            return namespace != null && presentNamespaces.contains(namespace);
        }

        private static <T> Set<T> immutableSet(Set<T> values) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(values, "values")));
        }

        private static Map<ResourceLocation, List<String>> immutableProblems(
                Map<ResourceLocation, List<String>> values
        ) {
            Objects.requireNonNull(values, "values");
            LinkedHashMap<ResourceLocation, List<String>> copy = new LinkedHashMap<>();
            values.forEach((id, messages) -> copy.put(
                    Objects.requireNonNull(id, "problem id"),
                    List.copyOf(Objects.requireNonNull(messages, "problem messages"))
            ));
            return Collections.unmodifiableMap(copy);
        }
    }
}
