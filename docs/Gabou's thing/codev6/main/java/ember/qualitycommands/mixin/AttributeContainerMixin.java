package ember.qualitycommands.mixin;
import com.google.common.collect.Lists;
import net.minecraft.util.math.MathHelper;
import java.util.List;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
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
import net.minecraft.registry.tag.FluidTags;
import org.jetbrains.annotations.Nullable;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ExecuteCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.CommandManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import ember.qualitycommands.ModComponents;
import ember.qualitycommands.QualityCommands;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.entity.Entity;
import net.minecraft.component.type.NbtComponent;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.BlockView;
import com.llamalad7.mixinextras.sugar.Local;

import ember.qualitycommands.util.LivingEntityAccessor;
import ember.qualitycommands.util.AttributeContainerAccessor;
import ember.qualitycommands.util.NbtComponentAccessor;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.Box;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.attribute.EntityAttribute;
@Mixin(AttributeContainer.class)
public class AttributeContainerMixin implements AttributeContainerAccessor{
    
    @Shadow
    @Mutable
    public DefaultAttributeContainer defaultAttributes;
    public DefaultAttributeContainer getDefaultAttributes(){
        return this.defaultAttributes;
   };
}

