package net.Gabou.identity2.mixin.client;

import java.util.function.BiFunction;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientWorldMixin {
    @Inject(method = "tickNonPassenger", at = @At("TAIL"))
    private void tickIdentity(Entity entity, CallbackInfo info) {
        Entity identity = ((EntityAccessor) entity).getCurrentIdentity();
        if (identity == null) {
            return;
        }

        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(identity.getType());
        if (id == null) {
            return;
        }

        int patchIndex = Identity2Client.visualPatchKeys.indexOf(id);
        if (patchIndex >= 0) {
            BiFunction<Entity, Entity, Entity> patchFunction = Identity2Client.visualPatchValues.get(patchIndex);
            ((EntityAccessor) entity).setCurrentIdentity(patchFunction.apply(identity, entity));
        }
    }
}

