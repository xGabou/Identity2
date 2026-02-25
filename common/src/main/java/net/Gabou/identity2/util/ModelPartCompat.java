package net.Gabou.identity2.util;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

import java.lang.reflect.Field;

public final class ModelPartCompat {

    private ModelPartCompat() {}

    public static ModelPart tryGetRoot(EntityModel<?> model) {
        if (model == null) return null;

        for (Class<?> c = model.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != ModelPart.class) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(model);
                    if (v instanceof ModelPart part) {
                        return part;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    public static ModelPart tryGetChild(ModelPart root, String name) {
        if (root == null || name == null || name.isBlank()) return null;
        try {
            if (!root.hasChild(name)) return null;
            return root.getChild(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
    public record PartSnapshot(boolean skipDraw, float xScale, float yScale, float zScale) {
        public static PartSnapshot capture(ModelPart part) {
            return new PartSnapshot(part.skipDraw, part.xScale, part.yScale, part.zScale);
        }

        public void restore(ModelPart part) {
            part.skipDraw = this.skipDraw;
            part.xScale = this.xScale;
            part.yScale = this.yScale;
            part.zScale = this.zScale;
        }
    }
}