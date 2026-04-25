package ember.qualitycommands.mixin.client;

import net.minecraft.registry.Registries;
import ember.qualitycommands.QualityCommandsClient;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.MathHelper;
import java.util.List;
import java.util.Map;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.state.EntityRenderState;
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
import net.minecraft.util.ResourceLocation;
import ember.qualitycommands.QualityCommands;
import ember.qualitycommands.util.EntityRenderStateModifier;
@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateModifier{
	Map<String,List<String>> targets=Map.of("head",List.of("dirt","glass"),"left_leg",List.of("minecraft:tnt"));
    List<String> overlays=List.of("dirt","glass");
    List<String> overlaysE=List.of("dirt","glass");
    @Override
    public Map<String,List<String>> getTargets(){
        return this.targets;
    }
    @Override
    public void setTargets(Map<String,List<String>> targets){
        this.targets=targets;
    }
    @Override
    public List<String> getOverlays(){return overlays;};
    @Override
    public void setOverlays(List<String> overlays){
        this.overlays=overlays;
    };
    @Override
    public List<String> getOverlaysE(){return overlaysE;};
    @Override
    public void setOverlaysE(List<String> overlaysE){
        this.overlaysE=overlaysE;
    };
}
