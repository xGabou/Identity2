package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerPersistenceMixin {
    @Unique
    private static final String CUSTOM_DATA_TAG_KEY = "identity2_custom_data";

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 0)
    private void identity2$saveCustomData(CompoundTag compoundTag, CallbackInfo info) {
        identity2$writeCustomData(compoundTag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 0)
    private void identity2$loadCustomData(CompoundTag compoundTag, CallbackInfo info) {
        identity2$readCustomData(compoundTag);
    }

    private void identity2$writeCustomData(CompoundTag compoundTag) {
        if (compoundTag == null) {
            return;
        }

        CompoundTag customData = ((EntityAccessor) this).getCustomData();
        if (customData == null || customData.isEmpty()) {
            compoundTag.remove(CUSTOM_DATA_TAG_KEY);
            return;
        }

        compoundTag.put(CUSTOM_DATA_TAG_KEY, customData.copy());
    }

    private void identity2$readCustomData(CompoundTag compoundTag) {
        EntityAccessor accessor = (EntityAccessor) this;
        if (compoundTag != null && compoundTag.contains(CUSTOM_DATA_TAG_KEY, Tag.TAG_COMPOUND)) {
            accessor.getCustomData().merge(compoundTag.getCompound(CUSTOM_DATA_TAG_KEY).copy());
            return;
        }
        accessor.getCustomData();
    }
}
