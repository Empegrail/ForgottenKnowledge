package empegrail.forgotten_knowledge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NecroticDeathTracker {
    private static final Map<UUID, Integer> trackedEntities = new HashMap<>();

    public static void trackEntity(LivingEntity entity, int level) {
        trackedEntities.put(entity.getUuid(), level);
    }

    public static void onEntityDeath(LivingEntity entity) {
        Integer level = trackedEntities.remove(entity.getUuid());
        if (level != null && level >= 3) {
            // Entity died with necrotic effect level 3+
            spawnDisintegrationEffect(entity, level);
        }
    }

    private static void spawnDisintegrationEffect(LivingEntity entity, int level) {
        if (entity.getWorld() instanceof ServerWorld serverWorld) {
            int particleCount = 30 + (level * 10);

            for (int i = 0; i < particleCount; i++) {
                serverWorld.spawnParticles(
                        ParticleTypes.ASH,
                        entity.getX(),
                        entity.getY() + entity.getHeight() / 2,
                        entity.getZ(),
                        3, // count
                        0.5, 0.5, 0.5, // spread
                        0.1 // speed
                );

                serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        entity.getX(),
                        entity.getY() + entity.getHeight() / 2,
                        entity.getZ(),
                        2, // count
                        0.3, 0.3, 0.3, // spread
                        0.05 // speed
                );

                if (level >= 4) {
                    serverWorld.spawnParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            entity.getX(),
                            entity.getY() + entity.getHeight() / 2,
                            entity.getZ(),
                            1, // count
                            0.2, 0.2, 0.2, // spread
                            0.02 // speed
                    );
                }
            }

            // Play disintegration sound
            entity.getWorld().playSound(
                    null,
                    entity.getBlockPos(),
                    SoundEvents.ENTITY_WITHER_DEATH,
                    SoundCategory.HOSTILE,
                    0.7f, 1.2f
            );
        }
    }
}