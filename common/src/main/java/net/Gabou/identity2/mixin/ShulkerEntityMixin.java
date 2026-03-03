package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.ShulkerEntityAccessor;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
@Mixin(Shulker.class)
public class ShulkerEntityMixin implements ShulkerEntityAccessor{

    @Shadow
    public void findNewAttachment(){};
    public void runTryAttachOrTeleport(){this.findNewAttachment();}
    @Shadow
    public void setRawPeekAmount(int amount){};
    @Override
    public void setPeekAmount(int amount) {
        this.setRawPeekAmount(amount);
    }
    @Shadow
    public int getRawPeekAmount(){return 0;};
    public int runGetPeekAmount(){return this.getRawPeekAmount();}
//Tons of Redirects - End
}

