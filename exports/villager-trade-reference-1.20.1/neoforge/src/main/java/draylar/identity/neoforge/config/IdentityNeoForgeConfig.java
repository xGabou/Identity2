package draylar.identity.neoforge.config;


import draylar.identity.neoforge.IdentityNeoForge;
import draylar.identity.api.platform.IdentityConfig;
import draylar.identity.registry.IdentityEntityTags;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentityNeoForgeConfig extends IdentityConfig {
    private  List<String> extraAquaticEntities = new ArrayList<>();
    private  List<String> removedAquaticEntities = new ArrayList<>();
    private  List<String> extraFlyingEntities = new ArrayList<>();
    private  List<String> removedFlyingEntities = new ArrayList<>();


    private int configVersion = IdentityNeoForge.CONFIG_VERSION;

    public boolean overlayIdentityUnlocks = true;
    public boolean overlayIdentityRevokes = true;
    public boolean revokeIdentityOnDeath = false;
    public boolean identitiesEquipItems = true;
    public boolean identitiesEquipArmor = true;
    public boolean renderOwnNameTag = false;
    public boolean hostilesIgnoreHostileIdentityPlayer = true;
    public boolean hostilesForgetNewHostileIdentityPlayer = false;
    public boolean wolvesAttackIdentityPrey = true;
    public boolean ownedWolvesAttackIdentityPrey = false;
    public boolean villagersRunFromIdentities = true;
    public boolean foxesAttackIdentityPrey = true;
    public boolean useIdentitySounds = true;
    public boolean playAmbientSounds = true;
    public boolean hearSelfAmbient = false;
    public boolean enableFlight = true;
    public int hostilityTime = 20 * 15;
    public List<String> advancementsRequiredForFlight = new ArrayList<>();
    public boolean scalingHealth = true;
    public int maxHealth = 20;
    public boolean enableClientSwapMenu = true;
    public boolean enableSwaps = true;
    public boolean allowSelfTrading = false;
    public List<String> allowedSwappers = new ArrayList<>();
    public int endermanAbilityTeleportDistance = 32;
    public boolean showPlayerNametag = false;
    public boolean forceChangeNew = false;
    public boolean forceChangeAlways = false;
    public boolean logCommands = true;
    public float flySpeed = 0.05f;
    public boolean killForIdentity = false;
    public int requiredKillsForIdentity = 10;
    public boolean wardenIsBlinded = true;
    public boolean wardenBlindsNearby = true;
    public String forcedIdentity = null;

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

    public static IdentityConfig getInstance() {
        return IdentityNeoForge.CONFIG;
    }

    @Override
    public int getConfigVersion() {
        return configVersion;
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
    public boolean shouldRenderOwnNameTag() {
        return renderOwnNameTag;
    }

    @Override
    public boolean showPlayerNametag() {
        return showPlayerNametag;
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

    public boolean allowSelfTrading() {
        return allowSelfTrading;
    }

    @Override
    public void setAllowSelfTrading(boolean allow) {
        this.allowSelfTrading = allow;
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
        return forcedIdentity;
    }



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
        renderOwnNameTag = value;
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
    public void setForcedIdentity(String id) {
        forcedIdentity = id;
    }


}
