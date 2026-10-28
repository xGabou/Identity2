package net.Gabou.identity2.testadapter.minecraft;

import com.mojang.brigadier.StringReader;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.client.identity.IdentityVariantDiscovery;
import net.Gabou.identity2.client.screen.IdentitySelectionScreen;
import net.Gabou.identity2.client.screen.IdentityVariantSelectionScreen;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityVariantNbtHelper;
import net.Gabou.identity2.testadapter.IdentityTestAdapter;
import net.Gabou.identity2.util.EnderDragonEntityRendererAccessor;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * All Minecraft 26.3 and Identity2 integration for the test boundary lives here.
 * No Fabric/NeoForge event dependency: a loader bootstrap only needs to construct this
 * adapter. Polling, deadlines, and capture are managed without blocking either game thread.
 * This source set is not included in released mod jars.
 */
public final class Minecraft26_3TestAdapter implements IdentityTestAdapter {
    private final Session session;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "identity-test-timer"));
    private final ExecutorService imageWriter = Executors.newSingleThreadExecutor(r -> daemon(r, "identity-test-images"));
    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
    private final Map<UUID, EntityRef> spawned = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean recording = new AtomicBoolean();
    private final CompletableFuture<Void> shutdown = new CompletableFuture<>();
    private volatile MinecraftServer attachedServer;
    private volatile UUID playerId;
    // Accessed only on the client thread.
    private boolean preferencesCaptured;
    private boolean originalPause;
    private CameraType originalCamera;
    private boolean touchedInput;
    private boolean touchedItemUse;

    public Minecraft26_3TestAdapter(Session session) { this.session = java.util.Objects.requireNonNull(session); }

    @Override public CompletionStage<Status> status() {
        return client(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui == null) throw error(Failure.NOT_READY, "Minecraft client is starting");
            MinecraftServer server = mc.getSingleplayerServer();
            boolean expected = server != null && sameWorld(server);
            return new Status("26.3", server != null, expected, expected && mc.player != null && mc.level != null,
                mc.level == null ? "" : mc.level.dimension().identifier().toString(),
                mc.gui.screen() == null ? "" : mc.gui.screen().getClass().getSimpleName());
        });
    }

    @Override public CompletionStage<Snapshot> snapshot() {
        return server(context -> Optional.ofNullable(context.playerOrNull()).map(p -> playerState(p, Side.SERVER)))
            .thenCompose(serverState -> client(() -> {
                Minecraft mc = requireClientWorld();
                Optional<PlayerState> clientState = Optional.ofNullable(mc.player).map(p -> playerState(p, Side.CLIENT));
                Optional<AnimationState> animation = mc.player == null ? Optional.empty() : Optional.of(animationState(mc));
                return new Snapshot(serverState, clientState, animation);
            }));
    }

    @Override public CompletionStage<Snapshot> awaitState(Predicate<Snapshot> condition, Duration timeout) {
        java.util.Objects.requireNonNull(condition);
        if (timeout == null || timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofMinutes(2)) > 0)
            return failed(Failure.INVALID_ARGUMENT, "Wait timeout must be positive and at most two minutes");
        CompletableFuture<Snapshot> result = tracked(timeout);
        poll(condition, result);
        return result;
    }

    private void poll(Predicate<Snapshot> condition, CompletableFuture<Snapshot> result) {
        if (result.isDone()) return;
        snapshot().whenComplete((value, error) -> {
            if (result.isDone()) return;
            if (error != null) {
                Throwable cause = unwrap(error);
                if (!(cause instanceof AdapterException e) || e.failure() != Failure.NOT_READY) {
                    result.completeExceptionally(cause); return;
                }
            } else {
                try { if (condition.test(value)) { result.complete(value); return; } }
                catch (Throwable failure) { result.completeExceptionally(failure); return; }
            }
            schedule(() -> poll(condition, result), 100, result);
        });
    }

    @Override public CompletionStage<Void> openWorldCreationScreen() {
        return client(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null || !(mc.gui.screen() instanceof TitleScreen))
                throw error(Failure.NOT_READY, "World creation requires the title screen and no loaded world");
            CreateWorldScreen.openFresh(mc, () -> mc.gui.setScreen(new TitleScreen()));
            return null;
        });
    }

    @Override public CompletionStage<Integer> buildPlatform(Platform spec) {
        return server(context -> {
            ServerLevel level = level(context.server(), spec.dimensionId());
            BlockState floor = block(spec.floorBlockId()), fill = block(spec.fillBlockId());
            int changed = 0;
            for (int x = spec.x(); x < spec.x() + spec.width(); x++) for (int z = spec.z(); z < spec.z() + spec.length(); z++) {
                if (level.setBlock(new BlockPos(x, spec.y(), z), floor, 2)) changed++;
                for (int y = spec.y() + 1; y <= spec.y() + spec.clearHeight(); y++)
                    if (level.setBlock(new BlockPos(x, y, z), fill, 2)) changed++;
            }
            return changed;
        });
    }

    @Override public CompletionStage<EntityRef> spawn(Spawn spec) {
        return server(context -> {
            ServerLevel level = level(context.server(), spec.dimensionId());
            Identifier id = id(spec.typeId());
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) throw error(Failure.INVALID_ARGUMENT, "Unknown entity: " + id);
            Entity created = BuiltInRegistries.ENTITY_TYPE.getValue(id).create(level, EntitySpawnReason.COMMAND);
            if (!(created instanceof LivingEntity living)) {
                if (created != null) created.discard();
                throw error(Failure.INVALID_ARGUMENT, "The test spawn must have a living entity factory: " + id);
            }
            try {
                IdentityVariantNbtHelper.applyVariantData(living, variant(spec.variantSnbt()));
                living.setPos(vector(spec.position()));
                if (living instanceof Mob mob) mob.setNoAi(spec.noAi());
                if (spec.health() > living.getMaxHealth()) {
                    var health = living.getAttribute(Attributes.MAX_HEALTH);
                    if (health == null) throw error(Failure.INVALID_ARGUMENT, "Entity has no max-health attribute");
                    health.setBaseValue(spec.health());
                }
                living.setHealth((float) spec.health());
                if (Math.abs(living.getHealth() - spec.health()) > .01) throw error(Failure.INVALID_ARGUMENT, "Requested health was clamped");
                if (!level.addFreshEntity(living)) throw error(Failure.INVALID_ARGUMENT, "World rejected test entity");
                EntityRef ref = reference(living); spawned.put(ref.uuid(), ref); return ref;
            } catch (Throwable failure) { living.discard(); throw failure; }
        });
    }

    @Override public CompletionStage<Void> removeSpawned(EntityRef ref) {
        return server(context -> {
            if (!ref.equals(spawned.get(ref.uuid()))) throw error(Failure.INVALID_ARGUMENT, "Entity was not spawned by this adapter");
            Entity entity = level(context.server(), ref.dimensionId()).getEntity(ref.uuid());
            if (entity != null) entity.discard();
            spawned.remove(ref.uuid()); return null;
        });
    }

    @Override public CompletionStage<Attack> attack(EntityRef ref) {
        return server(context -> {
            ServerPlayer player = context.player();
            Entity target = level(context.server(), ref.dimensionId()).getEntity(ref.uuid());
            if (!(target instanceof LivingEntity living)) throw error(Failure.ENTITY_MISSING, "Attack target is missing or not living");
            if (living.level() != player.level()) throw error(Failure.INVALID_ARGUMENT, "Attack target is in another dimension");
            if (!reference(living).equals(ref)) throw error(Failure.INVALID_ARGUMENT, "Entity reference does not match the target");
            double before = living.getHealth();
            player.attack(living);
            return new Attack(before, living.getHealth(), living.isDeadOrDying());
        });
    }

    @Override public CompletionStage<Void> setPlayerMode(PlayerMode mode) {
        return server(context -> { context.player().setGameMode(GameType.valueOf(mode.name())); return null; });
    }

    @Override public CompletionStage<Void> setVitalsForSetup(double fraction, int food) {
        if (!Double.isFinite(fraction) || fraction <= 0 || fraction > 1 || food < 0 || food > 20)
            return failed(Failure.INVALID_ARGUMENT, "Health fraction must be (0,1], food must be 0–20");
        return server(context -> {
            ServerPlayer player = context.player(); player.setHealth((float) (player.getMaxHealth() * fraction));
            player.getFoodData().setFoodLevel(food); player.clearFire(); return null;
        });
    }

    @Override public CompletionStage<Void> equip(Item main, Item off) {
        return server(context -> {
            // Validate both stacks before changing either slot.
            ItemStack mainStack = stack(main), offStack = stack(off);
            context.player().setItemInHand(InteractionHand.MAIN_HAND, mainStack);
            context.player().setItemInHand(InteractionHand.OFF_HAND, offStack); return null;
        });
    }

    @Override public CompletionStage<Void> teleport(Vec position) {
        return server(context -> { context.player().teleportTo(position.x(), position.y(), position.z()); return null; });
    }

    @Override public CompletionStage<Void> changeDimension(String dimension, Vec position) {
        return server(context -> {
            ServerPlayer player = context.player(); ServerLevel destination = level(context.server(), dimension);
            player.teleport(new TeleportTransition(destination, vector(position), Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
            return null;
        });
    }

    @Override public CompletionStage<Void> setTimeForSetup(String marker) {
        if (marker == null || !marker.matches("[a-z0-9_:]+")) return failed(Failure.INVALID_ARGUMENT, "Invalid time marker/tick value");
        return server(context -> {
            context.server().getCommands().getDispatcher().execute("time set " + marker, context.server().createCommandSourceStack().withSuppressedOutput());
            return null;
        });
    }

    @Override public CompletionStage<Void> setFlying(boolean flying) {
        return client(() -> {
            Minecraft mc = requireClientPlayer();
            if (flying && !mc.player.getAbilities().mayfly) throw error(Failure.INVALID_ARGUMENT, "The player has not been granted flight");
            mc.player.getAbilities().flying = flying; mc.player.onUpdateAbilities(); return null;
        });
    }

    @Override public CompletionStage<Void> setCamera(Camera camera) {
        return client(() -> {
            Minecraft mc = requireClientPlayer(); rememberPreferences(mc);
            mc.options.setCameraType(CameraType.valueOf(camera.name())); return null;
        });
    }

    @Override public CompletionStage<Void> setInput(Input input) {
        return client(() -> {
            Minecraft mc = requireClientPlayer(); rememberPreferences(mc); touchedInput = true;
            mc.options.pauseOnLostFocus = false; input(mc, input); return null;
        });
    }

    @Override public CompletionStage<Void> useItem(Hand hand) {
        return client(() -> { Minecraft mc = requireClientPlayer(); touchedItemUse = true; mc.gameMode.useItem(mc.player, hand(hand)); return null; });
    }
    @Override public CompletionStage<Void> releaseItem() {
        return client(() -> { Minecraft mc = requireClientPlayer(); mc.gameMode.releaseUsingItem(mc.player); touchedItemUse = false; return null; });
    }
    @Override public CompletionStage<Void> swing(Hand hand) {
        return client(() -> { requireClientPlayer().player.swing(hand(hand), SwingAnimation.DEFAULT, true); return null; });
    }

    @Override public CompletionStage<Void> morphForSetup(String identity, String data) {
        return server(context -> {
            if (!IdentityProgression.morph(context.player(), id(identity), variant(data))) throw error(Failure.INVALID_ARGUMENT, "Morph setup was rejected: " + identity);
            return null;
        });
    }
    @Override public CompletionStage<Void> requestMorph(String identity, String data) {
        return client(() -> {
            requireClientPlayer(); variant(data);
            if (!identity.isBlank()) id(identity);
            Identity2Client.sendMorphRequest(identity, data); return null;
        });
    }
    @Override public CompletionStage<Void> clearMorphForSetup() {
        return server(context -> { IdentityProgression.clearMorph(context.player()); return null; });
    }
    @Override public CompletionStage<Void> resetUnlocksForSetup() {
        return server(context -> { IdentityProgression.clearUnlockedIdentities(context.player()); IdentityProgression.syncUnlockedIdentities(context.player()); return null; });
    }

    @Override public CompletionStage<List<Variant>> variants(String identity) {
        return client(() -> {
            Minecraft mc = requireClientPlayer(); Identifier key = id(identity);
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(key)) throw error(Failure.INVALID_ARGUMENT, "Unknown identity: " + identity);
            CompoundTag data = customData(mc.player);
            boolean identityUnlocked = IdentityProgression.readUnlockedIdentityIdSet(data).contains(identity);
            Set<String> tokens = IdentityProgression.readUnlockedIdentityVariantTokenSet(data).get(identity);
            boolean wildcard = !IdentityProgression.shouldEnforceIdentityUnlocksForMorph() || IdentitySettings.unlockAllVariantsOnFirstUnlock;
            return IdentityVariantDiscovery.discover(BuiltInRegistries.ENTITY_TYPE.getValue(key), mc.level).stream().map(v ->
                new Variant(v.displayName(), IdentityProgression.serializeVariantNbt(v.variantNbt()), wildcard || (identityUnlocked &&
                    (tokens == null || tokens.stream().anyMatch(token -> IdentityProgression.matchesStoredVariantToken(v.variantNbt(), token)))))).toList();
        });
    }

    @Override public CompletionStage<Void> openMorphMenu() {
        return client(() -> { Minecraft mc = requireClientPlayer(); mc.gui.setScreen(new IdentitySelectionScreen()); return null; });
    }
    @Override public CompletionStage<Void> scrollMorphMenu(int rows) {
        if (rows < -512 || rows > 512) return failed(Failure.INVALID_ARGUMENT, "At most 512 scroll steps per call");
        return client(() -> {
            Minecraft mc = requireClientPlayer(); var screen = mc.gui.screen();
            if (!(screen instanceof IdentitySelectionScreen) && !(screen instanceof IdentityVariantSelectionScreen))
                throw error(Failure.INVALID_ARGUMENT, "The morph/variant menu is not open");
            for (int i = 0; i < Math.abs(rows); i++) screen.mouseScrolled(screen.width / 2.0, screen.height / 2.0, 0, rows > 0 ? -1 : 1);
            return null;
        });
    }
    @Override public CompletionStage<Void> closeScreen() {
        return client(() -> { requireClientPlayer().gui.setScreen(null); return null; });
    }

    @Override public CompletionStage<Path> screenshot(String label) {
        try { IdentityTestAdapter.artifactLabel(label); }
        catch (IllegalArgumentException e) { return failed(Failure.INVALID_ARGUMENT, e.getMessage()); }
        return screenshotTo(session.artifactDirectory().resolve(label + "-" + UUID.randomUUID() + ".png")).thenApply(Captured::path);
    }

    private CompletableFuture<Captured> screenshotTo(Path target) {
        CompletableFuture<Captured> result = tracked(session.requestTimeout());
        if (result.isDone()) return result;
        try {
            imageWriter.execute(() -> {
                try {
                    Files.createDirectories(target.getParent());
                    Minecraft.getInstance().execute(() -> {
                        if (result.isDone()) return;
                        try {
                            Minecraft mc = requireClientPlayer();
                            long capturedAt = System.nanoTime();
                            Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), pixels -> {
                                try {
                                    imageWriter.execute(() -> {
                                        try (pixels) { if (!result.isDone()) { pixels.writeToFile(target); result.complete(new Captured(target, capturedAt)); } }
                                        catch (Throwable error) { result.completeExceptionally(new AdapterException(Failure.CAPTURE_FAILED, "Cannot save screenshot", error)); }
                                    });
                                } catch (RejectedExecutionException stopped) { pixels.close(); result.completeExceptionally(error(Failure.CLOSED, "Capture writer stopped")); }
                            });
                        } catch (Throwable error) { result.completeExceptionally(error); }
                    });
                } catch (Throwable error) { result.completeExceptionally(new AdapterException(Failure.CAPTURE_FAILED, "Cannot prepare screenshot", error)); }
            });
        } catch (RejectedExecutionException stopped) { result.completeExceptionally(error(Failure.CLOSED, "Capture writer stopped")); }
        return result;
    }

    @Override public CompletionStage<FrameSequence> captureFrames(Capture capture) {
        if (!recording.compareAndSet(false, true)) return failed(Failure.INVALID_ARGUMENT, "A frame capture is already active");
        CompletableFuture<FrameSequence> result = tracked(capture.duration().plus(session.requestTimeout()));
        result.whenComplete((value, error) -> recording.set(false));
        Path folder = session.artifactDirectory().resolve(capture.label() + "-" + UUID.randomUUID());
        int count = (int) Math.ceil(capture.duration().toNanos() / 1_000_000_000.0 * capture.framesPerSecond());
        captureNext(capture, folder, count, System.nanoTime(), new ArrayList<>(), result);
        return result;
    }

    private void captureNext(Capture spec, Path folder, int count, long start, List<Frame> frames, CompletableFuture<FrameSequence> result) {
        if (result.isDone()) return;
        Path file = folder.resolve(String.format(java.util.Locale.ROOT, "frame-%05d.png", frames.size()));
        screenshotTo(file).whenComplete((captured, failure) -> {
            if (result.isDone()) return;
            if (failure != null) { result.completeExceptionally(unwrap(failure)); return; }
            frames.add(new Frame(captured.path(), captured.captureNanos() - start));
            if (frames.size() == count) { result.complete(new FrameSequence(spec.framesPerSecond(), frames)); return; }
            long due = frames.size() * 1_000_000_000L / spec.framesPerSecond();
            schedule(() -> captureNext(spec, folder, count, start, frames, result), Math.max(1, (due - (System.nanoTime() - start)) / 1_000_000), result);
        });
    }

    @Override public CompletionStage<Void> shutdown() {
        if (!closed.compareAndSet(false, true)) return shutdown;
        pending.forEach(future -> future.completeExceptionally(error(Failure.CLOSED, "Test adapter closed")));
        CompletableFuture<Void> clientCleanup = new CompletableFuture<>(), serverCleanup = new CompletableFuture<>();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) clientCleanup.complete(null);
        else minecraft.execute(() -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (touchedInput) input(mc, Input.NONE);
                if ((touchedItemUse || touchedInput) && mc.player != null && mc.gameMode != null
                    && mc.getSingleplayerServer() != null && sameWorld(mc.getSingleplayerServer())) mc.gameMode.releaseUsingItem(mc.player);
                if (preferencesCaptured) { mc.options.setCameraType(originalCamera); mc.options.pauseOnLostFocus = originalPause; }
                clientCleanup.complete(null);
            } catch (Throwable error) { clientCleanup.completeExceptionally(error); }
        });
        MinecraftServer server = attachedServer;
        if (server == null || !sameWorld(server)) serverCleanup.complete(null);
        else server.execute(() -> {
            try {
                for (EntityRef ref : spawned.values()) {
                    Entity entity = level(server, ref.dimensionId()).getEntity(ref.uuid());
                    if (entity != null) entity.discard();
                }
                serverCleanup.complete(null);
            } catch (Throwable error) { serverCleanup.completeExceptionally(error); }
        });
        CompletableFuture.allOf(clientCleanup, serverCleanup).orTimeout(session.requestTimeout().toMillis(), TimeUnit.MILLISECONDS).whenComplete((unused, failure) -> {
            spawned.clear(); timer.shutdownNow(); imageWriter.shutdown();
            if (failure == null) shutdown.complete(null); else shutdown.completeExceptionally(unwrap(failure));
        });
        return shutdown;
    }

    private PlayerState playerState(Player player, Side side) {
        CompoundTag data = customData(player);
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        String selected = data.getStringOr(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        Entity renderedIdentity = identity == null && IdentityProgression.PLAYER_IDENTITY_ID.toString().equals(selected) ? player : identity;
        Optional<Morph> morph = renderedIdentity == null ? Optional.empty() : Optional.of(new Morph(EntityType.getKey(renderedIdentity.getType()).toString(),
            data.getStringOr(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, ""), renderedIdentity instanceof LivingEntity living && living.isBaby(), renderedIdentity.getBbWidth(), renderedIdentity.getBbHeight()));
        Map<String, List<String>> variants = new LinkedHashMap<>();
        IdentityProgression.readUnlockedIdentityVariantTokenSet(data).forEach((key, tokens) -> variants.put(key, tokens.stream()
            .map(IdentityProgression::fromVariantUnlockToken).map(IdentityProgression::serializeVariantNbt).toList()));
        return new PlayerState(side, player.getUUID(), player.level().dimension().identifier().toString(), vector(player.position()), vector(player.getDeltaMovement()),
            player.getHealth(), player.getMaxHealth(), player.getEyeHeight(), player.getBbWidth(), player.getBbHeight(), player.isAlive(), player.onGround(),
            player.isInWater(), player.isSwimming(), player.getAbilities().mayfly, player.getAbilities().flying, player.isUsingItem(),
            player.isUsingItem() ? (player.getUsedItemHand() == InteractionHand.MAIN_HAND ? "MAIN" : "OFF") : "",
            itemId(player.getMainHandItem()), itemId(player.getOffhandItem()), selected, morph,
            new ArrayList<>(IdentityProgression.readUnlockedIdentityIdSet(data)), variants);
    }

    private AnimationState animationState(Minecraft mc) throws IllegalAccessException {
        var dispatcher = ((MinecraftClientAccessor) mc).getEntityRenderManager();
        Entity identity = ((EntityAccessor) mc.player).getCurrentIdentity();
        EntityRenderer renderer = dispatcher.getRenderer(identity == null ? mc.player : identity);
        EntityModel model = renderer instanceof LivingEntityRenderer living ? living.getModel()
            : renderer instanceof EnderDragonRenderer dragon ? ((EnderDragonEntityRendererAccessor) dragon).getModel() : null;
        Map<String, Vec> joints = new LinkedHashMap<>();
        // Read the last rendered model pose before extracting another render state.
        if (model != null) for (String part : List.of(PartNames.LEFT_WING, PartNames.RIGHT_WING, PartNames.LEFT_WING_BASE, PartNames.RIGHT_WING_BASE,
                PartNames.LEFT_ARM, PartNames.RIGHT_ARM, PartNames.LEFT_LEG, PartNames.RIGHT_LEG)) {
            ModelPart joint = model.root().createPartLookup().apply(part);
            if (joint != null) joints.put(part, new Vec(joint.xRot, joint.yRot, joint.zRot));
        }
        EntityRenderState render = dispatcher.extractEntity(mc.player, .5F);
        Map<String, Double> numbers = new LinkedHashMap<>(); Map<String, Boolean> flags = new LinkedHashMap<>(); Map<String, String> labels = new LinkedHashMap<>();
        for (var field : render.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            Object value = field.get(render);
            if (value instanceof Number n) numbers.put(field.getName(), n.doubleValue());
            else if (value instanceof Boolean flag) flags.put(field.getName(), flag);
            else if (value instanceof Enum<?> e) labels.put(field.getName(), e.name());
            else if (value instanceof String s) labels.put(field.getName(), s);
            else if (value instanceof net.minecraft.world.entity.AnimationState state) {
                flags.put(field.getName() + ".started", state.isStarted());
                numbers.put(field.getName() + ".elapsedMillis", (double) state.getTimeInMillis(render.ageInTicks));
            }
        }
        return new AnimationState(render.getClass().getSimpleName(), numbers, flags, labels, joints);
    }

    private CompletableFuture<Context> context() {
        return client(() -> {
            Minecraft mc = requireClientWorld(); MinecraftServer server = mc.getSingleplayerServer();
            if (mc.player != null) playerId = mc.player.getUUID();
            if (playerId == null) throw error(Failure.NOT_READY, "Local player has not joined yet");
            attachedServer = server; return new Context(server, playerId);
        });
    }

    private <T> CompletableFuture<T> server(ServerCall<T> operation) {
        CompletableFuture<T> result = tracked(session.requestTimeout());
        if (result.isDone()) return result;
        context().whenComplete((context, failure) -> {
            if (failure != null) { result.completeExceptionally(unwrap(failure)); return; }
            context.server().execute(() -> {
                // Expired/closed requests must never mutate the world after their deadline.
                if (result.isDone()) return;
                try {
                    if (context.server() != attachedServer || !sameWorld(context.server())) throw error(Failure.WRONG_WORLD, "Test world changed");
                    result.complete(operation.call(context));
                } catch (Throwable error) { result.completeExceptionally(error); }
            });
        });
        return result;
    }

    private <T> CompletableFuture<T> client(Call<T> operation) {
        CompletableFuture<T> result = tracked(session.requestTimeout());
        if (!result.isDone()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) result.completeExceptionally(error(Failure.NOT_READY, "Minecraft client is not initialized"));
            else mc.execute(() -> {
                if (result.isDone()) return;
                try { result.complete(operation.call()); } catch (Throwable error) { result.completeExceptionally(error); }
            });
        }
        return result;
    }

    private <T> CompletableFuture<T> tracked(Duration timeout) {
        CompletableFuture<T> result = new CompletableFuture<>();
        if (closed.get()) { result.completeExceptionally(error(Failure.CLOSED, "Test adapter is closed")); return result; }
        pending.add(result);
        try {
            var deadline = timer.schedule(() -> result.completeExceptionally(error(Failure.TIMEOUT, "Test adapter operation timed out after " + timeout)), timeout.toNanos(), TimeUnit.NANOSECONDS);
            result.whenComplete((value, error) -> { pending.remove(result); deadline.cancel(false); });
        } catch (RejectedExecutionException stopped) { pending.remove(result); result.completeExceptionally(error(Failure.CLOSED, "Test adapter is closing")); }
        return result;
    }

    private void schedule(Runnable action, long delayMillis, CompletableFuture<?> result) {
        try { if (!result.isDone()) timer.schedule(action, delayMillis, TimeUnit.MILLISECONDS); }
        catch (RejectedExecutionException stopped) { result.completeExceptionally(error(Failure.CLOSED, "Test adapter is closing")); }
    }

    private Minecraft requireClientWorld() {
        Minecraft mc = Minecraft.getInstance(); MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) throw error(Failure.NOT_READY, "An integrated test server is required");
        if (!sameWorld(server)) throw error(Failure.WRONG_WORLD, "Expected test world " + session.expectedWorldDirectory());
        return mc;
    }
    private Minecraft requireClientPlayer() {
        Minecraft mc = requireClientWorld();
        if (mc.player == null || mc.level == null || mc.gameMode == null) throw error(Failure.NOT_READY, "Client player is loading");
        return mc;
    }
    private boolean sameWorld(MinecraftServer server) {
        Path actual = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        if (actual.equals(session.expectedWorldDirectory())) return true;
        try { return Files.isSameFile(actual, session.expectedWorldDirectory()); } catch (java.io.IOException missing) { return false; }
    }
    private void rememberPreferences(Minecraft mc) {
        if (!preferencesCaptured) { preferencesCaptured = true; originalCamera = mc.options.getCameraType(); originalPause = mc.options.pauseOnLostFocus; }
    }
    private static void input(Minecraft mc, Input keys) {
        mc.options.keyUp.setDown(keys.forward()); mc.options.keyDown.setDown(keys.back());
        mc.options.keyLeft.setDown(keys.left()); mc.options.keyRight.setDown(keys.right());
        mc.options.keyJump.setDown(keys.jump()); mc.options.keyShift.setDown(keys.sneak());
        mc.options.keySprint.setDown(keys.sprint()); mc.options.keyAttack.setDown(keys.attack()); mc.options.keyUse.setDown(keys.use());
    }
    private static ServerLevel level(MinecraftServer server, String dimension) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id(dimension)));
        if (level == null) throw error(Failure.INVALID_ARGUMENT, "Unknown dimension: " + dimension);
        return level;
    }
    private static BlockState block(String name) {
        Identifier id = id(name);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) throw error(Failure.INVALID_ARGUMENT, "Unknown block: " + name);
        return BuiltInRegistries.BLOCK.getValue(id).defaultBlockState();
    }
    private static ItemStack stack(Item spec) {
        Identifier id = id(spec.id());
        if (!BuiltInRegistries.ITEM.containsKey(id)) throw error(Failure.INVALID_ARGUMENT, "Unknown item: " + spec.id());
        if (spec.count() == 0) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(BuiltInRegistries.ITEM.getValue(id), spec.count());
        if (result.isEmpty() || spec.count() > result.getMaxStackSize()) throw error(Failure.INVALID_ARGUMENT, "Invalid stack size for " + spec.id());
        return result;
    }
    private static Identifier id(String text) {
        try { return Identifier.parse(text); } catch (RuntimeException invalid) { throw new AdapterException(Failure.INVALID_ARGUMENT, "Invalid resource id: " + text, invalid); }
    }
    private static CompoundTag variant(String snbt) {
        if (snbt == null || snbt.length() > 1_048_576) throw error(Failure.INVALID_ARGUMENT, "Invalid/oversized variant data");
        if (snbt.isBlank()) return new CompoundTag();
        try {
            StringReader reader = new StringReader(snbt); CompoundTag data = CompoundTagArgument.compoundTag().parse(reader); reader.skipWhitespace();
            if (reader.canRead()) throw error(Failure.INVALID_ARGUMENT, "Trailing data after variant NBT");
            return data;
        } catch (AdapterException e) { throw e; }
        catch (Exception invalid) { throw new AdapterException(Failure.INVALID_ARGUMENT, "Invalid variant NBT", invalid); }
    }
    private static CompoundTag customData(Player player) { return ((NbtComponentAccessor) (Object) ((EntityAccessor) player).getCustomData()).getNbt(); }
    private static EntityRef reference(Entity entity) { return new EntityRef(entity.getUUID(), entity.level().dimension().identifier().toString(), EntityType.getKey(entity.getType()).toString()); }
    private static String itemId(ItemStack stack) { return stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    private static InteractionHand hand(Hand hand) { return hand == Hand.MAIN ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND; }
    private static Vec vector(Vec3 value) { return new Vec(value.x, value.y, value.z); }
    private static Vec3 vector(Vec value) { return new Vec3(value.x(), value.y(), value.z()); }
    private static Thread daemon(Runnable work, String name) { Thread thread = new Thread(work, name); thread.setDaemon(true); return thread; }
    private static AdapterException error(Failure failure, String text) { return new AdapterException(failure, text); }
    private static <T> CompletableFuture<T> failed(Failure failure, String text) { return CompletableFuture.failedFuture(error(failure, text)); }
    private static Throwable unwrap(Throwable error) { while (error instanceof CompletionException && error.getCause() != null) error = error.getCause(); return error; }
    @FunctionalInterface private interface Call<T> { T call() throws Exception; }
    @FunctionalInterface private interface ServerCall<T> { T call(Context context) throws Exception; }
    private record Context(MinecraftServer server, UUID playerId) {
        ServerPlayer playerOrNull() { return server.getPlayerList().getPlayer(playerId); }
        ServerPlayer player() { ServerPlayer player = playerOrNull(); if (player == null) throw error(Failure.NOT_READY, "Server player is loading"); return player; }
    }
    private record Captured(Path path, long captureNanos) {}
}
