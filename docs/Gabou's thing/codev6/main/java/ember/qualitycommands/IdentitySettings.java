package ember.qualitycommands;

import java.util.ArrayList;
import java.util.List;

public class IdentitySettings {
    //@Comment(value = "Whether an overlay message appears above the hotbar when a new identity is unlocked.")
    public static boolean overlayIdentityUnlocks = true;

    //@Comment(value = "Whether an overlay message appears above the hotbar when a new identity is revoked.")
    public static boolean overlayIdentityRevokes = true;

    //@Comment(value = "Whether a player's equipped identity is revoked on death.")
    public static boolean revokeIdentityOnDeath = false;

    //@Comment(value = "Whether identities equip the items (swords, items, tools) held by the underlying player.")
    public static boolean identitiesEquipItems = true;

    //@Comment(value = "Whether identities equip the armor (chestplate, leggings, elytra) worn by the underlying player.")
    public static boolean identitiesEquipArmor = true;

    //@Comment(value = "Whether hostile mobs ignore players with hostile mob identities.")
    public static boolean hostilesIgnoreHostileIdentityPlayer = true;

    //@Comment(value = "Whether a hostile mob will stop targeting you after switching to a hostile mob identity.")
    public static boolean hostilesForgetNewHostileIdentityPlayer = false;

    //@Comment(value = "Whether Wolves will attack Players with an identity that the Wolf would normally hunt (Sheep, Fox, Skeleton).")
    public static boolean wolvesAttackIdentityPrey = true;

    //@Comment(value = "Whether owned Wolves will attack Players with an identity that the Wolf would normally hunt (Sheep, Fox, Skeleton).")
    public static boolean ownedWolvesAttackIdentityPrey = false;

    //@Comment(value = "Whether Villagers will run from Players morphed as identities villagers normally run from (Zombies).")
    public static boolean villagersRunFromIdentities = true;

    //@Comment(value = "Whether Foxes will attack Players with an identity that the Fox would normally hunt (Fish, Chicken).")
    public static boolean foxesAttackIdentityPrey = true;

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

    //@Comment(value = "List of player names allowed to swap identities when swaps are disabled.")
    public static List<String> allowedSwappers = new ArrayList<>();

    //@Comment(value = "In blocks, how far can the Enderman ability teleport?")
    public static int endermanAbilityTeleportDistance = 32;

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

    //@Comment(value = "If true, the player has to kill a certain number of entities before unlocking an Identity.")
    public static boolean killForIdentity = false;

    //@Comment(value = "Number of kills required to unlock an Identity if killsForIdentity is true.")
    public static int requiredKillsForIdentity = 50;

    //@Comment(value = "If true, players with the Warden Identity will have a shorter view range with the darkness effect.")
    public static boolean wardenIsBlinded = true;

    //@Comment(value = "If true, players with the Warden Identity will blind other nearby players.")
    public static boolean wardenBlindsNearby = true;

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
}
