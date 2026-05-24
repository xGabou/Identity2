package net.Gabou.identity2.util;

import net.Gabou.identity2.IdentitySettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.lang.reflect.Method;

public final class IdentityEquipmentHelper {
    private IdentityEquipmentHelper() {
    }

    public static ItemStack getBlockedSlotStack(Entity host, EquipmentSlot slot) {
        if (host == null || slot == null) {
            return null;
        }
        Entity identity = ((EntityAccessor) host).getCurrentIdentity();
        if (!(identity instanceof LivingEntity livingIdentity)) {
            return null;
        }
        if (slot.getType() == EquipmentSlot.Type.HAND && !IdentitySettings.identitiesEquipItems) {
            return Items.AIR.getDefaultInstance();
        }
        if (slot.getType() != EquipmentSlot.Type.HAND && !IdentitySettings.identitiesEquipArmor) {
            return Items.AIR.getDefaultInstance();
        }
        if (!canUseSlot(livingIdentity, slot)) {
            return Items.AIR.getDefaultInstance();
        }
        return null;
    }

    private static boolean canUseSlot(LivingEntity livingIdentity, EquipmentSlot slot) {
        try {
            Method method = LivingEntity.class.getMethod("canUseSlot", EquipmentSlot.class);
            return (boolean) method.invoke(livingIdentity, slot);
        } catch (Throwable ignored) {
            return true;
        }
    }
}
