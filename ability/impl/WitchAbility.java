package draylar.identity.ability.impl;

import draylar.identity.ability.IdentityAbility;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

public class WitchAbility extends IdentityAbility<WitchEntity> {

    public static final List<RegistryEntry<Potion>> VALID_POTIONS = Arrays.asList(Potions.HARMING, Potions.POISON, Potions.SLOWNESS, Potions.WEAKNESS);

    @Override
    public void onUse(PlayerEntity player, WitchEntity identity, World world) {
        if (!world.isClient) {
            PotionEntity potionEntity = new PotionEntity(world, player);

            // choisir une potion au hasard dans ta liste
            RegistryEntry<Potion> potion = VALID_POTIONS.get(world.random.nextInt(VALID_POTIONS.size()));

            // créer l’item stack avec son composant PotionContents
            ItemStack potionStack = new ItemStack(Items.SPLASH_POTION);
            potionStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(potion));

            potionEntity.setItem(potionStack);

            potionEntity.setPitch(-20.0F);
            Vec3d rotation = player.getRotationVec(1.0F);
            potionEntity.setVelocity(rotation.getX(), rotation.getY(), rotation.getZ(), 0.75F, 8.0F);

            world.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WITCH_THROW,
                    SoundCategory.PLAYERS,
                    1.0F,
                    0.8F + world.getRandom().nextFloat() * 0.4F
            );

            world.spawnEntity(potionEntity);
        }
    }


    @Override
    public Item getIcon() {
        return Items.POTION;
    }
}
