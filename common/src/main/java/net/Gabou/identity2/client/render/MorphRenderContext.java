package net.Gabou.identity2.client.render;

import net.minecraft.world.entity.Entity;

public final class MorphRenderContext {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private MorphRenderContext() {
    }

    public static void begin(Entity source, Entity rendered, float tickDelta) {
        CURRENT.set(new Context(source, rendered, tickDelta));
    }

    public static void end() {
        CURRENT.remove();
    }

    public static Context current() {
        return CURRENT.get();
    }

    public record Context(Entity source, Entity rendered, float tickDelta) {
        public boolean matches(Entity entity) {
            return rendered == entity;
        }
    }
}
