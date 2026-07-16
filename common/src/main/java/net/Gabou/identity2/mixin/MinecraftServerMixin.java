package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.Gabou.identity2.ModRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
import net.Gabou.identity2.ModEffects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.Gabou.identity2.ModComponents;
import net.Gabou.identity2.Identity2;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements net.Gabou.identity2.util.MinecraftServerAccessor{
    @Unique
    private boolean identity2$moddedMobAbilityCoverageValidated;

    @Shadow
    public ServerFunctionManager functionManager;
    @Shadow
    public ServerFunctionManager getFunctions(){
		return functionManager;
	}
    @Override
    public ServerFunctionManager getCommandFunctionManager() {
        return this.getFunctions();
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void identity2$validateModdedMobAbilityCoverage(
            BooleanSupplier hasTimeLeft,
            CallbackInfo callbackInfo
    ) {
        if (identity2$moddedMobAbilityCoverageValidated) {
            return;
        }

        MinecraftServer server = (MinecraftServer) (Object) this;
        if (server.registryAccess().registry(ModRegistries.IDENTITY_ABILITY_KEY).isEmpty()) {
            return;
        }

        ModRegistries.captureIdentityAbilityRegistry(server.registryAccess());
        identity2$moddedMobAbilityCoverageValidated = true;
    }
}

