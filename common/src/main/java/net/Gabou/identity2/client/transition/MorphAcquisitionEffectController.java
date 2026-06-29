package net.Gabou.identity2.client.transition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.Gabou.identity2.packets.MorphAcquisitionS2CPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class MorphAcquisitionEffectController {
    private static final List<AcquisitionEffect> EFFECTS = new ArrayList<>();
    private static final int TENDRIL_SEGMENTS = 6;
    private static final int BASE_PARTICLES_PER_TICK = 3;
    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);
    private static final Vec3 WORLD_EAST = new Vec3(1.0D, 0.0D, 0.0D);

    private MorphAcquisitionEffectController() {
    }

    public static void enqueue(MorphAcquisitionS2CPacketPayload payload) {
        if (!net.Gabou.identity2.IdentitySettings.enableMorphAcquisitionTendrils) {
            return;
        }
        int maxTicks = Math.max(6, net.Gabou.identity2.IdentitySettings.morphAcquisitionAnimationTicks);
        EFFECTS.add(
            new AcquisitionEffect(
                payload.originEntityId(),
                payload.acquiredEntityId(),
                new Vec3(payload.acquiredX(), payload.acquiredY(), payload.acquiredZ()),
                maxTicks,
                payload.morphAcquisition()
            )
        );
    }

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || client.isPaused() || EFFECTS.isEmpty()) {
            return;
        }

        Iterator<AcquisitionEffect> iterator = EFFECTS.iterator();
        while (iterator.hasNext()) {
            AcquisitionEffect effect = iterator.next();
            if (effect.tickAndRender(level)) {
                iterator.remove();
            }
        }
    }

    private static final class AcquisitionEffect {
        private final int originEntityId;
        private final int acquiredEntityId;
        private final Vec3 fallbackAcquiredPos;
        private final int maxTicks;
        private final boolean morphAcquisition;
        private int age = 0;

        private AcquisitionEffect(int originEntityId, int acquiredEntityId, Vec3 fallbackAcquiredPos, int maxTicks, boolean morphAcquisition) {
            this.originEntityId = originEntityId;
            this.acquiredEntityId = acquiredEntityId;
            this.fallbackAcquiredPos = fallbackAcquiredPos;
            this.maxTicks = maxTicks;
            this.morphAcquisition = morphAcquisition;
        }

        private boolean tickAndRender(ClientLevel level) {
            Entity origin = level.getEntity(originEntityId);
            if (origin == null || !origin.isAlive()) {
                return true;
            }

            Entity acquired = level.getEntity(acquiredEntityId);
            Vec3 originPos = centered(origin);
            Vec3 acquiredPos = acquired != null ? centered(acquired) : fallbackAcquiredPos;
            if (acquiredPos == null) {
                return true;
            }

            float lifeProgress = Mth.clamp(age / (float) maxTicks, 0.0F, 1.0F);
            int particlesPerTick = BASE_PARTICLES_PER_TICK + (morphAcquisition ? 2 : 1);
            for (int i = 0; i < particlesPerTick; i++) {
                double segment = (i + level.random.nextDouble()) / (double) TENDRIL_SEGMENTS;
                Vec3 point = bezierPoint(acquiredPos, originPos, segment, lifeProgress, age * 0.2D + i);
                Vec3 velocity = originPos.subtract(point).scale(0.04D + level.random.nextDouble() * 0.03D);
                level.addParticle(ParticleTypes.WITCH, point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
            }

            if (age < Math.min(8, maxTicks / 2)) {
                for (int i = 0; i < 2; i++) {
                    Vec3 poof = acquiredPos.add(
                        (level.random.nextDouble() - 0.5D) * 0.35D,
                        (level.random.nextDouble() - 0.5D) * 0.35D,
                        (level.random.nextDouble() - 0.5D) * 0.35D
                    );
                    level.addParticle(ParticleTypes.POOF, poof.x, poof.y, poof.z, 0.0D, 0.03D, 0.0D);
                }
            }

            age++;
            return age >= maxTicks;
        }

        private static Vec3 centered(Entity entity) {
            return new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
        }

        private static Vec3 bezierPoint(Vec3 start, Vec3 end, double t, float lifeProgress, double phase) {
            Vec3 line = end.subtract(start);
            Vec3 dir = line.lengthSqr() > 1.0E-7D ? line.normalize() : WORLD_UP;
            Vec3 side = dir.cross(WORLD_UP);
            if (side.lengthSqr() < 1.0E-5D) {
                side = dir.cross(WORLD_EAST);
            }
            if (side.lengthSqr() < 1.0E-5D) {
                side = WORLD_UP;
            } else {
                side = side.normalize();
            }

            double arcStrength = (0.28D + 0.22D * Math.sin(phase)) * (1.0D - lifeProgress * 0.65D);
            Vec3 control = start.add(line.scale(0.5D))
                .add(side.scale(arcStrength))
                .add(0.0D, 0.18D * Math.cos(phase * 0.7D), 0.0D);

            double inv = 1.0D - t;
            return start.scale(inv * inv)
                .add(control.scale(2.0D * inv * t))
                .add(end.scale(t * t));
        }
    }
}
