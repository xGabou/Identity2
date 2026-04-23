package net.Gabou.identity2.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserApiService;
import net.Gabou.identity2.Identity2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = YggdrasilAuthenticationService.class, remap = false)
public abstract class YggdrasilAuthenticationServiceMixin {
    @WrapOperation(
        method = "createUserApiService(Ljava/lang/String;)Lcom/mojang/authlib/minecraft/UserApiService;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/authlib/yggdrasil/YggdrasilUserApiService;fetchProperties()V",
            remap = false
        )
    )
    private void identity2$guardFetchProperties(YggdrasilUserApiService userApiService, Operation<Void> original) {
        try {
            original.call(userApiService);
        } catch (Throwable throwable) {
            if (identity2$isInvalidCredentials(throwable)) {
                throw new IllegalStateException("Invalid Mojang credentials; refusing to continue.", throwable);
            }
            if (identity2$isAuthServiceUnavailable(throwable)) {
                Identity2.LOGGER.warn(
                    "Mojang auth service unavailable; continuing in offline mode: {}",
                    throwable.getMessage()
                );
                return;
            }
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Unexpected auth failure while fetching Mojang user properties.", throwable);
        }
    }

    private static boolean identity2$isInvalidCredentials(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InvalidCredentialsException) {
                return true;
            }
            if (current instanceof MinecraftClientHttpException httpException
                && httpException.getStatus() == MinecraftClientHttpException.UNAUTHORIZED) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean identity2$isAuthServiceUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AuthenticationUnavailableException || current instanceof MinecraftClientException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
