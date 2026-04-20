package net.Gabou.identity2;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class IdentitySettings {
    //@Comment(value = "Whether an overlay message appears above the hotbar when a new identity is unlocked.")
    public static boolean overlayIdentityUnlocks = true;

    //@Comment(value = "Whether an overlay message appears above the hotbar when a new identity is revoked.")
    public static boolean overlayIdentityRevokes = true;

    public enum DeathMorphRule {
        NONE,
        REVOKE_ACTIVE,
        WIPE_ALL
    }

    public static DeathMorphRule deathMorphRule = DeathMorphRule.WIPE_ALL;

    public static IdentitySettings.DeathMorphRule getEffectiveDeathMorphRule(@Nullable MinecraftServer server) {
        IdentitySettings.DeathMorphRule rule = IdentitySettings.deathMorphRule;

        if (rule != IdentitySettings.DeathMorphRule.NONE) {
            return rule;
        }

        boolean cheatsEnabled = server != null && server.getWorldData().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_SENDCOMMANDFEEDBACK);
        // This gamerule is not actually "cheats enabled". If you already have a real "are cheats enabled" check in your project, use that instead.

        if (cheatsEnabled) {
            return IdentitySettings.DeathMorphRule.NONE;
        }

        return IdentitySettings.DeathMorphRule.WIPE_ALL;
    }

    //@Comment(value = "If true, unlocking an identity also unlocks all of its variants.")
    public static boolean unlockAllVariantsOnFirstUnlock = false;
    //@Comment(value = "Whether identities equip the items (swords, items, tools) held by the underlying player.")
    public static boolean identitiesEquipItems = true;

    //@Comment(value = "Whether identities equip the armor (chestplate, leggings, elytra) worn by the underlying player.")
    public static boolean identitiesEquipArmor = true;

    //@Comment(value = "Whether hostile mobs ignore players with hostile mob identities.")
    public static boolean hostilesIgnoreHostileIdentityPlayer = true;

    //@Comment(value = "Whether a hostile mob will stop targeting you after switching to a hostile mob identity.")
    public static boolean hostilesForgetNewHostileIdentityPlayer = false;

    //@Comment(value = "Whether Villagers will run from Players morphed as identities villagers normally run from (Zombies).")
    public static boolean villagersRunFromIdentities = true;

    //@Comment(value = "Whether Identity sounds take priority over Player Sounds (eg. Blaze hurt sound when hit).")
    public static boolean useIdentitySounds = true;

    //@Comment(value = "Whether disguised players should randomly emit the ambient sound of their Identity.")
    public static boolean playAmbientSounds = true;

    //@Comment(value = "Whether disguised players should hear their own ambient sounds (only if playAmbientSounds is true).")
    public static boolean hearSelfAmbient = false;

    //@Comment(value = "Whether mobs in the flying entity tag can fly.")
    public static boolean enableFlight = true;

    //@Comment(value = "How long hostility lasts for players morphed as hostile mobs (think: Pigman aggression")
    public static int hostilityTime = 20 * 15;

    //@Comment(value = "A list of Advancements required before the player can fly using an Identity.")
    public static List<String> advancementsRequiredForFlight = new ArrayList<>();

    //@Comment(value = "Whether Identities modify your max health value based on their max health value.")
    public static boolean scalingHealth = true;

    //@Comment(value = "The maximum value of scaling health. Useful for not giving players 300 HP when they turn into a wither.")
    public static int maxHealth = 40;


    //@Comment(value = "If set to false, only operators can switch identities through the ` menu. Note that this config option is synced from S2C when a client joins the game, but a client can still open the menu if they have a modified version of Identity.")
    public static boolean enableClientSwapMenu = true;

    //@Comment(value = "If set to false, only operators can switch identities. Used on the server; guaranteed to be authoritative.")
    public static boolean enableSwaps = true;
    //@Comment(value = "If true, players can use /identity_villager trade myself to trade with their own villager identity.")
    public static boolean canTradeWithHimSelf = false;

    //@Comment(value = "If true, players with an offline UUID v3 are rejected during login.")
    public static boolean authStrictOfflineUuidReject = false;

    //@Comment(value = "If true, players that fail auth are kicked instead of being marked suspicious.")
    public static boolean authKickOnFailure = false;

    //@Comment(value = "How many ticks the auth challenge may remain pending before the player is marked suspicious.")
    public static int authChallengeTimeoutTicks = 200;

    //@Comment(value = "Cooldown multiplier applied to suspicious players when they try to use protected abilities.")
    public static float authCooldownMultiplier = 2.0f;

    //@Comment(value = "Chance for a suspicious player to have a protected ability use fail entirely.")
    public static double authAbilityFailureChance = 0.35D;

    //@Comment(value = "List of player names allowed to swap identities when swaps are disabled.")
    public static List<String> allowedSwappers = new ArrayList<>();

    //@Comment(value = "In blocks, how far can the Enderman ability teleport?")
    public static int endermanAbilityTeleportDistance = 1024;

    //@Comment(value = "If true, Giants gain zombie-like AI and can appear naturally in Hard difficulty.")
    public static boolean enableGiantZombieAiAndHardSpawns = false;

    //@Comment(value = "Should player nametags render above players disguised with an identity? Note that the server is the authority for this config option.")
    public static boolean showPlayerNametag = false;

    //@Comment(value = "If true, a player with an active Identity can see their own nametag in third person.")
    public static boolean renderOwnNametag = false;

    //@Comment(value = "If true, players that gain a NEW Identity will be forcibly changed into it on kill.")
    public static boolean forceChangeNew = false;

    //@Comment(value = "If true, players will be forcibly changed into any entity they kill. The above option, forceChangeNew, only applies to new unlocks.")
    public static boolean forceChangeAlways = false;

    //@Comment(value = "If true, /identity commands will send feedback in the action bar.")
    public static boolean logCommands = true;

    public static float flySpeed = 0.05f;

    //@Comment(value = "If true, killing entities unlocks their identities.")
    public static boolean enableIdentityKillUnlocks = true;

    //@Comment(value = "Kills required to unlock an identity.")
    public static int identityKillsRequired = 1;

    //@Comment(value = "Morph transition duration in ticks for dynamic model blending.")
    public static int morphTransitionTicks = 30;
    //@Comment(value = "If true, morph transitions spawn swirl particles. Model blending remains active either way.")
    public static boolean enableMorphTransitionParticles = true;
    //@Comment(value = "If true, unlocked morph acquisition plays tendril-style particles from target to player.")
    public static boolean enableMorphAcquisitionTendrils = true;
    //@Comment(value = "Duration in ticks of the morph acquisition tendril animation.")
    public static int morphAcquisitionAnimationTicks = 26;

    //@Comment(value = "If true, /identity morph only works for unlocked identities unless you are an operator.")
    public static boolean requireUnlockedIdentityForMorph = true;

    //@Comment(value = "If true, morphing consumes charges and morph kills grant charges.")
    public static boolean enableMorphChargeSystem = true;
    //@Comment(value = "Default number of charges gained per qualifying kill.")
    public static int defaultChargeGainPerKill = 1;

    //@Comment(value = "Charge cost paid when selecting a morph.")
    public static int defaultMorphUseChargeCost = 1;

    //@Comment(value = "If true, morphing does not require charges. Charges can still be used for death penalties and jar storage.")
    public static boolean dontRequireChargeForMorphing = true;

    //@Comment(value = "If true, creative players bypass morph charge checks and penalties.")
    public static boolean bypassMorphChargesForCreativePlayers = true;

    //@Comment(value = "If true, operators bypass morph charge checks and penalties.")
    public static boolean bypassMorphChargesForOperators = true;

    //@Comment(value = "If true, dying while morphed removes charges from the active morph.")
    public static boolean removeMorphChargeOnDeath = true;

    //@Comment(value = "How many charges are removed on death when removeMorphChargeOnDeath is true.")
    public static int morphDeathChargePenalty = 1;

    //@Comment(value = "Per-identity charge gain overrides in the format 'namespace:entity=value'.")
    public static List<String> chargeGainByIdentity = new ArrayList<>();

    //@Comment(value = "Per-identity charge cost overrides in the format 'namespace:entity=value'.")
    public static List<String> chargeCostByIdentity = new ArrayList<>();

    //@Comment(value = "Per-identity death charge penalty overrides in the format 'namespace:entity=value'.")
    public static List<String> chargeDeathPenaltyByIdentity = new ArrayList<>();

    //@Comment(value = "If true, soul jar storage/protection is enabled.")
    public static boolean enableSoulJarSystem = false;
    //@Comment(value = "Per-tier soul jar capacities in the format 'tier=value'.")
    public static List<String> soulJarTierCapacities = new ArrayList<>(
        List.of("mud=5", "glass=10", "reinforced=15", "true_soul=24")
    );

    //@Comment(value = "Per-tier soul jar item mapping in the format 'tier=namespace:item'.")
    public static List<String> soulJarTierItems = new ArrayList<>(
        List.of(
            "mud=identity2:mud_jar",
            "glass=identity2:glass_jar",
            "reinforced=identity2:reinforced_jar",
            "true_soul=identity2:endgame_jar"
        )
    );

    //@Comment(value = "Optional per-tier recipe id mapping in the format 'tier=namespace:recipe'.")
    public static List<String> soulJarTierRecipes = new ArrayList<>();

    //@Comment(value = "Tier key used for the endgame true soul jar.")
    public static String trueSoulJarTier = "true_soul";

    //@Comment(value = "If true, morphs stored in true soul jars are treated as permanent.")
    public static boolean trueSoulJarGrantsPermanent = true;

    //@Comment(value = "If true, morphs stored in true soul jars ignore charge death penalties.")
    public static boolean trueSoulJarPreventsDeathPenalty = true;

    //@Comment(value = "If true, soul jars can randomly drop into the world on mob death.")
    public static boolean enableRandomSoulJarWorldDrops = true;

    //@Comment(value = "Chance per non-player mob death to drop a random soul jar. Range: 0.0 to 1.0.")
    public static double soulJarWorldDropChance = 0.005D;

    //@Comment(value = "Weighted random jar tiers for world drops in the format 'tier=weight'.")
    public static List<String> soulJarWorldDropTierWeights = new ArrayList<>(
        List.of("mud=85", "glass=12", "reinforced=3", "true_soul=1")
    );

    //@Comment(value = "If true, stored morphs can become permanent by kill dedication.")
    public static boolean enablePermanentMorphs = false;
    //@Comment(value = "Default kills required to permanently bind a stored morph.")
    public static int defaultPermanentKillRequirement = 250;

    //@Comment(value = "Per-identity permanent requirement overrides in the format 'namespace:entity=value'.")
    public static List<String> permanentKillRequirementByIdentity = new ArrayList<>();

    //@Comment(value = "If true, players can absorb permanent morphs for account-bound permanence.")
    public static boolean enableSoulAbsorption = false;
    //@Comment(value = "Elder Guardian active ability radius in blocks.")
    public static double elderGuardianMiningFatigueRadius = 50.0D;

    //@Comment(value = "Elder Guardian active ability Mining Fatigue duration in ticks.")
    public static int elderGuardianMiningFatigueDurationTicks = 20 * 60;

    //@Comment(value = "Elder Guardian active ability Mining Fatigue amplifier (0-based).")
    public static int elderGuardianMiningFatigueAmplifier = 2;

    //@Comment(value = "Elder Guardian active ability cooldown in ticks. Set to 0 to use datapack cooldown.")
    public static int elderGuardianMiningFatigueCooldownTicks = 600;

    //@Comment(value = "Shulker secondary teleport cooldown in ticks.")
    public static int shulkerTeleportCooldownTicks = 80;

    //@Comment(value = "If true, players with the Warden Identity will have a shorter view range with the darkness effect.")
    public static boolean wardenIsBlinded = true;

    //@Comment(value = "If true, players with the Warden Identity will blind other nearby players.")
    public static boolean wardenBlindsNearby = true;

    //@Comment(value = "If true, each player is morphed into a random unlocked identity once per Minecraft day.")
    public static boolean randomMorphEveryDay = false;

    //@Comment(value = "The Identity type that is forced on all players")
    public static String forcedIdentity = null;
    //@Comment(value = "List of additional entities considered aquatic even if not tagged.")
    public static List<String> extraAquaticEntities = new ArrayList<>();

    //@Comment(value = "List of entities to forcibly exclude from being considered aquatic.")
    public static List<String> removedAquaticEntities = new ArrayList<>();

    //@Comment(value = "List of entities to forcibly exclude from being considered flying.")
    public static List<String> removedFlyingEntities = new ArrayList<>();
    //@Comment(value = "List of entities to forcibly include as flying.")
    public static List<String> extraFlyingEntities = new ArrayList<>();

    //@Comment(value = "Runtime tag additions in the format 'namespace:tag=namespace:entity'. Example: 'identity2:slow_falling=yourmod:blue_jay'")
    public static List<String> extraEntityTypeTagAssignments = new ArrayList<>();

    //@Comment(value = "Runtime tag removals in the format 'namespace:tag=namespace:entity'.")
    public static List<String> removedEntityTypeTagAssignments = new ArrayList<>();
}
