package ember.qualitycommands.mixin.client;

import net.minecraft.registry.Registries;
import ember.qualitycommands.QualityCommandsClient;
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
import ember.qualitycommands.ModEffects;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import java.util.Set;
import ember.qualitycommands.ModBlocks;
import net.minecraft.client.world.ClientWorld;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.util.EnderDragonEntityAccessor;
import ember.qualitycommands.util.NbtComponentAccessor;
import java.util.function.BiFunction;
import net.minecraft.util.Identifier;
import ember.qualitycommands.QualityCommands;
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
                    if(QualityCommandsClient.visualPatchKeys.contains(id)){
                        BiFunction<Entity,Entity,Entity> patchFunction= QualityCommandsClient.visualPatchValues.get(QualityCommandsClient.visualPatchKeys.indexOf(id));
                        ((EntityAccessor)entity).setCurrentIdentity(patchFunction.apply(identity,entity));
                        //QualityCommands.LOGGER.info("visual patch "+id);
                    }
                    }
                }
            }
        }
    }
}
