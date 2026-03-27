package net.Gabou.identity2.api.morph;

import net.minecraft.world.entity.Entity;

public interface IdentityMorphTickHandler {
    void tick(Entity host, Entity currentMorph);
}
