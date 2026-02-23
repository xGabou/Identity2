package draylar.identity.fabric.config;

import draylar.identity.api.platform.IdentityConfig;
import draylar.identity.fabric.IdentityFabric;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Config(name = "identity")
public class IdentityFabricConfig extends IdentityConfig implements ConfigData {


    @Comment(value = "Whether an overlay message appears above the hotbar when a new identity is unlocked.")
    public boolean overlayIdentityUnlocks = true;

    @Comment(value = "Whether an overlay message appears above the hotbar when a new identity is revoked.")
    public boolean overlayIdentityRevokes = true;

    @Comment(value = "Whether a player's equipped identity is revoked on death.")
    public boolean revokeIdentityOnDeath = false;

    @Comment(value = "Whether identities equip the items (swords, items, tools) held by the underlying player.")
    public boolean identitiesEquipItems = true;

    @Comment(value = "Whether identities equip the armor (chestplate, leggings, elytra) worn by the underlying player.")
    public boolean identitiesEquipArmor = true;

    @Comment(value = "Whether hostile mobs ignore players with hostile mob identities.")
    public boolean hostilesIgnoreHostileIdentityPlayer = true;

    @Comment(value = "Whether a hostile mob will stop targeting you after switching to a hostile mob identity.")
    public boolean hostilesForgetNewHostileIdentityPlayer = false;

    @Comment(value = "Whether Wolves will attack Players with an identity that the Wolf would normally hunt (Sheep, Fox, Skeleton).")
    public boolean wolvesAttackIdentityPrey = true;

    @Comment(value = "Whether owned Wolves will attack Players with an identity that the Wolf would normally hunt (Sheep, Fox, Skeleton).")
    public boolean ownedWolvesAttackIdentityPrey = false;

    @Comment(value = "Whether Villagers will run from Players morphed as identities villagers normally run from (Zombies).")
    public boolean villagersRunFromIdentities = true;

    @Comment(value = "Whether Foxes will attack Players with an identity that the Fox would normally hunt (Fish, Chicken).")
    public boolean foxesAttackIdentityPrey = true;

    @Comment(value = "Whether Identity sounds take priority over Player Sounds (eg. Blaze hurt sound when hit).")
    public boolean useIdentitySounds = true;

    @Comment(value = "Whether disguised players should randomly emit the ambient sound of their Identity.")
    public boolean playAmbientSounds = true;

    @Comment(value = "Whether disguised players should hear their own ambient sounds (only if playAmbientSounds is true).")
    public boolean hearSelfAmbient = false;

    @Comment(value = "Whether mobs in the flying entity tag can fly.")
    public boolean enableFlight = true;

    @Comment(value = "How long hostility lasts for players morphed as hostile mobs (think: Pigman aggression")
    public int hostilityTime = 20 * 15;

    @Comment(value = "A list of Advancements required before the player can fly using an Identity.")
    public List<String> advancementsRequiredForFlight = new ArrayList<>();

    @Comment(value = "Whether Identities modify your max health value based on their max health value.")
    public boolean scalingHealth = true;

    @Comment(value = "The maximum value of scaling health. Useful for not giving players 300 HP when they turn into a wither.")
    public int maxHealth = 40;


    @Comment(value = "If set to false, only operators can switch identities through the ` menu. Note that this config option is synced from S2C when a client joins the game, but a client can still open the menu if they have a modified version of Identity.")
    public boolean enableClientSwapMenu = true;

    @Comment(value = "If set to false, only operators can switch identities. Used on the server; guaranteed to be authoritative.")
    public boolean enableSwaps = true;
    @Comment(value = "If true, players can use /identity_villager trade myself to trade with their own villager identity.")
    public boolean canTradeWithHimSelf = false;

    @Comment(value = "List of player names allowed to swap identities when swaps are disabled.")
    public List<String> allowedSwappers = new ArrayList<>();

    @Comment(value = "In blocks, how far can the Enderman ability teleport?")
    public int endermanAbilityTeleportDistance = 32;

    @Comment(value = "Should player nametags render above players disguised with an identity? Note that the server is the authority for this config option.")
    public boolean showPlayerNametag = false;

    @Comment(value = "If true, a player with an active Identity can see their own nametag in third person.")
    public boolean renderOwnNametag = false;

    @Comment(value = "If true, players that gain a NEW Identity will be forcibly changed into it on kill.")
    public boolean forceChangeNew = false;

    @Comment(value = "If true, players will be forcibly changed into any entity they kill. The above option, forceChangeNew, only applies to new unlocks.")
    public boolean forceChangeAlways = false;

    @Comment(value = "If true, /identity commands will send feedback in the action bar.")
    public boolean logCommands = true;

    public float flySpeed = 0.05f;

    @Comment(value = "If true, the player has to kill a certain number of entities before unlocking an Identity.")
    public boolean killForIdentity = false;

    @Comment(value = "Number of kills required to unlock an Identity if killsForIdentity is true.")
    public int requiredKillsForIdentity = 50;

    @Comment(value = "If true, players with the Warden Identity will have a shorter view range with the darkness effect.")
    public boolean wardenIsBlinded = true;

    @Comment(value = "If true, players with the Warden Identity will blind other nearby players.")
    public boolean wardenBlindsNearby = true;

    @Comment(value = "The Identity type that is forced on all players")
    public String forcedIdentity = null;
    @Comment(value = "List of additional entities considered aquatic even if not tagged.")
    public List<String> extraAquaticEntities = new ArrayList<>();

    @Comment(value = "List of entities to forcibly exclude from being considered aquatic.")
    public List<String> removedAquaticEntities = new ArrayList<>();

    @Comment(value = "List of entities to forcibly exclude from being considered flying.")
    public List<String> removedFlyingEntities = new ArrayList<>();
    @Comment(value = "List of entities to forcibly include as flying.")
    public List<String> extraFlyingEntities = new ArrayList<>();

    @Override
    public List<String> extraAquaticEntities() {
        return extraAquaticEntities;
    }

    @Override
    public List<String> removedAquaticEntities() {
        return removedAquaticEntities;
    }

    @Override
    public List<String> extraFlyingEntities() {
        return extraFlyingEntities;
    }

    @Override
    public List<String> removedFlyingEntities() {
        return removedFlyingEntities;
    }


    @Comment(value = "An override map for requiredKillsForIdentity for specific entity types.")
    public Map<String, Integer> requiredKillsByType = new HashMap<>() {
        {
            put("minecraft:ender_dragon", 1);
            put("minecraft:elder_guardian", 1);
            put("minecraft:wither", 1);
        }
    };

    public Map<String, Integer> abilityCooldownMap = new HashMap<>() {
        {
            put("minecraft:ghast", 60);
            put("minecraft:blaze", 20);
            put("minecraft:ender_dragon", 20);
            put("minecraft:enderman", 100);
            put("minecraft:creeper", 100);
            put("minecraft:wither", 200);
            put("minecraft:snow_golem", 10);
            put("minecraft:witch", 200);
            put("minecraft:evoker", 10);
        }
    };

    @Override
    public int getConfigVersion() {
        return IdentityFabric.CONFIG_VERSION;
    }

    @Override
    public boolean enableFlight() {
        return enableFlight;
    }

    @Override
    public List<String> advancementsRequiredForFlight() {
        return advancementsRequiredForFlight;
    }

    @Override
    public Map<String, Integer> getAbilityCooldownMap() {
        return abilityCooldownMap;
    }

    @Override
    public boolean requiresKillsForIdentity() {
        return killForIdentity;
    }

    @Override
    public int getRequiredKillsForIdentity() {
        return requiredKillsForIdentity;
    }

    @Override
    public Map<String, Integer> getRequiredKillsByType() {
        return requiredKillsByType;
    }

    @Override
    public boolean shouldOverlayIdentityUnlocks() {
        return overlayIdentityUnlocks;
    }

    @Override
    public boolean forceChangeNew() {
        return forceChangeNew;
    }

    @Override
    public boolean forceChangeAlways() {
        return forceChangeAlways;
    }

    @Override
    public boolean logCommands() {
        return logCommands;
    }

    @Override
    public boolean enableClientSwapMenu() {
        return enableClientSwapMenu;
    }

    @Override
    public boolean wolvesAttackIdentityPrey() {
        return wolvesAttackIdentityPrey;
    }

    @Override
    public boolean ownedWolvesAttackIdentityPrey() {
        return ownedWolvesAttackIdentityPrey;
    }

    @Override
    public boolean villagersRunFromIdentities() {
        return villagersRunFromIdentities;
    }

    @Override
    public boolean revokeIdentityOnDeath() {
        return revokeIdentityOnDeath;
    }

    @Override
    public boolean overlayIdentityRevokes() {
        return overlayIdentityRevokes;
    }

    @Override
    public float flySpeed() {
        return flySpeed;
    }

    @Override
    public boolean scalingHealth() {
        return scalingHealth;
    }

    @Override
    public int maxHealth() {
        return maxHealth;
    }

    @Override
    public boolean identitiesEquipItems() {
        return identitiesEquipItems;
    }

    @Override
    public boolean identitiesEquipArmor() {
        return identitiesEquipArmor;
    }

    @Override
    public boolean showPlayerNametag() {
        return showPlayerNametag;
    }

    @Override
    public boolean shouldRenderOwnNameTag() {
        return renderOwnNametag;
    }

    @Override
    public boolean foxesAttackIdentityPrey() {
        return foxesAttackIdentityPrey;
    }

    @Override
    public boolean hostilesForgetNewHostileIdentityPlayer() {
        return hostilesForgetNewHostileIdentityPlayer;
    }

    @Override
    public boolean hostilesIgnoreHostileIdentityPlayer() {
        return hostilesIgnoreHostileIdentityPlayer;
    }

    @Override
    public boolean playAmbientSounds() {
        return playAmbientSounds;
    }

    @Override
    public boolean useIdentitySounds() {
        return useIdentitySounds;
    }

    @Override
    public boolean hearSelfAmbient() {
        return hearSelfAmbient;
    }

    @Override
    public double endermanAbilityTeleportDistance() {
        return endermanAbilityTeleportDistance;
    }

    @Override
    public boolean enableSwaps() {
        return enableSwaps;
    }

    @Override
    public void setEnableSwaps(boolean enabled) {
        this.enableSwaps = enabled;
    }

    @Override
    public boolean allowSelfTrading() {
        return canTradeWithHimSelf;
    }

    @Override
    public void setAllowSelfTrading(boolean allow) {
        this.canTradeWithHimSelf = allow;
    }

    @Override
    public List<String> allowedSwappers() {
        return allowedSwappers;
    }

    @Override
    public int hostilityTime() {
        return hostilityTime;
    }

    @Override
    public boolean wardenIsBlinded() {
        return wardenIsBlinded;
    }

    @Override
    public boolean wardenBlindsNearby() {
        return wardenBlindsNearby;
    }

    @Override
    public String getForcedIdentity() {
        return null;
    }

    @Override
    public void setOverlayIdentityUnlocks(boolean value) {
        overlayIdentityUnlocks = value;
    }

    @Override
    public void setOverlayIdentityRevokes(boolean value) {
        overlayIdentityRevokes = value;
    }

    @Override
    public void setRevokeIdentityOnDeath(boolean value) {
        revokeIdentityOnDeath = value;
    }

    @Override
    public void setIdentitiesEquipItems(boolean value) {
        identitiesEquipItems = value;
    }

    @Override
    public void setIdentitiesEquipArmor(boolean value) {
        identitiesEquipArmor = value;
    }

    @Override
    public void setShowPlayerNametag(boolean value) {
        showPlayerNametag = value;
    }

    @Override
    public void setRenderOwnNameTag(boolean value) {
        renderOwnNametag = value;
    }

    @Override
    public void setHostilesIgnoreHostileIdentityPlayer(boolean value) {
        hostilesIgnoreHostileIdentityPlayer = value;
    }

    @Override
    public void setHostilesForgetNewHostileIdentityPlayer(boolean value) {
        hostilesForgetNewHostileIdentityPlayer = value;
    }

    @Override
    public void setWolvesAttackIdentityPrey(boolean value) {
        wolvesAttackIdentityPrey = value;
    }

    @Override
    public void setOwnedWolvesAttackIdentityPrey(boolean value) {
        ownedWolvesAttackIdentityPrey = value;
    }

    @Override
    public void setVillagersRunFromIdentities(boolean value) {
        villagersRunFromIdentities = value;
    }

    @Override
    public void setFoxesAttackIdentityPrey(boolean value) {
        foxesAttackIdentityPrey = value;
    }

    @Override
    public void setUseIdentitySounds(boolean value) {
        useIdentitySounds = value;
    }

    @Override
    public void setPlayAmbientSounds(boolean value) {
        playAmbientSounds = value;
    }

    @Override
    public void setHearSelfAmbient(boolean value) {
        hearSelfAmbient = value;
    }

    @Override
    public void setEnableFlight(boolean value) {
        enableFlight = value;
    }

    @Override
    public void setHostilityTime(int ticks) {
        hostilityTime = ticks;
    }

    @Override
    public void setScalingHealth(boolean value) {
        scalingHealth = value;
    }

    @Override
    public void setMaxHealth(int value) {
        maxHealth = value;
    }

    @Override
    public void setEnableClientSwapMenu(boolean value) {
        enableClientSwapMenu = value;
    }

    @Override
    public void setForceChangeNew(boolean value) {
        forceChangeNew = value;
    }

    @Override
    public void setForceChangeAlways(boolean value) {
        forceChangeAlways = value;
    }

    @Override
    public void setLogCommands(boolean value) {
        logCommands = value;
    }

    @Override
    public void setFlySpeed(float value) {
        flySpeed = value;
    }

    @Override
    public void setKillForIdentity(boolean value) {
        killForIdentity = value;
    }

    @Override
    public void setRequiredKillsForIdentity(int value) {
        requiredKillsForIdentity = value;
    }

    @Override
    public void setEndermanAbilityTeleportDistance(int value) {
        endermanAbilityTeleportDistance = value;
    }

    @Override
    public void setWardenIsBlinded(boolean value) {
        wardenIsBlinded = value;
    }

    @Override
    public void setWardenBlindsNearby(boolean value) {
        wardenBlindsNearby = value;
    }

    @Override
    public void setForcedIdentity(@Nullable String id) {
        forcedIdentity = id;
    }
}
