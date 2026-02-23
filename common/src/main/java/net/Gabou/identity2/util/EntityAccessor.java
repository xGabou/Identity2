package net.Gabou.identity2.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

public interface EntityAccessor {
    void fixAttributes(Entity entity, Entity identity);

    CustomData getCustomData();

    Entity getCurrentIdentity();

    void setCurrentIdentity(Entity entity);

    void setCurrentIdentity(String id, CompoundTag data);

    void setCurrentIdentity(String id);

    void setVehicle(Entity vehicle);

    void setTouchingWater(boolean touchingWater);

    void setLastPosition(Vec3 pos);

    void runAddAirTravelEffects();

    EntityDimensions getEntityDimensions();

    void setEntityDimensions(EntityDimensions dimensions);

    float getStandingEyeHeight();

    void setStandingEyeHeight(float standingEyeHeight);

    Entity getIdentityOwner();

    void setIdentityOf(Entity entity);

    Entity.MovementEmission getMoveEffect();

    boolean isFlappingWings();

    void setId(int id);

    int getId();

    double getIdentityGravity();

    boolean shouldTickBlockCollision();

    boolean canFly();

    int getAbilityCooldown();

    void setAbilityCooldown(int cooldown);

    int getSecondaryAbilityCooldown();

    void setSecondaryAbilityCooldown(int cooldown);
}
