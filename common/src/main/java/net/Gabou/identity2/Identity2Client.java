package net.Gabou.identity2;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.Gabou.identity2.client.platform.ModClientPlatform;
import net.Gabou.identity2.client.screen.IdentitySelectionScreen;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.packets.IdentityAbilityPacketPayload;
import net.Gabou.identity2.packets.IdentityMorphRequestC2SPacketPayload;
import net.Gabou.identity2.util.EnderDragonEntityRendererAccessor;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.BiFunction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class Identity2Client {
    private static final Identity2Client INSTANCE = new Identity2Client();
    private static ModClientPlatform platform;
    private static boolean initialized = false;

    public static final ArrayList<BiFunction<Entity, Entity, Entity>> visualPatchValues = new ArrayList<>(0);
    public static final ArrayList<Identifier> visualPatchKeys = new ArrayList<>(0);
    private static final KeyBinding.Category IDENTITY_KEY_CATEGORY = KeyBinding.Category.create(Identifier.of("category.identity2.test"));

    private static final KeyBinding abilityKeyBinding = new KeyBinding(
        "key.identity2.dashminus",
        InputUtil.Type.KEYSYM,
        InputUtil.GLFW_KEY_V,
        IDENTITY_KEY_CATEGORY
    );
    private static final KeyBinding identityMenuKeyBinding = new KeyBinding(
        "key.identity2.identity_menu",
        InputUtil.Type.KEYSYM,
        InputUtil.GLFW_KEY_G,
        IDENTITY_KEY_CATEGORY
    );

    private static final int fadingTickRequirement = 0;
    private static final int MAX_PENDING_PACKET_PROCESS_PER_TICK = 256;
    private static final int MAX_PENDING_PACKET_PROCESS_TICKS = 100;
    private static final int MAX_PENDING_PACKET_QUEUE_SIZE = 8192;
    private static int lastCooldown = 0;
    private static int ticksSinceUpdate = 0;
    private static boolean isFading = false;
    private static int fadingProgress = 0;
    private static int pendingPacketProcessTicks = 0;
    private static final ArrayList<CustomEntityDataS2CPacketPayload> pendingDoubleDataPackets = new ArrayList<>(0);
    private static final ArrayList<CustomEntityStringDataS2CPacketPayload> pendingStringDataPackets = new ArrayList<>(0);
    private static final ArrayList<CustomEntityBoolDataS2CPacketPayload> pendingBoolDataPackets = new ArrayList<>(0);

    static {
        addVisualPatch((identity, entity) -> {
            if (identity instanceof EnderDragonEntity dragonIdentity) {
                dragonIdentity.yawAcceleration += MathHelper.wrapDegrees(entity.getYaw() - identity.getYaw()) * 0.1F;
            }
            return identity;
        }, Identifier.of("minecraft:ender_dragon"));
    }

    private Identity2Client() {
    }

    public static void initialize(ModClientPlatform platformImpl) {
        if (initialized) {
            return;
        }

        platform = platformImpl;
        initialized = true;

        KeyMappingRegistry.register(abilityKeyBinding);
        KeyMappingRegistry.register(identityMenuKeyBinding);

        if (platform != null) {
            platform.logClientRegistries();
        }

        NetworkManager.registerReceiver(
            NetworkManager.s2c(),
            CustomEntityDataS2CPacketPayload.ID,
            CustomEntityDataS2CPacketPayload.CODEC,
            (payload, context) -> context.queue(() -> INSTANCE.onUpdateCustomData(payload))
        );
        NetworkManager.registerReceiver(
            NetworkManager.s2c(),
            CustomEntityStringDataS2CPacketPayload.ID,
            CustomEntityStringDataS2CPacketPayload.CODEC,
            (payload, context) -> context.queue(() -> INSTANCE.onUpdateCustomData(payload))
        );
        NetworkManager.registerReceiver(
            NetworkManager.s2c(),
            CustomEntityBoolDataS2CPacketPayload.ID,
            CustomEntityBoolDataS2CPacketPayload.CODEC,
            (payload, context) -> context.queue(() -> INSTANCE.onUpdateCustomData(payload))
        );

        ClientTickEvent.CLIENT_POST.register(Identity2Client::onClientTickEnd);
        ClientGuiEvent.RENDER_HUD.register(Identity2Client::renderIdentityCooldown);
    }

    public static void sendIdentityAbilityPacket(int entityId) {
        NetworkManager.sendToServer(new IdentityAbilityPacketPayload(entityId));
    }

    public static void sendMorphRequest(String identityId) {
        sendMorphRequest(identityId, "");
    }

    public static void sendMorphRequest(String identityId, String variantNbt) {
        NetworkManager.sendToServer(new IdentityMorphRequestC2SPacketPayload(identityId, variantNbt == null ? "" : variantNbt));
    }

    public static void addVisualPatch(BiFunction<Entity, Entity, Entity> value, Identifier id) {
        visualPatchKeys.ensureCapacity(visualPatchKeys.size() + 1);
        visualPatchValues.ensureCapacity(visualPatchValues.size() + 1);
        visualPatchKeys.add(id);
        visualPatchValues.add(value);
    }

    private static void onClientTickEnd(MinecraftClient client) {
        processPendingCustomDataPackets(client);

        while (identityMenuKeyBinding.wasPressed()) {
            if (client.player != null && client.currentScreen == null) {
                client.setScreen(new IdentitySelectionScreen());
            }
        }

        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            return;
        }

        Registry<IdentityAbilityDefinition> identityAbilityRegistry = ModRegistries.getIdentityAbilityRegistry();
        if (identityAbilityRegistry == null) {
            return;
        }

        IdentityAbilityDefinition identityAbility = identityAbilityRegistry.get(net.minecraft.entity.EntityType.getId(identity.getType()));
        if (identityAbility == null) {
            return;
        }

        int usedAbility = 0;

        while (abilityKeyBinding.wasPressed()) {
            if (((EntityAccessor) player).getAbilityCooldown() == 0) {
                ((EntityAccessor) player).setAbilityCooldown(identityAbility.cooldown() + identityAbility.useduration());
                sendIdentityAbilityPacket(0);
                usedAbility = 1;
            }
        }

        int cd = ((EntityAccessor) player).getAbilityCooldown();
        if (cd > identityAbility.cooldown()) {
            sendIdentityAbilityPacket(identityAbility.cooldown() + identityAbility.useduration() - cd + 1);
        }

        // Passive tick packet is only needed for identities that implement passive behavior.
        if (hasPassiveTick(identityAbility)) {
            sendIdentityAbilityPacket(-1 - usedAbility);
        }
    }

    private void onUpdateCustomData(CustomEntityDataS2CPacketPayload packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            enqueuePendingPacket(pendingDoubleDataPackets, packet);
            return;
        }

        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity != null) {
            NbtComponent n = ((EntityAccessor) entity).getCustomData();
            boolean shapeChanged = false;
            for (CustomEntityDataS2CPacket.Entry entry : packet.entries()) {
                ((NbtComponentAccessor) (Object) n).getNbt().putDouble(entry.key(), entry.value());
                if ("width_override".equals(entry.key()) || "height_override".equals(entry.key())) {
                    shapeChanged = true;
                }
            }
            if (shapeChanged) {
                ((EntityAccessor) entity).setEntityDimensions(entity.getDimensions(entity.getPose()));
                Entity identity = ((EntityAccessor) entity).getCurrentIdentity();
                if (identity != null) {
                    ((EntityAccessor) entity).setStandingEyeHeight(identity.getStandingEyeHeight());
                }
            }
        }
    }

    private void onUpdateCustomData(CustomEntityStringDataS2CPacketPayload packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            enqueuePendingPacket(pendingStringDataPackets, packet);
            return;
        }

        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity != null) {
            NbtComponent n = ((EntityAccessor) entity).getCustomData();
            boolean identityDataChanged = false;
            for (CustomEntityDataS2CPacket.EntryString entry : packet.entries()) {
                ((NbtComponentAccessor) (Object) n).getNbt().putString(entry.key(), entry.value());
                if (
                    "model_override".equals(entry.key()) ||
                    IdentityProgression.SELECTED_IDENTITY_TYPE_KEY.equals(entry.key()) ||
                    IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY.equals(entry.key())
                ) {
                    identityDataChanged = true;
                }
            }
            if (identityDataChanged) {
                applyIdentityFromCustomData(entity);
            }
        }
    }

    private void onUpdateCustomData(CustomEntityBoolDataS2CPacketPayload packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            enqueuePendingPacket(pendingBoolDataPackets, packet);
            return;
        }

        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity != null) {
            NbtComponent n = ((EntityAccessor) entity).getCustomData();
            for (CustomEntityDataS2CPacket.EntryBool entry : packet.entries()) {
                ((NbtComponentAccessor) (Object) n).getNbt().putBoolean(entry.key(), entry.value());
            }
        }
    }

    private static void processPendingCustomDataPackets(MinecraftClient client) {
        if (client.world == null) {
            return;
        }

        if (pendingDoubleDataPackets.isEmpty() && pendingStringDataPackets.isEmpty() && pendingBoolDataPackets.isEmpty()) {
            pendingPacketProcessTicks = 0;
            return;
        }

        pendingPacketProcessTicks++;
        processPendingDoublePackets(client);
        processPendingStringPackets(client);
        processPendingBoolPackets(client);

        // Avoid an unbounded per-tick scan if some queued packets can never resolve.
        if (pendingPacketProcessTicks > MAX_PENDING_PACKET_PROCESS_TICKS) {
            pendingDoubleDataPackets.clear();
            pendingStringDataPackets.clear();
            pendingBoolDataPackets.clear();
            pendingPacketProcessTicks = 0;
        }
    }

    private static void processPendingDoublePackets(MinecraftClient client) {
        int max = Math.min(MAX_PENDING_PACKET_PROCESS_PER_TICK, pendingDoubleDataPackets.size());
        for (int i = 0; i < max; ) {
            if (INSTANCE.tryApplyCustomData(client, pendingDoubleDataPackets.get(i))) {
                pendingDoubleDataPackets.remove(i);
                max--;
            } else {
                i++;
            }
        }
    }

    private static void processPendingStringPackets(MinecraftClient client) {
        int max = Math.min(MAX_PENDING_PACKET_PROCESS_PER_TICK, pendingStringDataPackets.size());
        for (int i = 0; i < max; ) {
            if (INSTANCE.tryApplyCustomData(client, pendingStringDataPackets.get(i))) {
                pendingStringDataPackets.remove(i);
                max--;
            } else {
                i++;
            }
        }
    }

    private static void processPendingBoolPackets(MinecraftClient client) {
        int max = Math.min(MAX_PENDING_PACKET_PROCESS_PER_TICK, pendingBoolDataPackets.size());
        for (int i = 0; i < max; ) {
            if (INSTANCE.tryApplyCustomData(client, pendingBoolDataPackets.get(i))) {
                pendingBoolDataPackets.remove(i);
                max--;
            } else {
                i++;
            }
        }
    }

    private boolean tryApplyCustomData(MinecraftClient client, CustomEntityDataS2CPacketPayload packet) {
        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity == null) {
            return false;
        }

        NbtComponent n = ((EntityAccessor) entity).getCustomData();
        boolean shapeChanged = false;
        for (CustomEntityDataS2CPacket.Entry entry : packet.entries()) {
            ((NbtComponentAccessor) (Object) n).getNbt().putDouble(entry.key(), entry.value());
            if ("width_override".equals(entry.key()) || "height_override".equals(entry.key())) {
                shapeChanged = true;
            }
        }
        if (shapeChanged) {
            ((EntityAccessor) entity).setEntityDimensions(entity.getDimensions(entity.getPose()));
            Entity identity = ((EntityAccessor) entity).getCurrentIdentity();
            if (identity != null) {
                ((EntityAccessor) entity).setStandingEyeHeight(identity.getStandingEyeHeight());
            }
        }
        return true;
    }

    private boolean tryApplyCustomData(MinecraftClient client, CustomEntityStringDataS2CPacketPayload packet) {
        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity == null) {
            return false;
        }

        NbtComponent n = ((EntityAccessor) entity).getCustomData();
        boolean identityDataChanged = false;
        for (CustomEntityDataS2CPacket.EntryString entry : packet.entries()) {
            ((NbtComponentAccessor) (Object) n).getNbt().putString(entry.key(), entry.value());
            if (
                "model_override".equals(entry.key()) ||
                IdentityProgression.SELECTED_IDENTITY_TYPE_KEY.equals(entry.key()) ||
                IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY.equals(entry.key())
            ) {
                identityDataChanged = true;
            }
        }
        if (identityDataChanged) {
            applyIdentityFromCustomData(entity);
        }
        return true;
    }

    private boolean tryApplyCustomData(MinecraftClient client, CustomEntityBoolDataS2CPacketPayload packet) {
        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity == null) {
            return false;
        }

        NbtComponent n = ((EntityAccessor) entity).getCustomData();
        for (CustomEntityDataS2CPacket.EntryBool entry : packet.entries()) {
            ((NbtComponentAccessor) (Object) n).getNbt().putBoolean(entry.key(), entry.value());
        }
        return true;
    }

    private static Entity resolvePacketTarget(MinecraftClient client, int entityId) {
        if (client.world != null) {
            Entity entity = client.world.getEntityById(entityId);
            if (entity != null) {
                return entity;
            }
        }
        if (client.player != null && client.player.getId() == entityId) {
            return client.player;
        }
        return null;
    }

    private static boolean hasPassiveTick(IdentityAbilityDefinition identityAbility) {
        Identifier predef = identityAbility.bultinability();
        return predef != null && "shulker".equals(predef.getPath());
    }

    private void applyIdentityFromCustomData(Entity entity) {
        NbtComponent n = ((EntityAccessor) entity).getCustomData();
        String type = ((NbtComponentAccessor) (Object) n).getNbt().getString(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        if (type.isBlank()) {
            type = ((NbtComponentAccessor) (Object) n).getNbt().getString("model_override", "");
        }
        if (type.isBlank()) {
            ((EntityAccessor) entity).setCurrentIdentity("");
            return;
        }
        String variantRaw = ((NbtComponentAccessor) (Object) n).getNbt().getString(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
        ((EntityAccessor) entity).setCurrentIdentity(type, IdentityProgression.parseVariantNbt(variantRaw));
    }

    private static <T> void enqueuePendingPacket(ArrayList<T> list, T packet) {
        if (list.size() >= MAX_PENDING_PACKET_QUEUE_SIZE) {
            list.remove(0);
        }
        list.add(packet);
    }

    private static void renderIdentityCooldown(DrawContext matrices, RenderTickCounter deltax) {
        float delta = deltax.getTickProgress(false);
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        Window window = client.getWindow();
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            return;
        }

        Registry<IdentityAbilityDefinition> identityAbilityRegistry = ModRegistries.getIdentityAbilityRegistry();
        if (identityAbilityRegistry == null) {
            return;
        }

        IdentityAbilityDefinition identityAbility = identityAbilityRegistry.get(net.minecraft.entity.EntityType.getId(identity.getType()));
        if (identityAbility == null) {
            return;
        }

        if (client.currentScreen instanceof ChatScreen) {
            return;
        }

        double d = client.getWindow().getScaleFactor();
        int cd = ((EntityAccessor) player).getAbilityCooldown();
        int max = identityAbility.cooldown() + identityAbility.useduration();
        float cooldownScale = 1 - cd / (float) max;

        if (cd == lastCooldown) {
            ticksSinceUpdate++;
            if (ticksSinceUpdate > fadingTickRequirement && !isFading) {
                isFading = true;
                fadingProgress = 0;
            }
        } else if (ticksSinceUpdate > fadingProgress) {
            ticksSinceUpdate = 0;
            isFading = false;
        }

        if (isFading) {
            fadingProgress = Math.min(50, fadingProgress + 1);
        } else {
            fadingProgress = Math.max(0, fadingProgress - 1);
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int iconwidth = 17;

        matrices.getMatrices().pushMatrix();
        if (cooldownScale != 1) {
            matrices.enableScissor(
                (int) (0d * d),
                (int) ((double) height * .92 + iconwidth * (1 - cooldownScale)),
                (int) ((double) width * d),
                (int) ((double) height * d)
            );
        }

        if (isFading && cooldownScale == 1) {
            float fadeScalar = fadingProgress / 50f;
            float scale = 1f + (float) Math.sin(fadeScalar * 1.5 * Math.PI) - .25f;
            scale = Math.max(scale, 0.01F);
            matrices.getMatrices().scaleAround(scale, (int) (width * .95f + iconwidth * .5f), (int) (height * .92f + iconwidth * .5f));
        }

        ItemStack stack = new ItemStack(identityAbility.icon());
        matrices.drawItem(stack, (int) (width * .95f), (int) (height * .92f));

        if (cooldownScale != 1) {
            matrices.disableScissor();
        }

        matrices.getMatrices().popMatrix();

        lastCooldown = Math.round(MathHelper.lerp(delta, cd - 1, cd));
    }

    private static Field getFieldFromClassHeirarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy.");
    }

    public static EntityModel getModel(Entity e) {
        EntityRenderer idrenderer = ((MinecraftClientAccessor) MinecraftClient.getInstance()).getEntityRenderManager().getRenderer(e);

        EntityModel eModel = null;
        if (idrenderer instanceof LivingEntityRenderer) {
            try {
                eModel = ((LivingEntityRenderer) idrenderer).getModel();
            } catch (Exception f) {
                try {
                    eModel = (EntityModel) getFieldFromClassHeirarchy(eModel.getClass(), "model").get((Object) eModel);
                } catch (Exception g) {
                    int x = 0;
                }
            }
        }
        if (idrenderer instanceof EnderDragonEntityRenderer) {
            eModel = ((EnderDragonEntityRendererAccessor) (EnderDragonEntityRenderer) idrenderer).getModel();
        }
        return eModel;
    }
}

