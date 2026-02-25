package net.Gabou.identity2.mixin;

import net.Gabou.identity2.Identity2;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 29999984), require = 0)
    private int identity2$expandAbsoluteMaxSize(int original) {
        return Identity2.maxWorldSize - 16;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(doubleValue = 5.9999968E7D), require = 0)
    private double identity2$expandDefaultBorderSize(double original) {
        return (Identity2.maxWorldSize - 16) * 2.0D;
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(doubleValue = 5.9999968E7D), require = 0)
    private static double identity2$expandDefaultSettingsSize(double original) {
        return (Identity2.maxWorldSize - 16) * 2.0D;
    }

    @Mixin(WorldBorder.Settings.class)
    public abstract static class SettingsMixin {
        @ModifyConstant(method = "read", constant = @Constant(doubleValue = -2.9999984E7D), require = 0)
        private static double identity2$expandMinCenterCoordinate(double original) {
            return -Identity2.maxWorldSize + 16;
        }

        @ModifyConstant(method = "read", constant = @Constant(doubleValue = 2.9999984E7D), require = 0)
        private static double identity2$expandMaxCenterCoordinate(double original) {
            return Identity2.maxWorldSize - 16;
        }
    }
}
