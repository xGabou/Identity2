package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
@Mixin(EnderDragon.class)
public class EnderDragonEntityMixin implements net.Gabou.identity2.util.EnderDragonEntityAccessor{
    @Shadow
    public void checkCrystals(){}

    @Shadow
    private int growlTime;
	public int setTicksUntilNextGrowl(int ticks){return this.growlTime=ticks;}

    public int getTicksUntilNextGrowl(){return this.growlTime;}

    public void runTickWithEndCrystals(){this.checkCrystals();}
}

