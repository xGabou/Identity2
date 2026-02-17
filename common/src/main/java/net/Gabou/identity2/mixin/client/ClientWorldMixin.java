package net.Gabou.identity2.mixin.client;

import net.minecraft.registry.Registries;
import net.Gabou.identity2.Identity2Client;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.MathHelper;
import java.util.List;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.AirBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.MovementType;
import net.Gabou.identity2.ModEffects;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import java.util.Set;
import net.Gabou.identity2.ModBlocks;
import net.minecraft.client.world.ClientWorld;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.EnderDragonEntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import java.util.function.BiFunction;
import net.minecraft.util.Identifier;
import net.Gabou.identity2.Identity2;
@Mixin(ClientWorld.class)
public class ClientWorldMixin{
	@Shadow
    public static Set<Item> BLOCK_MARKER_ITEMS = Set.of(Items.BARRIER, Items.LIGHT,ModBlocks.MAGIC_BARRIER_BLOCK.asItem());
    
    @Inject(method = "tickEntity", at = @At("TAIL"))
	private void tickIdentity(Entity entity,CallbackInfo info) {
        if(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").isPresent()){
            if(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get().length()!=0){
                String d=((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get();
                if(d.contains("{")){
                    d=d.substring(0,d.indexOf('{'));
                }
                if(Registries.ENTITY_TYPE.containsId(Identifier.of(d))){
                    if(((EntityAccessor)entity).getCurrentIdentity()!=null){
                    //Sync identity to entity
                    Identifier id=Identifier.of(d);
                    Entity identity=((EntityAccessor)entity).getCurrentIdentity();
                    identity.tick();
                    if(Identity2Client.visualPatchKeys.contains(id)){
                        BiFunction<Entity,Entity,Entity> patchFunction= Identity2Client.visualPatchValues.get(Identity2Client.visualPatchKeys.indexOf(id));
                        ((EntityAccessor)entity).setCurrentIdentity(patchFunction.apply(identity,entity));
                        //Identity2.LOGGER.info("visual patch "+id);
                    }
                    }
                }
            }
        }
    }
}

