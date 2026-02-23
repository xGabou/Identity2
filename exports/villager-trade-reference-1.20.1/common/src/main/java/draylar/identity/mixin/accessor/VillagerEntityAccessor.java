package draylar.identity.mixin.accessor;

import net.minecraft.village.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VillagerEntity.class)
public interface VillagerEntityAccessor {

    @Accessor("experience")
    int getExperience();

    @Accessor("experience")
    void setExperience(int value);

    @Invoker("canLevelUp")
    boolean callGetNextLevelExperience();

    @Invoker("fillRecipes")
    void callFillRecipes();

    @Invoker("levelUp")
    void callLevelUp();
}

