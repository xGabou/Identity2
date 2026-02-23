package draylar.identity.mixin.player;

import dev.architectury.event.EventResult;
import draylar.identity.Identity;
import draylar.identity.api.PlayerIdentity;
import draylar.identity.api.SafeTagManager;
import draylar.identity.api.event.IdentitySwapCallback;
import draylar.identity.api.FlightHelper;
import draylar.identity.api.platform.IdentityConfig;
import draylar.identity.mixin.ServerChunkLoadingManagerAccessor;
import draylar.identity.api.variant.IdentityType;
import draylar.identity.impl.DimensionsRefresher;
import draylar.identity.impl.PlayerDataProvider;
import draylar.identity.mixin.EntityTrackerAccessor;
import draylar.identity.registry.IdentityEntityTags;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityDataMixin extends LivingEntity implements PlayerDataProvider {

    @Shadow public abstract void playSound(SoundEvent sound, float volume, float pitch);
    @Unique private static final String ABILITY_COOLDOWN_KEY = "AbilityCooldown";
    @Unique private final Set<IdentityType<?>> unlocked = new HashSet<>();
    @Unique private final Set<IdentityType<?>> favorites = new HashSet<>();
    @Unique private int remainingTime = 0;
    @Unique private int abilityCooldown = 0;
    @Unique private LivingEntity identity = null;
    @Unique private IdentityType<?> identityType = null;
    @Unique private final Map<String, NbtCompound> villagerIdentities = new HashMap<>();
    @Unique private @Nullable String activeVillagerKey = null;

    private PlayerEntityDataMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void readNbt(NbtCompound tag, CallbackInfo info) {
        unlocked.clear();

        // This tag might exist - it contains old save data for pre-variant Identities.
        // Each entry will be a string with an entity registry ID value.
        NbtList unlockedIdList = tag.getList("UnlockedMorphs", NbtElement.STRING_TYPE);
        unlockedIdList.forEach(entityRegistryID -> {
            Identifier id = Identifier.of(entityRegistryID.asString());
            if(Registries.ENTITY_TYPE.containsId(id)) {
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);

                // The variant added from the UnlockedMorphs list will default to the fallback value if needed (eg. Sheep => White)
                // This value will be re-serialize in UnlockedIdentities list, so this is 100% for old save conversions
                unlocked.add(new IdentityType(type));
            } else {
                // TODO: log reading error here
            }
        });

        // This is the new tag for saving Identity unlock information.
        // It includes metadata for variants.
        NbtList unlockedIdentityList = tag.getList("UnlockedIdentities", NbtElement.COMPOUND_TYPE);
        unlockedIdentityList.forEach(compound -> {
            IdentityType<?> type = IdentityType.from((NbtCompound) compound);
            if(type != null) {
                unlocked.add(type);
            } else {
                // TODO: log reading error here
            }
        });

        // Favorites - OLD TAG containing String IDs
        favorites.clear();
        NbtList favoriteIdList = tag.getList("FavoriteIdentities", NbtElement.STRING_TYPE);
        favoriteIdList.forEach(registryID -> {
            Identifier id = Identifier.of(registryID.asString());
            if(Registries.ENTITY_TYPE.containsId(id)) {
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);
                favorites.add(new IdentityType(type));
            }
        });

        // Favorites - NEW TAG for updated variant compound data
        NbtList favoriteTypeList = tag.getList("FavoriteIdentitiesV2", NbtElement.STRING_TYPE);
        favoriteTypeList.forEach(compound -> {
            IdentityType<?> type = IdentityType.from((NbtCompound) compound);
            if(type != null) {
                favorites.add(type);
            }
        });

        // Abilities
        abilityCooldown = tag.getInt(ABILITY_COOLDOWN_KEY);

        // Hostility
        remainingTime = tag.getInt("RemainingHostilityTime");

        // Current Identity
        readCurrentIdentity(tag.getCompound("CurrentIdentity"));
        // Villager Identities
        villagerIdentities.clear();
        NbtCompound villagerTag = tag.getCompound("VillagerIdentities");
        for(String key : villagerTag.getKeys()) {
            villagerIdentities.put(key, villagerTag.getCompound(key));
        }
        if(tag.contains("ActiveVillagerKey", NbtElement.STRING_TYPE)) {
            String storedKey = tag.getString("ActiveVillagerKey");
            activeVillagerKey = storedKey.isEmpty() ? null : storedKey;
        } else {
            activeVillagerKey = null;
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void writeNbt(NbtCompound tag, CallbackInfo info) {
        // Write 'Unlocked' Identity data
        {
            NbtList idList = new NbtList();
            unlocked.forEach(identity -> idList.add(identity.writeCompound()));

            // This was "UnlockedMorphs" in previous versions, but it has been changed with the introduction of variants.
            tag.put("UnlockedIdentities", idList);
        }

        // Favorites
        {
            NbtList idList = new NbtList();
            favorites.forEach(entityId -> idList.add(entityId.writeCompound()));
            tag.put("FavoriteIdentitiesV2", idList);
        }

        // Abilities
        tag.putInt(ABILITY_COOLDOWN_KEY, abilityCooldown);

        // Hostility
        tag.putInt("RemainingHostilityTime", remainingTime);

        // Current Identity
        tag.put("CurrentIdentity", writeCurrentIdentity(new NbtCompound()));

        // Villager Identities
        NbtCompound villagerTag = new NbtCompound();
        villagerIdentities.forEach((key, value) -> villagerTag.put(key, value.copy()));
        tag.put("VillagerIdentities", villagerTag);

        if(activeVillagerKey != null && !activeVillagerKey.isEmpty()) {
            tag.putString("ActiveVillagerKey", activeVillagerKey);
        }
    }

    @Unique
    private NbtCompound writeCurrentIdentity(NbtCompound tag) {
        NbtCompound entityTag = new NbtCompound();

        // serialize current identity data to tag if it exists
        if(identity != null) {
            identity.writeNbt(entityTag);
            if(identityType != null) {
                identityType.writeEntityNbt(entityTag);
            }
        }

        // put entity type ID under the key "id", or "minecraft:empty" if no identity is equipped (or the identity entity type is invalid)
        tag.putString("id", identity == null ? "minecraft:empty" : Registries.ENTITY_TYPE.getId(identity.getType()).toString());
        tag.put("EntityData", entityTag);
        return tag;
    }

    @Unique
    public void readCurrentIdentity(NbtCompound tag) {
        Optional<EntityType<?>> type = EntityType.fromNbt(tag);

        // set identity to null (no identity) if the entity id is "minecraft:empty"
        if(tag.getString("id").equals("minecraft:empty")) {
            this.identity = null;
            ((DimensionsRefresher) this).identity_refreshDimensions();
        }

        // if entity type was valid, deserialize entity data from tag
        else if(type.isPresent()) {
            NbtCompound entityTag = tag.getCompound("EntityData");

            // ensure entity data exists
            if(entityTag != null) {
                if(identity == null || !type.get().equals(identity.getType())) {
                    identity = (LivingEntity) type.get().create(getWorld());

                    // refresh player dimensions/hitbox on client
                    ((DimensionsRefresher) this).identity_refreshDimensions();
                }

                identity.readNbt(entityTag);
                identityType = IdentityType.fromEntityNbt(tag);
            }
        }
    }

    @Unique
    @Override
    public Set<IdentityType<?>> getUnlocked() {
        return unlocked;
    }

    @Override
    public void setUnlocked(Set<IdentityType<?>> unlocked) {
        this.unlocked.clear();
        this.unlocked.addAll(unlocked);
    }
    @Unique
    private boolean identityIsUndead = false;

    @Unique
    public boolean identity$isIdentityUndead() {
        return identityIsUndead;
    }

    @Unique
    @Override
    public Set<IdentityType<?>> getFavorites() {
        return favorites;
    }

    @Override
    public void setFavorites(Set<IdentityType<?>> favorites) {
        this.favorites.clear();
        this.favorites.addAll(favorites);
    }

    @Unique
    @Override
    public int getRemainingHostilityTime() {
        return remainingTime;
    }

    @Unique
    @Override
    public void setRemainingHostilityTime(int max) {
        remainingTime = max;
    }

    @Unique
    @Override
    public int getAbilityCooldown() {
        return abilityCooldown;
    }

    @Unique
    @Override
    public void setAbilityCooldown(int abilityCooldown) {
        this.abilityCooldown = abilityCooldown;
    }

    @Unique
    @Override
    public LivingEntity getIdentity() {
        return identity;
    }

    @Override
    public IdentityType<?> getIdentityType() {
        return identityType;
    }

    @Override
    public void setIdentityType(@Nullable IdentityType<?> type) {
        identityType = type;
    }

    @Override
    public Map<String, NbtCompound> getVillagerIdentities() {
        return villagerIdentities;
    }

    @Override
    public void setVillagerIdentity(String key, NbtCompound identity) {
        if(identity == null) {
            villagerIdentities.remove(key);
        } else {
            villagerIdentities.put(key, identity);
        }
    }

    @Override
    public void removeVillagerIdentity(String key) {
        villagerIdentities.remove(key);
        if(activeVillagerKey != null && activeVillagerKey.equals(key)) {
            activeVillagerKey = null;
        }
    }

    @Override
    public @Nullable String getActiveVillagerKey() {
        return activeVillagerKey;
    }

    @Override
    public void setActiveVillagerKey(@Nullable String key) {
        activeVillagerKey = key;
    }


    @Unique
    @Override
    public void setIdentity(LivingEntity identity) {
        this.identity = identity;
    }

    @Unique
    @Override
    public boolean updateIdentity(@Nullable LivingEntity identity) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        EventResult result = IdentitySwapCallback.EVENT.invoker().swap((ServerPlayerEntity) player, identity);
        if(result.isFalse()) {
            return false;
        }

        this.identity = identity;

        if(!(identity instanceof VillagerEntity)) {
            activeVillagerKey = null;
        }
        // refresh entity hitbox dimensions
        ((DimensionsRefresher) player).identity_refreshDimensions();
        if (identity != null) {
            this.identityIsUndead = identity.getType().isIn(EntityTypeTags.UNDEAD);
        } else {
            this.identityIsUndead = false;
        }

        // Identity is valid and scaling health is on; set entity's max health and current health to reflect identity.
        if (identity != null && IdentityConfig.getInstance().scalingHealth()) {
            double oldMax = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).getBaseValue();
            double newMax = Math.min(IdentityConfig.getInstance().maxHealth(), identity.getMaxHealth());
            identity$scaleHealth(player, oldMax, newMax);
        }


        // If the identity is null (going back to player), set the player's base health value to 20 (default) to clear old changes.
        if (identity == null && IdentityConfig.getInstance().scalingHealth()) {
            double oldMax = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).getBaseValue();
            double newMax = 20.0;
            identity$scaleHealth(player, oldMax, newMax);
        }


        // update flight properties on player depending on identity
        ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;
        if(Identity.hasFlyingPermissions((ServerPlayerEntity) player)) {
            FlightHelper.grantFlightTo(serverPlayerEntity);
            player.getAbilities().setFlySpeed(IdentityConfig.getInstance().flySpeed());
            player.sendAbilitiesUpdate();
        } else {
            FlightHelper.revokeFlight(serverPlayerEntity);
            player.getAbilities().setFlySpeed(0.05f);
            player.sendAbilitiesUpdate();
        }

        // If the player is riding a Ravager and changes into an Identity that cannot ride Ravagers, kick them off.
        if(player.getVehicle() instanceof RavagerEntity) {
            if(identity == null) {
                player.stopRiding();
            }
            else if( !(identity.getType().isIn(IdentityEntityTags.RAVAGER_RIDING)) || SafeTagManager.isCustomRavagerRiding(identity.getType()))
                player.stopRiding();
        }

        // sync with client
        if(!player.getWorld().isClient) {
            PlayerIdentity.sync((ServerPlayerEntity) player);

            Int2ObjectMap<Object> trackers = ((ServerChunkLoadingManagerAccessor) ((ServerWorld) player.getWorld()).getChunkManager().chunkLoadingManager).getEntityTrackers();
            Object tracking = trackers.get(player.getId());
            ((EntityTrackerAccessor) tracking).getListeners().forEach(listener -> {
                PlayerIdentity.sync((ServerPlayerEntity) player, listener.getPlayer());
            });
        }

        return true;
    }

    @Unique
    private void identity$scaleHealth(PlayerEntity player, double oldMax, double newMax) {
        double currentHealth = player.getHealth();

        double ratio = (oldMax > 0.0) ? (currentHealth / oldMax) : 1.0;
        double scaledHealth = net.minecraft.util.math.MathHelper.clamp(ratio * newMax, 1.0, newMax);

        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(newMax);
        player.setHealth((float) scaledHealth);
    }
}
