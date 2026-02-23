package draylar.identity.impl;

import draylar.identity.api.variant.IdentityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.Map;

public interface PlayerDataProvider {

    Set<IdentityType<?>> getUnlocked();
    void setUnlocked(Set<IdentityType<?>> unlocked);

    Set<IdentityType<?>> getFavorites();
    void setFavorites(Set<IdentityType<?>> favorites);

    int getRemainingHostilityTime();
    void setRemainingHostilityTime(int max);

    int getAbilityCooldown();
    void setAbilityCooldown(int cooldown);

    LivingEntity getIdentity();
    void setIdentity(@Nullable LivingEntity identity);
    boolean updateIdentity(@Nullable LivingEntity identity);

    IdentityType<?> getIdentityType();
    void setIdentityType(@Nullable IdentityType<?> type);

    Map<String, NbtCompound> getVillagerIdentities();
    void setVillagerIdentity(String key, NbtCompound identity);
    void removeVillagerIdentity(String key);
    @Nullable String getActiveVillagerKey();
    void setActiveVillagerKey(@Nullable String key);
}
