package net.Gabou.identity2.util;

import net.Gabou.identity2.IdentitySettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        if (!livingIdentity.canUseSlot(slot)) {
            return Items.AIR.getDefaultInstance();
        }
        return null;
    }
}
