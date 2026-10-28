package net.Gabou.identity2.testadapter;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static net.Gabou.identity2.testadapter.IdentityTestAdapter.*;

/** Runs with only the Java 17 API on its classpath, independent of Minecraft. */
public final class IdentityTestAdapterContractCheck {
    private static int checks;
    public static void main(String[] args) {
        rejects(() -> new Vec(Double.NaN, 0, 0));
        rejects(() -> new Vec(0, Double.POSITIVE_INFINITY, 0));
        rejects(() -> new Session(Path.of("test-world"), Path.of("artifacts"), Duration.ZERO));
        rejects(() -> new Session(Path.of("test-world"), Path.of("artifacts"), Duration.ofHours(1)));
        rejects(() -> new Platform("minecraft:overworld", 0, 200, 0, 64, 64, 32, "minecraft:stone", "minecraft:air"));
        rejects(() -> new Platform("minecraft:overworld", Integer.MAX_VALUE, 200, 0, 2, 2, 1, "minecraft:stone", "minecraft:air"));
        rejects(() -> new Capture("../outside", Duration.ofSeconds(1), 8));
        rejects(() -> new Capture("valid", Duration.ofSeconds(31), 8));
        rejects(() -> new Capture("valid", Duration.ofSeconds(1), 0));
        rejects(() -> new Item("minecraft:stone", -1));
        rejects(() -> new Spawn("minecraft:cow", "minecraft:overworld", new Vec(0, 200, 0), "", 0, true));
        Session session = new Session(Path.of("world/../test-world"), Path.of("artifacts"), Duration.ofSeconds(10));
        check(session.expectedWorldDirectory().isAbsolute() && !session.expectedWorldDirectory().toString().contains(".."), "normalized world path");
        Map<String, Double> source = new HashMap<>(); source.put("ageInTicks", 12d);
        AnimationState state = new AnimationState("BatRenderState", source, Map.of(), Map.of(), Map.of());
        source.put("ageInTicks", 40d);
        check(state.numbers().get("ageInTicks") == 12d, "snapshot copies mutable maps");
        try { state.numbers().put("x", 1d); throw new AssertionError("mutable snapshot"); }
        catch (UnsupportedOperationException expected) { checks++; }
        List<Frame> frames = new ArrayList<>(); frames.add(new Frame(Path.of("frame.png"), 0));
        FrameSequence sequence = new FrameSequence(8, frames); frames.clear();
        check(sequence.frames().size() == 1, "capture copies mutable frame list");
        check(!new Snapshot(java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()).ready(), "loading state is explicit");
        check(new Platform("minecraft:overworld", -10, 199, -10, 21, 21, 6, "minecraft:stone", "minecraft:air").width() == 21, "normal fixture accepted");
        System.out.println("Test adapter pure-Java contract: " + checks + " checks passed");
    }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException | ArithmeticException expected) { checks++; return; }
        throw new AssertionError("Invalid contract input was accepted");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
