package net.Gabou.identity2.util;

import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

public interface EntityAccessor {
    void fixAttributes(Entity entity, Entity identity);

    NbtComponent getCustomData();

    Entity getCurrentIdentity();

    void setCurrentIdentity(Entity entity);

    void setCurrentIdentity(String id, NbtCompound data);

    void setCurrentIdentity(String id);

    void setVehicle(Entity vehicle);

    void setTouchingWater(boolean touchingWater);

    void setLastPosition(Vec3d pos);

    void runAddAirTravelEffects();

    EntityDimensions getEntityDimensions();

    void setEntityDimensions(EntityDimensions dimensions);

    float getStandingEyeHeight();

    void setStandingEyeHeight(float standingEyeHeight);

    Entity getIdentityOwner();

    void setIdentityOf(Entity entity);

    Entity.MoveEffect getMoveEffect();

    boolean isFlappingWings();

    void setId(int id);

    int getId();

    double getGravity();

    boolean shouldTickBlockCollision();

    boolean canFly();

    int getAbilityCooldown();

    void setAbilityCooldown(int cooldown);
}
