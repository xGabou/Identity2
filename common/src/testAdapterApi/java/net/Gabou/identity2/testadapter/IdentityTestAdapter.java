package net.Gabou.identity2.testadapter;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * Version-independent boundary for a future in-game test runner.
 *
 * <p>No Minecraft, loader, NBT, render, or Identity2 objects cross this interface.
 * Implementations dispatch work to the appropriate game thread. Never join a returned
 * future on a game thread. Action completion means the action was dispatched/executed;
 * use {@link #awaitState} when the client and server must have finished synchronizing.
 *
 * <p>This is an adapter, not a test runner. Spawning/morph setup operations are
 * deliberately distinct from real attacks and normal client morph requests.
 *
 * <p>A portable scenario can receive this interface and compose actions without any
 * game imports, for example:
 * <pre>{@code
 * adapter.requestMorph("minecraft:cow", "{}")
 *     .thenCompose(ignored -> adapter.awaitState(state -> state.ready()
 *         && state.client().orElseThrow().morph()
 *             .map(morph -> morph.typeId().equals("minecraft:cow")).orElse(false),
 *         Duration.ofSeconds(10)));
 * }</pre>
 * <p>The development bootstrap constructs the version-specific implementation with
 * an exact disposable-world path. World setup changes (blocks, progression, time,
 * inventory and player mode) persist in that world; shutdown only performs the
 * cleanup documented below. Use a fresh fixture world for independent runs.
 */
public interface IdentityTestAdapter {
    enum Side { SERVER, CLIENT }
    enum Hand { MAIN, OFF }
    enum Camera { FIRST_PERSON, THIRD_PERSON_BACK, THIRD_PERSON_FRONT }
    enum PlayerMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }
    enum Failure { NOT_READY, WRONG_WORLD, ENTITY_MISSING, INVALID_ARGUMENT, CLOSED, TIMEOUT, CAPTURE_FAILED }

    final class AdapterException extends RuntimeException {
        private final Failure failure;
        public AdapterException(Failure failure, String message) { super(message); this.failure = Objects.requireNonNull(failure); }
        public AdapterException(Failure failure, String message, Throwable cause) { super(message, cause); this.failure = Objects.requireNonNull(failure); }
        public Failure failure() { return failure; }
    }

    /** The world path is exact, not a parent directory or wildcard. */
    record Session(Path expectedWorldDirectory, Path artifactDirectory, Duration requestTimeout) {
        public Session {
            expectedWorldDirectory = Objects.requireNonNull(expectedWorldDirectory).toAbsolutePath().normalize();
            artifactDirectory = Objects.requireNonNull(artifactDirectory).toAbsolutePath().normalize();
            positive(requestTimeout, "requestTimeout");
        }
    }

    record Vec(double x, double y, double z) {
        public Vec { finite(x, "x"); finite(y, "y"); finite(z, "z"); }
    }

    record EntityRef(UUID uuid, String dimensionId, String typeId) {
        public EntityRef { Objects.requireNonNull(uuid); text(dimensionId, "dimensionId"); text(typeId, "typeId"); }
    }

    record Status(String minecraftVersion, boolean integratedServer, boolean expectedWorld,
                  boolean clientReady, String dimensionId, String screen) {}

    record Morph(String typeId, String variantSnbt, boolean baby, double width, double height) {}

    record PlayerState(Side side, UUID uuid, String dimensionId, Vec position, Vec velocity,
                       double health, double maxHealth, double eyeHeight, double width, double height,
                       boolean alive, boolean onGround, boolean inWater, boolean swimming,
                       boolean mayFly, boolean flying, boolean usingItem, String usedHand,
                       String mainItem, String offItem, String selectedIdentityId, Optional<Morph> morph,
                       List<String> unlockedIdentities, Map<String, List<String>> unlockedVariants) {
        public PlayerState {
            Objects.requireNonNull(side); Objects.requireNonNull(uuid); Objects.requireNonNull(position); Objects.requireNonNull(velocity);
            morph = Objects.requireNonNull(morph);
            unlockedIdentities = List.copyOf(unlockedIdentities);
            unlockedVariants = unlockedVariants.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        }
    }

    /** Public render values and model-joint rotations, with only Java data in the result. */
    record AnimationState(String renderStateType, Map<String, Double> numbers,
                          Map<String, Boolean> flags, Map<String, String> labels, Map<String, Vec> jointRotations) {
        public AnimationState {
            numbers = Map.copyOf(numbers); flags = Map.copyOf(flags); labels = Map.copyOf(labels); jointRotations = Map.copyOf(jointRotations);
        }
    }

    /** A transition may temporarily have a server player but no client player/morph. */
    record Snapshot(Optional<PlayerState> server, Optional<PlayerState> client, Optional<AnimationState> animation) {
        public Snapshot { Objects.requireNonNull(server); Objects.requireNonNull(client); Objects.requireNonNull(animation); }
        public boolean ready() {
            return server.isPresent() && client.isPresent() && server.get().uuid().equals(client.get().uuid())
                && server.get().dimensionId().equals(client.get().dimensionId());
        }
    }

    /** variantSnbt is opaque adapter data; portable tests should obtain it from variants(), not construct NBT. */
    record Spawn(String typeId, String dimensionId, Vec position, String variantSnbt, double health, boolean noAi) {
        public Spawn {
            text(typeId, "typeId"); text(dimensionId, "dimensionId"); Objects.requireNonNull(position);
            variantSnbt = Objects.requireNonNull(variantSnbt); finite(health, "health");
            if (health <= 0) throw new IllegalArgumentException("health must be positive");
        }
    }

    record Attack(double healthBefore, double healthAfter, boolean killed) {}

    /** Floor at y; clear/fill the space above it. Limited to a small test fixture. */
    record Platform(String dimensionId, int x, int y, int z, int width, int length, int clearHeight,
                    String floorBlockId, String fillBlockId) {
        public Platform {
            text(dimensionId, "dimensionId"); text(floorBlockId, "floorBlockId"); text(fillBlockId, "fillBlockId");
            if (width < 1 || length < 1 || clearHeight < 1 || width > 64 || length > 64 || clearHeight > 32
                || (long) width * length * (clearHeight + 1) > 32768) throw new IllegalArgumentException("Platform exceeds fixture bounds");
            Math.addExact(x, width); Math.addExact(y, clearHeight); Math.addExact(z, length);
        }
    }

    record Item(String id, int count) {
        public static final Item EMPTY = new Item("minecraft:air", 0);
        public Item { text(id, "id"); if (count < 0 || count > 999) throw new IllegalArgumentException("Invalid item count"); }
    }

    record Input(boolean forward, boolean back, boolean left, boolean right, boolean jump,
                 boolean sneak, boolean sprint, boolean attack, boolean use) {
        public static final Input NONE = new Input(false, false, false, false, false, false, false, false, false);
    }

    record Variant(String name, String snbt, boolean unlocked) {}
    record Capture(String label, Duration duration, int framesPerSecond) {
        public Capture {
            artifactLabel(label); positive(duration, "duration");
            if (duration.compareTo(Duration.ofSeconds(30)) > 0 || framesPerSecond < 1 || framesPerSecond > 12)
                throw new IllegalArgumentException("Capture must be at most 30 seconds and 1–12 fps");
        }
    }
    record Frame(Path path, long elapsedNanos) {}
    /** PNG frames with real timestamps. Encoding them into video belongs to the future runner/reporting layer. */
    record FrameSequence(int requestedFramesPerSecond, List<Frame> frames) {
        public FrameSequence { frames = List.copyOf(frames); }
    }

    CompletionStage<Status> status();
    CompletionStage<Snapshot> snapshot();
    CompletionStage<Snapshot> awaitState(Predicate<Snapshot> condition, Duration timeout);
    default CompletionStage<Snapshot> awaitReady(Duration timeout) { return awaitState(Snapshot::ready, timeout); }

    /** Opens the world-creation UI; does not claim that a world has been created. */
    CompletionStage<Void> openWorldCreationScreen();
    CompletionStage<Integer> buildPlatform(Platform platform);
    CompletionStage<EntityRef> spawn(Spawn spawn);
    /** Removes only an entity created by this adapter instance. */
    CompletionStage<Void> removeSpawned(EntityRef entity);
    /** A real player attack, including the normal damage/death/unlock hooks. No forced cooldown reset. */
    CompletionStage<Attack> attack(EntityRef entity);
    CompletionStage<Void> setPlayerMode(PlayerMode mode);
    CompletionStage<Void> setVitalsForSetup(double healthFraction, int foodLevel);
    CompletionStage<Void> equip(Item main, Item off);
    CompletionStage<Void> teleport(Vec position);
    CompletionStage<Void> changeDimension(String dimensionId, Vec position);
    CompletionStage<Void> setTimeForSetup(String markerOrTicks);
    CompletionStage<Void> setFlying(boolean flying);
    CompletionStage<Void> setCamera(Camera camera);
    CompletionStage<Void> setInput(Input input);
    CompletionStage<Void> useItem(Hand hand);
    CompletionStage<Void> releaseItem();
    CompletionStage<Void> swing(Hand hand);

    CompletionStage<Void> morphForSetup(String identityId, String variantSnbt);
    /** Uses the same client request path as the morph menu; await a snapshot to check its outcome. */
    CompletionStage<Void> requestMorph(String identityId, String variantSnbt);
    CompletionStage<Void> clearMorphForSetup();
    CompletionStage<Void> resetUnlocksForSetup();
    CompletionStage<List<Variant>> variants(String identityId);
    CompletionStage<Void> openMorphMenu();
    CompletionStage<Void> scrollMorphMenu(int rows);
    CompletionStage<Void> closeScreen();

    CompletionStage<Path> screenshot(String label);
    CompletionStage<FrameSequence> captureFrames(Capture capture);
    /** Releases synthetic input, restores camera/pause preferences, and removes this adapter's spawned entities. */
    CompletionStage<Void> shutdown();

    private static void finite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
    private static void text(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
    private static void positive(Duration value, String name) {
        if (value == null || value.isNegative() || value.isZero() || value.compareTo(Duration.ofMinutes(2)) > 0)
            throw new IllegalArgumentException(name + " must be positive and at most two minutes");
    }
    static void artifactLabel(String label) {
        if (label == null || !label.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,79}"))
            throw new IllegalArgumentException("Artifact label must use 1–80 letters, numbers, underscores or hyphens");
    }
}
