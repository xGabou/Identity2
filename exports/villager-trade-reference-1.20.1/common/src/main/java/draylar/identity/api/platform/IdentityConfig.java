package draylar.identity.api.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public abstract class IdentityConfig {

    @ExpectPlatform
    public static IdentityConfig getInstance() {
        throw new AssertionError();
    }

    public abstract int getConfigVersion();

    public abstract boolean enableFlight();

    public abstract List<String> advancementsRequiredForFlight();

    public abstract Map<String, Integer> getAbilityCooldownMap();

    public abstract boolean requiresKillsForIdentity();

    public abstract int getRequiredKillsForIdentity();

    public abstract Map<String, Integer> getRequiredKillsByType();

    public abstract boolean shouldOverlayIdentityUnlocks();

    public abstract boolean forceChangeNew();

    public abstract boolean forceChangeAlways();

    public abstract boolean logCommands();

    public abstract boolean enableClientSwapMenu();

    public abstract boolean wolvesAttackIdentityPrey();

    public abstract boolean ownedWolvesAttackIdentityPrey();

    public abstract boolean villagersRunFromIdentities();

    public abstract boolean revokeIdentityOnDeath();

    public abstract boolean overlayIdentityRevokes();

    public abstract float flySpeed();

    public abstract boolean scalingHealth();

    public abstract int maxHealth();

    public abstract boolean identitiesEquipItems();

    public abstract boolean identitiesEquipArmor();

    public abstract boolean showPlayerNametag();

    public abstract boolean shouldRenderOwnNameTag();

    public abstract boolean foxesAttackIdentityPrey();

    public abstract boolean hostilesForgetNewHostileIdentityPlayer();

    public abstract boolean hostilesIgnoreHostileIdentityPlayer();

    public abstract boolean playAmbientSounds();

    public abstract boolean useIdentitySounds();

    public abstract boolean hearSelfAmbient();

    public abstract double endermanAbilityTeleportDistance();

    public abstract boolean enableSwaps();

    /**
     * Sets whether all players may swap identities regardless of the whitelist.
     * When set to {@code false}, only operators or whitelisted players may swap.
     */
    public abstract void setEnableSwaps(boolean enabled);

    // Whether players can trade with themselves when morphed as a villager
    public abstract boolean allowSelfTrading();

    // Toggle self-trading rule at runtime (e.g., via command)
    public abstract void setAllowSelfTrading(boolean allow);

    /**
     * Players listed here may swap identities even when {@link #enableSwaps()} is false.
     * Names are compared case-insensitively.
     */
    public abstract List<String> allowedSwappers();

    public abstract int hostilityTime();

    public abstract boolean wardenIsBlinded();

    public abstract boolean wardenBlindsNearby();

    public abstract String getForcedIdentity();
    // Allow players to add entities to aquatic detection manually
    public abstract List<String> extraAquaticEntities();

    public abstract List<String> removedAquaticEntities();

    public abstract List<String> extraFlyingEntities();

    public abstract List<String> removedFlyingEntities();

    public abstract void setOverlayIdentityUnlocks(boolean value);

    public abstract void setOverlayIdentityRevokes(boolean value);

    public abstract void setRevokeIdentityOnDeath(boolean value);

    public abstract void setIdentitiesEquipItems(boolean value);

    public abstract void setIdentitiesEquipArmor(boolean value);

    public abstract void setShowPlayerNametag(boolean value);

    public abstract void setRenderOwnNameTag(boolean value);

    public abstract void setHostilesIgnoreHostileIdentityPlayer(boolean value);

    public abstract void setHostilesForgetNewHostileIdentityPlayer(boolean value);

    public abstract void setWolvesAttackIdentityPrey(boolean value);

    public abstract void setOwnedWolvesAttackIdentityPrey(boolean value);

    public abstract void setVillagersRunFromIdentities(boolean value);

    public abstract void setFoxesAttackIdentityPrey(boolean value);

    public abstract void setUseIdentitySounds(boolean value);

    public abstract void setPlayAmbientSounds(boolean value);

    public abstract void setHearSelfAmbient(boolean value);

    public abstract void setEnableFlight(boolean value);

    public abstract void setHostilityTime(int ticks);

    public abstract void setScalingHealth(boolean value);

    public abstract void setMaxHealth(int value);

    public abstract void setEnableClientSwapMenu(boolean value);

    public abstract void setForceChangeNew(boolean value);

    public abstract void setForceChangeAlways(boolean value);

    public abstract void setLogCommands(boolean value);

    public abstract void setFlySpeed(float value);

    public abstract void setKillForIdentity(boolean value);

    public abstract void setRequiredKillsForIdentity(int value);

    public abstract void setEndermanAbilityTeleportDistance(int value);

    public abstract void setWardenIsBlinded(boolean value);

    public abstract void setWardenBlindsNearby(boolean value);

    public abstract void setForcedIdentity(@Nullable String id);
}
