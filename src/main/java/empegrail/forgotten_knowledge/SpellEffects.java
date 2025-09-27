package empegrail.forgotten_knowledge;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Small collection of example SpellEffect implementations.
 * Now includes level scaling for spell effects.
 */
public final class SpellEffects {
    private SpellEffects() {}

    // Helper: Get spell level from item stack
    private static int getSpellLevel(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SPELL_LEVEL, 1);
    }

    // Helper: Calculate scaling factor based on spell level
    private static float getScaleFactor(int level) {
        return 1.0f + (level - 1) * 0.5f; // Level 1: 1.0x, Level 2: 1.5x, Level 3: 2.0x, etc.
    }

    // Helper: Create a player attack DamageSource that works across mappings/versions
    private static DamageSource createPlayerAttackSource(ServerWorld world, PlayerEntity player) {
        try {
            return world.getDamageSources().playerAttack(player);
        } catch (Exception ignored) {
            return world.getDamageSources().generic();
        }
    }

    // Create a Lighting Strike Effect with level scaling
    public static final SpellEffect LIGHTNING_STRIKE = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);
        float scaleFactor = getScaleFactor(level);

        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        // Spawn multiple lightning bolts based on level
        for (int i = 0; i < level; i++) {
            LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            Vec3d offset = new Vec3d(
                    world.random.nextDouble() * 5.0 * scaleFactor - 2.5 * scaleFactor,
                    0,
                    world.random.nextDouble() * 5.0 * scaleFactor - 2.5 * scaleFactor
            );
            bolt.refreshPositionAfterTeleport(targetPos.toCenterPos().add(offset));
            world.spawnEntity(bolt);
        }

        return true;
    };

    // Create an Explosion Effect with level scaling
    public static final SpellEffect EXPLOSION = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);
        float scaleFactor = getScaleFactor(level);

        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        float power = 3.0f * scaleFactor;

        world.createExplosion(
                user,
                targetPos.getX() + 0.5,
                targetPos.getY() + 0.5,
                targetPos.getZ() + 0.5,
                power,
                false,
                World.ExplosionSourceType.TNT
        );

        return true;
    };


    // Create an Effect that makes objects vanish with dramatic buildup for high levels
    public static final SpellEffect VANISH_OBJECT = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);

        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        boolean vanishedAny = false;

        // New scaling system - simple and clean
        int size;
        if (level <= 4) {
            // Levels 1-4: linear scaling (1, 2, 3, 4)
            size = level;
        } else {
            // Levels 5+: exponential scaling (8, 16, 32, 64, etc.)
            size = (int) Math.pow(2, level - 2);
        }

        // Calculate the offset to make it dig into the surface
        int offset = size / 2;

        // For level 6 and above, create a dramatic particle buildup
        if (level >= 6 && world instanceof ServerWorld serverWorld) {
            // First, spawn particles that flow from player to form the shape
            spawnParticleBuildup(serverWorld, user, targetPos, size, offset);

            // Then remove the blocks after a short delay to let the particles be seen
            serverWorld.getServer().execute(() -> {
                // Small delay to allow particles to be visible
                try {
                    Thread.sleep(800); // 0.8 second delay for drama
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                // Now remove the blocks
                for (int x = -offset; x < size - offset; x++) {
                    for (int y = -offset; y < size - offset; y++) {
                        for (int z = -offset; z < size - offset; z++) {
                            BlockPos currentPos = targetPos.add(x, y, z);
                            BlockState state = world.getBlockState(currentPos);

                            if (state.isAir() || state.getBlock().getHardness() < 0) {
                                continue;
                            }

                            world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                        }
                    }
                }
            });

            return true; // We've scheduled the removal, so return true immediately
        }
        else {
            // For levels 1-5, immediate removal (original behavior)
            for (int x = -offset; x < size - offset; x++) {
                for (int y = -offset; y < size - offset; y++) {
                    for (int z = -offset; z < size - offset; z++) {
                        BlockPos currentPos = targetPos.add(x, y, z);
                        BlockState state = world.getBlockState(currentPos);

                        if (state.isAir() || state.getBlock().getHardness() < 0) {
                            continue;
                        }

                        world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                        vanishedAny = true;
                    }
                }
            }
        }

        return vanishedAny;
    };

    // Helper method to spawn dramatic particle buildup for high-level spells
    private static void spawnParticleBuildup(ServerWorld world, PlayerEntity user, BlockPos targetPos, int size, int offset) {
        Vec3d userPos = user.getEyePos();
        int particleCount = size * 10; // Scale particle count with spell size

        // Spawn particles that flow from player to the target area
        for (int i = 0; i < particleCount; i++) {
            // Calculate a random position within the target cube
            double targetX = targetPos.getX() + world.random.nextDouble() * size - offset;
            double targetY = targetPos.getY() + world.random.nextDouble() * size - offset;
            double targetZ = targetPos.getZ() + world.random.nextDouble() * size - offset;

            Vec3d targetVec = new Vec3d(targetX + 0.5, targetY + 0.5, targetZ + 0.5);

            // Calculate direction from user to target
            Vec3d direction = targetVec.subtract(userPos).normalize();

            // Spawn particle along the path with some randomness
            double progress = world.random.nextDouble();
            Vec3d particlePos = userPos.add(direction.multiply(progress * userPos.distanceTo(targetVec)));

            // Add some randomness to the particle position
            particlePos = particlePos.add(
                    world.random.nextDouble() * 2 - 1,
                    world.random.nextDouble() * 2 - 1,
                    world.random.nextDouble() * 2 - 1
            );

            world.spawnParticles(
                    ParticleTypes.ENCHANT,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, // count
                    0.1, 0.1, 0.1, // delta (small spread)
                    0.02 // speed
            );
        }

        // Also spawn particles forming the outline of the cube
        spawnCubeOutline(world, targetPos, size, offset);
    }

    // Helper method to spawn particles forming the outline of the cube
    private static void spawnCubeOutline(ServerWorld world, BlockPos center, int size, int offset) {
        BlockPos start = center.add(-offset, -offset, -offset);
        BlockPos end = center.add(size - offset - 1, size - offset - 1, size - offset - 1);

        // Spawn particles along the edges of the cube
        for (int i = 0; i < size; i++) {
            // Bottom face edges
            spawnParticleAt(world, start.getX() + i, start.getY(), start.getZ());
            spawnParticleAt(world, start.getX() + i, start.getY(), end.getZ());
            spawnParticleAt(world, start.getX(), start.getY(), start.getZ() + i);
            spawnParticleAt(world, end.getX(), start.getY(), start.getZ() + i);

            // Top face edges
            spawnParticleAt(world, start.getX() + i, end.getY(), start.getZ());
            spawnParticleAt(world, start.getX() + i, end.getY(), end.getZ());
            spawnParticleAt(world, start.getX(), end.getY(), start.getZ() + i);
            spawnParticleAt(world, end.getX(), end.getY(), start.getZ() + i);

            // Vertical edges
            spawnParticleAt(world, start.getX(), start.getY() + i, start.getZ());
            spawnParticleAt(world, end.getX(), start.getY() + i, start.getZ());
            spawnParticleAt(world, start.getX(), start.getY() + i, end.getZ());
            spawnParticleAt(world, end.getX(), start.getY() + i, end.getZ());
        }
    }

    private static void spawnParticleAt(ServerWorld world, double x, double y, double z) {
        world.spawnParticles(
                ParticleTypes.ENCHANT,
                x + 0.5, y + 0.5, z + 0.5,
                1, // count
                0, 0, 0, // no spread
                0 // no motion
        );
    }



    // RETRIBUTION Effect - Punishes the next attacker with multiplied damage
    public static final SpellEffect RETRIBUTION = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);

        // Store the retribution data on the player using a custom data component
        // We'll use a simple approach by storing it in a temporary field
        if (world instanceof ServerWorld serverWorld) {
            // Apply the retribution effect to the player
            applyRetributionEffect(user, level);

            // Play a subtle activation sound
            world.playSound(null, user.getBlockPos(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
                    SoundCategory.PLAYERS,
                    0.5f, 0.8f + (level * 0.1f));

            // Spawn subtle particles around the player
            serverWorld.spawnParticles(
                    ParticleTypes.ENCHANT,
                    user.getX(),
                    user.getY() + 1.0,
                    user.getZ(),
                    10 + (level * 2),
                    0.5, 0.5, 0.5,
                    0.02
            );

            return true;
        }

        return false;
    };

    // Helper method to apply retribution effect to the player
    private static void applyRetributionEffect(PlayerEntity player, int level) {
        // We'll use a custom data component to track retribution
        // For now, we'll use a simple approach with a static map (in a real implementation, you'd want persistent storage)
        RetributionManager.setRetribution(player, level);
    }

    // Custom manager class to handle retribution effects
    public static class RetributionManager {
        // Simple in-memory storage (would need persistence in a real implementation)
        private static final Map<UUID, RetributionData> activeRetributions = new HashMap<>();

        public static void setRetribution(PlayerEntity player, int level) {
            activeRetributions.put(player.getUuid(), new RetributionData(level, player.getWorld().getTime()));
        }

        public static RetributionData getRetribution(PlayerEntity player) {
            return activeRetributions.get(player.getUuid());
        }

        public static void removeRetribution(PlayerEntity player) {
            activeRetributions.remove(player.getUuid());
        }

        // Call this when a player is attacked
        public static boolean onPlayerAttacked(PlayerEntity player, LivingEntity attacker, float damageAmount) {
            RetributionData data = getRetribution(player);
            if (data != null) {
                // Calculate retribution damage (damage dealt × level)
                float retributionDamage = damageAmount * data.level;

                // Apply the damage to the attacker
                if (attacker.damage((ServerWorld) player.getWorld(), player.getDamageSources().magic(), retributionDamage)) {

                    // HEAL THE PLAYER - Give back the health they lost
                    //player.heal(damageAmount);


                    // Spawn thorns-like particles
                    if (player.getWorld() instanceof ServerWorld serverWorld) {
                        spawnRetributionParticles(serverWorld, attacker, data.level);
                    }

                    // Play thorns sound
                    player.getWorld().playSound(null, attacker.getBlockPos(),
                            SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                            SoundCategory.PLAYERS,
                            1.0f, 0.8f + (data.level * 0.1f));

                    // Remove the retribution effect after it triggers
                    removeRetribution(player);
                    return true;
                }
            }
            return false;
        }

        private static void spawnRetributionParticles(ServerWorld world, LivingEntity target, int level) {
            int particleCount = 10 + (level * 5); // More particles for higher levels

            for (int i = 0; i < particleCount; i++) {
                world.spawnParticles(
                        ParticleTypes.CRIT,
                        target.getX() + (world.random.nextDouble() - 0.5) * target.getWidth(),
                        target.getY() + world.random.nextDouble() * target.getHeight(),
                        target.getZ() + (world.random.nextDouble() - 0.5) * target.getWidth(),
                        1, // count
                        0.1, 0.1, 0.1, // spread
                        0.05 // speed
                );

                // Add magic particles for higher levels
                if (level >= 3) {
                    world.spawnParticles(
                            ParticleTypes.ENCHANT,
                            target.getX() + (world.random.nextDouble() - 0.5) * target.getWidth(),
                            target.getY() + world.random.nextDouble() * target.getHeight(),
                            target.getZ() + (world.random.nextDouble() - 0.5) * target.getWidth(),
                            1, // count
                            0.15, 0.15, 0.15, // spread
                            0.03 // speed
                    );
                }
            }
        }
    }

    // Data class to store retribution information
    public static class RetributionData {
        public final int level;
        public final long activationTime;

        public RetributionData(int level, long activationTime) {
            this.level = level;
            this.activationTime = activationTime;
        }
    }

    // Ice Effect - Shoots an ice projectile that damages and slows enemies
    public static final SpellEffect ICE_SPEAR = (world, user, hand, stack, hit) -> {
        if (world.isClient()) return false;

        int level = getSpellLevel(stack);
        int slownessDuration = (3 + level) * 20; // In ticks (3 seconds + 1s per level)
        float damage = 4.0f + (level * 2.0f);
        double speed = 2.0 + (level * 0.5);
        double maxDistance = 50.0 + (level * 10.0);

        if (world instanceof ServerWorld serverWorld) {
            Vec3d startPos = user.getEyePos();
            Vec3d direction = user.getRotationVec(1.0f).normalize();

            // Create a simple projectile by raycasting with steps
            for (double distance = 0; distance <= maxDistance; distance += 1.0) {
                Vec3d currentPos = startPos.add(direction.multiply(distance));

                // Spawn ice/snow particles along the path
                serverWorld.spawnParticles(
                        ParticleTypes.SNOWFLAKE,
                        currentPos.x, currentPos.y, currentPos.z,
                        2, // Fewer particles for performance
                        0.1, 0.1, 0.1, // Small spread
                        0.01 // Speed
                );

                // Add ice particles for higher levels
                if (level >= 3) {
                    serverWorld.spawnParticles(
                            ParticleTypes.ITEM_SNOWBALL,
                            currentPos.x, currentPos.y, currentPos.z,
                            1,
                            0.05, 0.05, 0.05,
                            0.005
                    );
                }

                // Check for entity collision at this point
                Box collisionBox = new Box(currentPos, currentPos).expand(0.5);
                List<LivingEntity> entities = world.getEntitiesByClass(
                        LivingEntity.class,
                        collisionBox,
                        entity -> entity != user && entity.isAlive()
                );

                if (!entities.isEmpty()) {
                    // Hit an entity!
                    LivingEntity target = entities.get(0);

                    // Apply damage and slowness (freeze effect)
                    if (target.damage(serverWorld, user.getDamageSources().magic(), damage)) {
                        // Apply slowness as our "freeze" effect
                        target.addStatusEffect(new StatusEffectInstance(
                                StatusEffects.SLOWNESS,
                                slownessDuration,
                                Math.min(level - 1, 3), // Higher levels = stronger slowness (up to Slowness IV)
                                false, true
                        ));

                        // Apply mining fatigue to simulate freezing (reduces attack speed)
                        if (level >= 2) {
                            target.addStatusEffect(new StatusEffectInstance(
                                    StatusEffects.MINING_FATIGUE,
                                    slownessDuration,
                                    Math.min(level - 2, 2), // Scales with level
                                    false, true
                            ));
                        }

                        // For very high levels, apply weakness too
                        if (level >= 4) {
                            target.addStatusEffect(new StatusEffectInstance(
                                    StatusEffects.WEAKNESS,
                                    slownessDuration / 2, // Shorter duration
                                    Math.min(level - 4, 1),
                                    false, true
                            ));
                        }

                        // Small knockback (less than fire bolt for ice theme)
                        Vec3d knockback = direction.multiply(0.2);
                        target.addVelocity(knockback.x, 0.05, knockback.z);
                        target.velocityModified = true;
                    }

                    // Impact effect - ice explosion
                    serverWorld.spawnParticles(
                            ParticleTypes.SNOWFLAKE,
                            currentPos.x, currentPos.y, currentPos.z,
                            15 + (level * 5),
                            0.5, 0.5, 0.5,
                            0.1
                    );

                    // Ice shard particles for impact
                    serverWorld.spawnParticles(
                            ParticleTypes.ITEM_SNOWBALL,
                            currentPos.x, currentPos.y, currentPos.z,
                            10 + (level * 3),
                            0.3, 0.3, 0.3,
                            0.05
                    );

                    // Sound - ice breaking
                    world.playSound(
                            null,
                            BlockPos.ofFloored(currentPos),
                            SoundEvents.BLOCK_GLASS_BREAK,
                            SoundCategory.HOSTILE,
                            0.8f, 1.2f + (level * 0.1f)
                    );

                    break; // Stop after hitting something
                }

                // Stop if we hit a block
                BlockPos blockPos = BlockPos.ofFloored(currentPos);
                if (!world.getBlockState(blockPos).isAir()) {
                    // Block impact effect - create frost on the block
                    serverWorld.spawnParticles(
                            ParticleTypes.SNOWFLAKE,
                            currentPos.x, currentPos.y, currentPos.z,
                            10 + (level * 3),
                            0.3, 0.3, 0.3,
                            0.05
                    );

                    // Try to place frost/snow on the block if it's a solid surface
                    Direction hitFace = getHitFace(direction);
                    BlockPos surfacePos = blockPos.offset(hitFace);

                    if (world.getBlockState(surfacePos).isAir()) {
                        // Place snow or frost walker ice based on level
                        if (level >= 3) {
                            world.setBlockState(surfacePos, Blocks.FROSTED_ICE.getDefaultState());
                        } else {
                            world.setBlockState(surfacePos, Blocks.SNOW.getDefaultState());
                        }
                    }

                    break;
                }

                // Small delay between steps to make it feel like a projectile
                try {
                    Thread.sleep(5); // 5ms delay - makes it visible as moving
                } catch (InterruptedException e) {
                    break;
                }
            }

            // Launch sound - ice magic
            world.playSound(
                    null,
                    user.getBlockPos(),
                    SoundEvents.ENTITY_SNOWBALL_THROW,
                    SoundCategory.PLAYERS,
                    0.7f,
                    0.8f + (level * 0.1f)
            );

            return true;
        }

        return false;
    };

    // Helper method to determine which face of the block was hit ice
    private static Direction getHitFace(Vec3d direction) {
        // Simple approximation - use the dominant direction component
        double absX = Math.abs(direction.x);
        double absY = Math.abs(direction.y);
        double absZ = Math.abs(direction.z);

        if (absX > absY && absX > absZ) {
            return direction.x > 0 ? Direction.EAST : Direction.WEST;
        } else if (absY > absX && absY > absZ) {
            return direction.y > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return direction.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    // Create a Fire Wave Effect with level scaling
    public static final SpellEffect FIRE_WAVE = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);
        int maxRadius = 4 + (level - 1) * 2; // Level 1: 4, Level 2: 6, Level 3: 8, etc.

        BlockPos playerPos = user.getBlockPos();
        ServerWorld serverWorld = (ServerWorld) world;

        // Create the expanding wave effect
        for (int radius = 1; radius <= maxRadius; radius++) {
            final int currentRadius = radius;

            // Schedule each ring with a delay to create the wave effect
            serverWorld.getServer().execute(() -> {
                createFireRing(serverWorld, playerPos, currentRadius, user, level);
            });

            // Add delay between rings for wave effect (2 ticks per ring)
            try {
                Thread.sleep(40); // 40ms = 2 ticks
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // For level 3+, create a second wave after a delay
        if (level >= 3) {
            serverWorld.getServer().execute(() -> {
                try {
                    Thread.sleep(1000); // 1 second delay for second wave
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                for (int radius = 1; radius <= maxRadius; radius++) {
                    final int currentRadius = radius;
                    serverWorld.getServer().execute(() -> {
                        createFireRing(serverWorld, playerPos, currentRadius, user, level);
                    });

                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        return true;
    };

    // Helper method to create a ring of fire at specific radius
    private static void createFireRing(ServerWorld world, BlockPos center, int radius, PlayerEntity player, int level) {
        DamageSource damageSource = createPlayerAttackSource(world, player);

        // Calculate all positions in the ring
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Only process blocks exactly at the current radius (circle pattern)
                double distance = Math.sqrt(x*x + z*z);
                if (Math.abs(distance - radius) > 0.5) continue;

                BlockPos targetPos = center.add(x, 0, z);

                // Check if block is flammable and ignite it
                BlockState state = world.getBlockState(targetPos);
                if (isFlammable(state)) {
                    // Try to place fire on top of flammable blocks
                    BlockPos firePos = targetPos.up();
                    if (world.getBlockState(firePos).isAir()) {
                        world.setBlockState(firePos, Blocks.FIRE.getDefaultState());
                    }
                }

                // Set entities on fire
                Box damageBox = new Box(
                        targetPos.getX() - 0.5, targetPos.getY(), targetPos.getZ() - 0.5,
                        targetPos.getX() + 1.5, targetPos.getY() + 2, targetPos.getZ() + 1.5
                );

                List<Entity> entities = world.getEntitiesByClass(Entity.class, damageBox,
                        entity -> entity != player && entity.isAlive());

                for (Entity entity : entities) {
                    entity.setFireTicks(80); // 4 seconds (20 ticks/second)
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.damage(world, damageSource, 2.0f * level);
                    }
                }

                // Spawn particles for visual effect
                world.spawnParticles(ParticleTypes.FLAME,
                        targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5,
                        3, 0.2, 0.5, 0.2, 0.05);

                world.spawnParticles(ParticleTypes.SMOKE,
                        targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5,
                        2, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    // Helper method to check if a block is flammable
    private static boolean isFlammable(BlockState state) {
        return state.isIn(BlockTags.LEAVES) ||
                state.isIn(BlockTags.LOGS) ||
                state.isIn(BlockTags.PLANKS) ||
                state.isOf(Blocks.GRASS_BLOCK) ||
                state.isOf(Blocks.TALL_GRASS) ||
                state.isOf(Blocks.FERN) ||
                state.isOf(Blocks.DEAD_BUSH) ||
                state.isOf(Blocks.VINE) ||
                state.isIn(BlockTags.WOOL) ||
                state.isIn(BlockTags.WOOL_CARPETS);
    }

    // FIRE_BOLT Effect - Simple custom projectile with flame trail
    public static final SpellEffect FIRE_BOLT = (world, user, hand, stack, hit) -> {
        if (world.isClient()) return false;

        int level = getSpellLevel(stack);
        int fireDuration = (3 + level) * 20; // In ticks
        float damage = 4.0f + (level * 2.0f);
        double speed = 2.0 + (level * 0.5);
        double maxDistance = 50.0 + (level * 10.0);

        if (world instanceof ServerWorld serverWorld) {
            Vec3d startPos = user.getEyePos();
            Vec3d direction = user.getRotationVec(1.0f).normalize();

            // Create a simple projectile by raycasting with steps
            for (double distance = 0; distance <= maxDistance; distance += 1.0) {
                Vec3d currentPos = startPos.add(direction.multiply(distance));

                // Spawn flame particles along the path
                serverWorld.spawnParticles(
                        ParticleTypes.FLAME,
                        currentPos.x, currentPos.y, currentPos.z,
                        2, // Fewer particles for performance
                        0.1, 0.1, 0.1, // Small spread
                        0.01 // Speed
                );

                // Check for entity collision at this point
                Box collisionBox = new Box(currentPos, currentPos).expand(0.5);
                List<LivingEntity> entities = world.getEntitiesByClass(
                        LivingEntity.class,
                        collisionBox,
                        entity -> entity != user && entity.isAlive()
                );

                if (!entities.isEmpty()) {
                    // Hit an entity!
                    LivingEntity target = entities.get(0);

                    // Apply damage and fire
                    if (target.damage(serverWorld, user.getDamageSources().magic(), damage)) {
                        target.setOnFireFor(fireDuration);

                        // Small knockback
                        Vec3d knockback = direction.multiply(0.3);
                        target.addVelocity(knockback.x, 0.1, knockback.z);
                    }

                    // Impact effect
                    serverWorld.spawnParticles(
                            ParticleTypes.FLAME,
                            currentPos.x, currentPos.y, currentPos.z,
                            15 + (level * 5),
                            0.5, 0.5, 0.5,
                            0.1
                    );

                    // Sound
                    world.playSound(
                            null,
                            BlockPos.ofFloored(currentPos),
                            SoundEvents.ENTITY_BLAZE_HURT,
                            SoundCategory.HOSTILE,
                            0.8f, 1.0f
                    );

                    break; // Stop after hitting something
                }

                // Stop if we hit a block
                if (!world.getBlockState(BlockPos.ofFloored(currentPos)).isAir()) {
                    // Block impact effect
                    serverWorld.spawnParticles(
                            ParticleTypes.FLAME,
                            currentPos.x, currentPos.y, currentPos.z,
                            10 + (level * 3),
                            0.3, 0.3, 0.3,
                            0.05
                    );
                    break;
                }

                // Small delay between steps to make it feel like a projectile
                try {
                    Thread.sleep(5); // 5ms delay - makes it visible as moving
                } catch (InterruptedException e) {
                    break;
                }
            }

            // Launch sound
            world.playSound(
                    null,
                    user.getBlockPos(),
                    SoundEvents.ENTITY_BLAZE_SHOOT,
                    SoundCategory.PLAYERS,
                    0.7f,
                    1.2f + (level * 0.1f)
            );

            return true;
        }

        return false;
    };

    // IGNITE Effect - Silently curses targets with fire that scales with level
    public static final SpellEffect IGNITE = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);
        float scaleFactor = getScaleFactor(level);

        double range = 20.0 * scaleFactor;

        // Fire duration scales with level (in seconds, converted to ticks)
        int baseFireDuration = 3; // 3 seconds at level 1
        int fireDurationTicks = (int)(baseFireDuration * level * 20); // Convert to ticks

        Vec3d lookVec = user.getRotationVec(1.0f);
        Vec3d start = user.getEyePos();
        Vec3d end = start.add(lookVec.multiply(range));

        Box hitBox = new Box(start, end).expand(scaleFactor);
        List<Entity> targets = world.getOtherEntities(user, hitBox);

        // Find the closest valid target in front of the player
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity target : targets) {
            if (target instanceof LivingEntity living && target != user) {
                Vec3d toTarget = living.getPos().subtract(start).normalize();
                if (lookVec.dotProduct(toTarget) > 0.3) {
                    double dist = living.squaredDistanceTo(start);
                    if (dist < closestDist) {
                        closest = living;
                        closestDist = dist;
                    }
                }
            }
        }

        if (closest != null && world instanceof ServerWorld serverWorld) {
            // Set the target on fire - this is the correct way in Minecraft 1.21.8
            closest.setOnFireFor(fireDurationTicks);

            // For additional fire damage at higher levels, we can apply instant fire damage
            if (level >= 3) {
                // Apply instant fire damage that scales with level
                float fireDamage = 2.0f * (level - 2); // 2 damage at level 3, 4 at level 4, etc.
                DamageSource fireDamageSource = world.getDamageSources().onFire();
                closest.damage(serverWorld, fireDamageSource, fireDamage);
            }

            // Spawn fire particles around the target
            serverWorld.spawnParticles(
                    ParticleTypes.FLAME,
                    closest.getX(),
                    closest.getBodyY(0.5),
                    closest.getZ(),
                    level * 3, // More particles for higher levels
                    0.5, 0.5, 0.5, // spread
                    0.05 // speed
            );

            // Spawn smoke particles for a cursed fire effect
            serverWorld.spawnParticles(
                    ParticleTypes.SMOKE,
                    closest.getX(),
                    closest.getBodyY(0.5),
                    closest.getZ(),
                    level * 2, // More smoke for higher levels
                    0.3, 0.3, 0.3, // spread
                    0.02 // speed
            );

            // For high levels, add soul fire particles for a cursed look
            if (level >= 4) {
                serverWorld.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        closest.getX(),
                        closest.getBodyY(0.5),
                        closest.getZ(),
                        level, // Soul fire particles
                        0.2, 0.2, 0.2, // spread
                        0.01 // speed
                );
            }

            // Silent curse - no sound played (as requested)
            return true;
        }

        return false;
    };

    // Sword Slash Effect with level scaling - Sharpness
    public static final SpellEffect SWORD_SLASH = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);
        float scaleFactor = getScaleFactor(level);

        double range = 20.0 * scaleFactor;
        float damage = 9.0f * scaleFactor;

        Vec3d lookVec = user.getRotationVec(1.0f);
        Vec3d start = user.getEyePos();
        Vec3d end = start.add(lookVec.multiply(range));

        Box hitBox = new Box(start, end).expand(scaleFactor);
        List<Entity> targets = world.getOtherEntities(user, hitBox);

        // Find the closest valid target in front of the player
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity target : targets) {
            if (target instanceof LivingEntity living && target != user) {
                Vec3d toTarget = living.getPos().subtract(start).normalize();
                if (lookVec.dotProduct(toTarget) > 0.3) {
                    double dist = living.squaredDistanceTo(start);
                    if (dist < closestDist) {
                        closest = living;
                        closestDist = dist;
                    }
                }
            }
        }

        if (closest != null && world instanceof ServerWorld serverWorld) {
            DamageSource src = createPlayerAttackSource(serverWorld, user);
            closest.damage(serverWorld, src, damage);

            // Spawn particles based on level
            serverWorld.spawnParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    closest.getX(),
                    closest.getBodyY(0.5),
                    closest.getZ(),
                    level, // More particles for higher levels
                    0.5 * scaleFactor, 0.5 * scaleFactor, 0.5 * scaleFactor,
                    0.0
            );

            // Play slash sound
            world.playSound(null, user.getBlockPos(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    SoundCategory.PLAYERS, 1.0f * scaleFactor, 1.0f);
        }

        return true;
    };

    // Turn Undead / Smite Effect
    // HOLY_NOVA Effect - Damages undead mobs with holy light
    public static final SpellEffect HOLY_NOVA = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);

        // Scaling system for radius and damage
        int radius;
        float damage;
        switch (level) {
            case 1:
                radius = 6;
                damage = 6.0f; // 3 hearts
                break;
            case 2:
                radius = 9;
                damage = 10.0f; // 5 hearts
                break;
            case 3:
                radius = 12;
                damage = 14.0f; // 7 hearts
                break;
            case 4:
                radius = 15;
                damage = 20.0f; // 10 hearts (one-shot basic undead)
                break;
            default:
                // Continue scaling beyond level 4
                radius = 15 + (level - 4) * 5;
                damage = 20.0f + (level - 4) * 5.0f;
        }

        if (world instanceof ServerWorld serverWorld) {
            // Phase 1: Charging effect (1 second buildup)
            spawnChargingEffect(serverWorld, user, radius);

            // Play charging sound (bell building up)
            world.playSound(null, user.getBlockPos(),
                    SoundEvents.BLOCK_BELL_USE,
                    SoundCategory.PLAYERS,
                    0.5f, 0.8f);

            // Schedule the nova burst after 1 second (20 ticks)
            serverWorld.getServer().execute(() -> {
                try {
                    Thread.sleep(1000); // 1 second delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                // Phase 2: Nova burst
                executeNovaBurst(serverWorld, user, radius, damage, level);
            });

            return true;
        }

        return false;
    };

    // Helper method for the charging phase
    private static void spawnChargingEffect(ServerWorld world, PlayerEntity user, int radius) {
        int particleCount = 30;

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double distance = world.random.nextDouble() * radius;
            double x = user.getX() + Math.cos(angle) * distance;
            double z = user.getZ() + Math.sin(angle) * distance;
            double y = user.getY() + 1.0 + world.random.nextDouble() * 2.0;

            // Golden particles slowly rising
            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK, // Golden sparkle effect
                    x, y, z,
                    1, // count
                    0.1, 0.1, 0.1, // spread
                    0.05 // slow upward motion
            );
        }
    }

    // Helper method for the nova burst phase
    private static void executeNovaBurst(ServerWorld world, PlayerEntity user, int radius, float damage, int level) {
        // Play burst sound
        world.playSound(null, user.getBlockPos(),
                SoundEvents.BLOCK_BELL_RESONATE,
                SoundCategory.PLAYERS,
                1.0f, 1.0f);

        world.playSound(null, user.getBlockPos(),
                SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT,
                SoundCategory.PLAYERS,
                0.7f, 1.2f);

        // Burst of golden light particles
        spawnNovaBurstParticles(world, user, radius);

        // Shockwave particles
        spawnShockwaveParticles(world, user, radius);

        // Damage undead mobs in the area
        Box novaArea = new Box(
                user.getX() - radius, user.getY() - 2, user.getZ() - radius,
                user.getX() + radius, user.getY() + 3, user.getZ() + radius
        );

        List<LivingEntity> entities = world.getNonSpectatingEntities(LivingEntity.class, novaArea);
        boolean affectedAny = false;

        for (LivingEntity entity : entities) {
            // Skip the player and non-undead mobs
            if (entity == user || !isUndead(entity)) continue;

            // Calculate distance-based damage falloff (optional)
            double distance = entity.getPos().distanceTo(user.getPos());
            float finalDamage = damage;
            if (distance > radius * 0.5) {
                // Reduce damage for entities at the edge
                finalDamage *= 0.7f;
            }

            // Create holy damage source (bypasses some undead resistances)
            DamageSource holyDamage = world.getDamageSources().indirectMagic(user, user);

            // Apply damage
            if (entity.damage(world, holyDamage, finalDamage)) {
                affectedAny = true;

                // Apply knockback
                Vec3d knockbackDir = entity.getPos().subtract(user.getPos()).normalize();
                double knockbackStrength = 1.0 + (level * 0.3);
                entity.addVelocity(
                        knockbackDir.x * knockbackStrength,
                        0.3 + (level * 0.1), // slight upward knockback
                        knockbackDir.z * knockbackStrength
                );
                entity.velocityModified = true;

                // Spawn holy particles on the undead entity
                spawnHolyParticlesOnEntity(world, entity);
            }
        }

        // Additional effect for high levels
        if (level >= 3) {
            // Heal the player slightly (2 hearts at level 3, scaling up)
            float healAmount = Math.min(4.0f + (level - 3) * 2.0f, 20.0f);
            user.heal(healAmount);

            // Play healing sound
            world.playSound(null, user.getBlockPos(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS,
                    0.5f, 1.5f);
        }
    }

    // Helper method to check if an entity is undead
    private static boolean isUndead(LivingEntity entity) {
        // Common undead mob types
        return entity.getType() == EntityType.ZOMBIE ||
                entity.getType() == EntityType.SKELETON ||
                entity.getType() == EntityType.WITHER_SKELETON ||
                entity.getType() == EntityType.ZOMBIFIED_PIGLIN ||
                entity.getType() == EntityType.DROWNED ||
                entity.getType() == EntityType.PHANTOM ||
                entity.getType() == EntityType.WITHER ||
                entity.getType() == EntityType.ZOMBIE_VILLAGER ||
                entity.getType() == EntityType.HUSK ||
                entity.getType() == EntityType.STRAY ||
                entity.getType() == EntityType.ZOGLIN;
    }

    // Helper method for nova burst particles
    private static void spawnNovaBurstParticles(ServerWorld world, PlayerEntity user, int radius) {
        int burstParticles = 50;

        for (int i = 0; i < burstParticles; i++) {
            double angle = (2 * Math.PI * i) / burstParticles;
            double x = user.getX() + Math.cos(angle) * radius;
            double z = user.getZ() + Math.sin(angle) * radius;
            double y = user.getY() + 1.0;

            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    x, y, z,
                    3, // count
                    0.5, 1.0, 0.5, // spread
                    0.1 // speed
            );

            // Additional glow effect
            world.spawnParticles(
                    ParticleTypes.GLOW,
                    x, y + 0.5, z,
                    2, // count
                    0.3, 0.3, 0.3, // spread
                    0.05 // speed
            );
        }
    }

    // Helper method for shockwave particles
    private static void spawnShockwaveParticles(ServerWorld world, PlayerEntity user, int radius) {
        // Create expanding rings of particles
        for (int ring = 1; ring <= 3; ring++) {
            int ringParticles = 20;
            double ringRadius = radius * (ring / 3.0);

            for (int i = 0; i < ringParticles; i++) {
                double angle = (2 * Math.PI * i) / ringParticles;
                double x = user.getX() + Math.cos(angle) * ringRadius;
                double z = user.getZ() + Math.sin(angle) * ringRadius;
                double y = user.getY() + 0.5;

                world.spawnParticles(
                        ParticleTypes.GLOW,
                        x, y, z,
                        1, // count
                        0, 0.1, 0, // spread (mostly upward)
                        0.2 // speed
                );
            }
        }
    }

    // Helper method to spawn holy particles on damaged undead
    private static void spawnHolyParticlesOnEntity(ServerWorld world, LivingEntity entity) {
        int particleCount = 10;

        for (int i = 0; i < particleCount; i++) {
            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    entity.getX() + (world.random.nextDouble() - 0.5) * entity.getWidth(),
                    entity.getY() + world.random.nextDouble() * entity.getHeight(),
                    entity.getZ() + (world.random.nextDouble() - 0.5) * entity.getWidth(),
                    1, // count
                    0.1, 0.1, 0.1, // spread
                    0.05 // speed
            );
        }
    }

    // VERMIN_BANE Effect - Eliminates arthropod mobs with pestilent energy
    public static final SpellEffect VERMIN_BANE = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);

        // Scaling system for radius and damage
        int radius;
        float damage;
        switch (level) {
            case 1:
                radius = 6;
                damage = 6.0f; // 3 hearts
                break;
            case 2:
                radius = 9;
                damage = 10.0f; // 5 hearts
                break;
            case 3:
                radius = 12;
                damage = 14.0f; // 7 hearts
                break;
            case 4:
                radius = 15;
                damage = 20.0f; // 10 hearts
                break;
            default:
                // Continue scaling beyond level 4
                radius = 15 + (level - 4) * 3;
                damage = 20.0f + (level - 4) * 5.0f;
        }

        if (world instanceof ServerWorld serverWorld) {
            // Phase 1: Charging effect (1 second buildup)
            spawnVerminChargingEffect(serverWorld, user, radius);

            // Play charging sound (buzzing/chittering building up)
            world.playSound(null, user.getBlockPos(),
                    SoundEvents.ENTITY_BEE_LOOP,
                    SoundCategory.PLAYERS,
                    0.3f, 0.5f); // Lower pitch for creepier sound

            world.playSound(null, user.getBlockPos(),
                    SoundEvents.ENTITY_SILVERFISH_AMBIENT,
                    SoundCategory.PLAYERS,
                    0.4f, 0.7f);

            // Schedule the burst after 1 second (20 ticks)
            serverWorld.getServer().execute(() -> {
                try {
                    Thread.sleep(1000); // 1 second delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                // Phase 2: Vermin bane burst
                executeVerminBurst(serverWorld, user, radius, damage, level);
            });

            return true;
        }

        return false;
    };

    // Helper method for the charging phase
    private static void spawnVerminChargingEffect(ServerWorld world, PlayerEntity user, int radius) {
        int particleCount = 25;

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double distance = world.random.nextDouble() * radius;
            double x = user.getX() + Math.cos(angle) * distance;
            double z = user.getZ() + Math.sin(angle) * distance;
            double y = user.getY() + 1.0 + world.random.nextDouble() * 2.0;

            // Dark ash and smoke particles slowly rising
            world.spawnParticles(
                    ParticleTypes.ASH,
                    x, y, z,
                    2, // count
                    0.1, 0.1, 0.1, // spread
                    0.03 // slow upward motion
            );

            world.spawnParticles(
                    ParticleTypes.SMOKE,
                    x, y, z,
                    1, // count
                    0.15, 0.15, 0.15, // spread
                    0.02 // slow upward motion
            );
        }
    }

    // Helper method for the vermin burst phase
    private static void executeVerminBurst(ServerWorld world, PlayerEntity user, int radius, float damage, int level) {
        // Play burst sounds
        world.playSound(null, user.getBlockPos(),
                SoundEvents.ENTITY_BEE_DEATH,
                SoundCategory.PLAYERS,
                0.8f, 0.6f);

        world.playSound(null, user.getBlockPos(),
                SoundEvents.ENTITY_SPIDER_DEATH,
                SoundCategory.PLAYERS,
                0.7f, 0.8f);

        world.playSound(null, user.getBlockPos(),
                SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, //was ENTITY_GENERIC_EXPLODE, but got error.
                SoundCategory.PLAYERS,
                0.5f, 1.5f);

        // Burst of dark energy particles
        spawnVerminBurstParticles(world, user, radius);

        // Shockwave of black/gray particles
        spawnVerminShockwave(world, user, radius);

        // Damage arthropod mobs in the area
        Box verminArea = new Box(
                user.getX() - radius, user.getY() - 2, user.getZ() - radius,
                user.getX() + radius, user.getY() + 3, user.getZ() + radius
        );

        List<LivingEntity> entities = world.getNonSpectatingEntities(LivingEntity.class, verminArea);
        boolean affectedAny = false;

        for (LivingEntity entity : entities) {
            // Skip the player and non-arthropod mobs
            if (entity == user || !isArthropod(entity)) continue;

            // Check for instant kill conditions
            boolean instantKill = shouldInstantKill(entity, level);

            // Calculate final damage (instant kill does massive damage)
            float finalDamage = instantKill ? 100.0f : damage;

            // Create vermin bane damage source
            DamageSource verminDamage = world.getDamageSources().indirectMagic(user, user);

            // Make arthropod screech before applying damage
            playArthropodScreech(world, entity);

            // Apply damage
            if (entity.damage(world, verminDamage, finalDamage)) {
                affectedAny = true;

                // Spawn death particles and effects
                spawnArthropodDeathEffects(world, entity, level);

                // Apply slight knockback
                Vec3d knockbackDir = entity.getPos().subtract(user.getPos()).normalize();
                entity.addVelocity(
                        knockbackDir.x * 0.5,
                        0.2,
                        knockbackDir.z * 0.5
                );
                entity.velocityModified = true;
            }
        }

        // Additional effect for high levels
        if (level >= 3 && affectedAny) {
            // Play a satisfying extermination sound
            world.playSound(null, user.getBlockPos(),
                    SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.PLAYERS,
                    0.3f, 1.2f);
        }
    }

    // Helper method to check if an entity is an arthropod
    private static boolean isArthropod(LivingEntity entity) {
        return entity.getType() == EntityType.SPIDER ||
                entity.getType() == EntityType.CAVE_SPIDER ||
                entity.getType() == EntityType.SILVERFISH ||
                entity.getType() == EntityType.ENDERMITE ||
                entity.getType() == EntityType.BEE;
    }

    // Helper method to determine instant kill conditions
    private static boolean shouldInstantKill(LivingEntity entity, int level) {
        if (level >= 4) {
            // Level 4+ instantly kills spiders and cave spiders
            return entity.getType() == EntityType.SPIDER ||
                    entity.getType() == EntityType.CAVE_SPIDER;
        }
        if (level >= 3) {
            // Level 3 instantly kills silverfish and endermites
            return entity.getType() == EntityType.SILVERFISH ||
                    entity.getType() == EntityType.ENDERMITE;
        }
        return false;
    }

    // Helper method to play arthropod screech sounds
    private static void playArthropodScreech(World world, LivingEntity entity) {
        if (entity.getType() == EntityType.SPIDER || entity.getType() == EntityType.CAVE_SPIDER) {
            world.playSound(null, entity.getBlockPos(),
                    SoundEvents.ENTITY_SPIDER_HURT,
                    SoundCategory.HOSTILE,
                    1.0f, 1.2f);
        } else if (entity.getType() == EntityType.SILVERFISH) {
            world.playSound(null, entity.getBlockPos(),
                    SoundEvents.ENTITY_SILVERFISH_HURT,
                    SoundCategory.HOSTILE,
                    1.0f, 1.0f);
        } else if (entity.getType() == EntityType.ENDERMITE) {
            world.playSound(null, entity.getBlockPos(),
                    SoundEvents.ENTITY_ENDERMITE_HURT,
                    SoundCategory.HOSTILE,
                    1.0f, 1.0f);
        } else if (entity.getType() == EntityType.BEE) {
            world.playSound(null, entity.getBlockPos(),
                    SoundEvents.ENTITY_BEE_HURT,
                    SoundCategory.HOSTILE,
                    1.0f, 0.8f);
        }
    }

    // Helper method for vermin burst particles
    private static void spawnVerminBurstParticles(ServerWorld world, PlayerEntity user, int radius) {
        int burstParticles = 40;

        for (int i = 0; i < burstParticles; i++) {
            double angle = (2 * Math.PI * i) / burstParticles;
            double x = user.getX() + Math.cos(angle) * radius;
            double z = user.getZ() + Math.sin(angle) * radius;
            double y = user.getY() + 1.0;

            // Black/gray particle burst
            world.spawnParticles(
                    ParticleTypes.SMOKE,
                    x, y, z,
                    2, // count
                    0.3, 0.5, 0.3, // spread
                    0.1 // speed
            );

            // Ash particles for the dark energy effect
            world.spawnParticles(
                    ParticleTypes.ASH,
                    x, y, z,
                    1, // count
                    0.2, 0.3, 0.2, // spread
                    0.08 // speed
            );
        }
    }

    // Helper method for vermin shockwave
    private static void spawnVerminShockwave(ServerWorld world, PlayerEntity user, int radius) {
        // Create expanding rings of dark particles
        for (int ring = 1; ring <= 3; ring++) {
            int ringParticles = 15;
            double ringRadius = radius * (ring / 3.0);

            for (int i = 0; i < ringParticles; i++) {
                double angle = (2 * Math.PI * i) / ringParticles;
                double x = user.getX() + Math.cos(angle) * ringRadius;
                double z = user.getZ() + Math.sin(angle) * ringRadius;
                double y = user.getY() + 0.5;

                world.spawnParticles(
                        ParticleTypes.SMOKE,
                        x, y, z,
                        2, // count
                        0, 0.2, 0, // spread (mostly upward)
                        0.15 // speed
                );
            }
        }
    }

    // Helper method to spawn arthropod death effects
    private static void spawnArthropodDeathEffects(ServerWorld world, LivingEntity entity, int level) {
        int particleCount = 15 + (level * 5);

        for (int i = 0; i < particleCount; i++) {
            // Smoke and ash particles where the arthropod dies
            world.spawnParticles(
                    ParticleTypes.SMOKE,
                    entity.getX() + (world.random.nextDouble() - 0.5) * entity.getWidth(),
                    entity.getY() + world.random.nextDouble() * entity.getHeight(),
                    entity.getZ() + (world.random.nextDouble() - 0.5) * entity.getWidth(),
                    1, // count
                    0.1, 0.1, 0.1, // spread
                    0.05 // speed
            );

            world.spawnParticles(
                    ParticleTypes.ASH,
                    entity.getX() + (world.random.nextDouble() - 0.5) * entity.getWidth(),
                    entity.getY() + world.random.nextDouble() * entity.getHeight(),
                    entity.getZ() + (world.random.nextDouble() - 0.5) * entity.getWidth(),
                    1, // count
                    0.08, 0.08, 0.08, // spread
                    0.03 // speed
            );

            // For high levels, add some soul particles for extra effect
            if (level >= 3) {
                world.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        entity.getX() + (world.random.nextDouble() - 0.5) * entity.getWidth(),
                        entity.getY() + world.random.nextDouble() * entity.getHeight(),
                        entity.getZ() + (world.random.nextDouble() - 0.5) * entity.getWidth(),
                        1, // count
                        0.05, 0.05, 0.05, // spread
                        0.02 // speed
                );
            }
        }
    }


    // Bind Effect - immobilizes entities in a 9x9 square with enchantment visuals
    public static final SpellEffect BIND = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        // Find target position (like VANISH_OBJECT)
        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        if (world instanceof ServerWorld serverWorld) {
            // Square expands from center out to 9x9
            int radius = 4; // 9x9 means radius 4 blocks around center
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = targetPos.add(x, 0, z);
                    serverWorld.spawnParticles(
                            ParticleTypes.ENCHANT,          // enchantment particles
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,               // float a bit above ground
                            pos.getZ() + 0.5,
                            3,                              // density
                            0.2, 0.2, 0.2,                  // spread
                            0.01                            // speed
                    );
                }
            }

            // Affect entities in area
            Box area = new Box(
                    targetPos.getX() - radius, targetPos.getY() - 2, targetPos.getZ() - radius,
                    targetPos.getX() + radius, targetPos.getY() + 2, targetPos.getZ() + radius
            );
            List<Entity> entities = world.getOtherEntities(user, area);


            for (Entity e : entities) {
                if (e instanceof LivingEntity living) {
                    // Freeze movement with strong slowness
                    living.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOWNESS, 60, 9, false, false, false
                    )); // 3 sec, amplifier 9

                    // Wrap enchantment visuals around target
                    for (int i = 0; i < 20; i++) {
                        double angle = (i / 20.0) * Math.PI * 2;
                        double dx = Math.cos(angle) * 0.8;
                        double dz = Math.sin(angle) * 0.8;
                        serverWorld.spawnParticles(
                                ParticleTypes.ENCHANT,
                                living.getX() + dx,
                                living.getBodyY(0.5),
                                living.getZ() + dz,
                                1, 0, 0, 0, 0.0
                        );
                    }
                }
            }
        }

        return true;
    };

    // FROST Effect - Freezes terrain and slows mobs with scaling area
    public static final SpellEffect FROST = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);

        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        // Scaling system for frost area
        int size;
        switch (level) {
            case 1: size = 9; break;
            case 2: size = 12; break;
            case 3: size = 18; break;
            case 4: size = 24; break;
            case 5: size = 32; break;
            case 6: size = 52; break;
            default: size = 52 + (level - 6) * 20; // Continue scaling beyond level 6
        }

        int radius = size / 2;
        boolean affectedAny = false;

        if (world instanceof ServerWorld serverWorld) {
            // Freeze blocks in the area
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Find the topmost solid block at this x,z position
                    BlockPos surfacePos = findSurfacePosition(world, targetPos.add(x, 0, z));
                    if (surfacePos != null) {
                        // Place ice or frost on the surface
                        BlockPos icePos = surfacePos.up(); // 1 block above surface

                        if (world.getBlockState(icePos).isAir()) {
                            // Place frost walker ice or regular ice based on level
                            if (level >= 3) {
                                world.setBlockState(icePos, Blocks.BLUE_ICE.getDefaultState());
                            } else if (level >= 2) {
                                world.setBlockState(icePos, Blocks.PACKED_ICE.getDefaultState());
                            } else {
                                world.setBlockState(icePos, Blocks.ICE.getDefaultState());
                            }
                            affectedAny = true;
                        }

                        // Also freeze water blocks if they exist
                        if (world.getBlockState(surfacePos).isOf(Blocks.WATER)) {
                            world.setBlockState(surfacePos, Blocks.ICE.getDefaultState());
                            affectedAny = true;
                        }
                    }
                }
            }

            // Apply freezing effects to mobs in the area
            Box freezeArea = new Box(
                    targetPos.getX() - radius, targetPos.getY() - 2, targetPos.getZ() - radius,
                    targetPos.getX() + radius, targetPos.getY() + 3, targetPos.getZ() + radius
            );

            List<LivingEntity> entities = world.getNonSpectatingEntities(LivingEntity.class, freezeArea);
            for (LivingEntity entity : entities) {
                if (entity == user) continue;

                // Apply slowness (freezing effect)
                int duration = (10 + level * 5) * 20; // 10 seconds base + 5 seconds per level
                int slownessLevel = Math.min(level, 4); // Cap at Slowness V

                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS,
                        duration,
                        slownessLevel - 1, // Level 0 = Slowness I, Level 1 = Slowness II, etc.
                        false, false, true
                ));

                // Apply additional freezing effects at higher levels
                if (level >= 3) {
                    entity.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.MINING_FATIGUE, // Slows attack speed
                            duration,
                            Math.min(level - 2, 3),
                            false, false, true
                    ));
                }

                // At very high levels, apply weakness too
                if (level >= 5) {
                    entity.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.WEAKNESS,
                            duration,
                            Math.min(level - 4, 2),
                            false, false, true
                    ));
                }

                // Spawn frost particles around affected mobs
                spawnFrostParticles(serverWorld, entity);
                affectedAny = true;
            }

            // Snowfall effect for level 6+
            if (level >= 6) {
                spawnSnowfall(serverWorld, targetPos, radius, level);
            }

            // Play frost sound
            if (affectedAny) {
                world.playSound(null, targetPos,
                        SoundEvents.BLOCK_GLASS_BREAK,
                        SoundCategory.PLAYERS,
                        1.0f, 0.8f + (level * 0.05f));
            }
        }

        return affectedAny;
    };

    // Helper method to spawn frost particles around entities
    private static void spawnFrostParticles(ServerWorld world, LivingEntity entity) {
        int particleCount = 8;
        double radius = 0.8;

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;

            world.spawnParticles(
                    ParticleTypes.SNOWFLAKE,
                    entity.getX() + dx,
                    entity.getY() + 0.5,
                    entity.getZ() + dz,
                    2, // count
                    0.1, 0.1, 0.1, // spread
                    0.02 // speed
            );
        }
    }

    // Helper method to create snowfall effect
    private static void spawnSnowfall(ServerWorld world, BlockPos center, int radius, int level) {
        int snowHeight = 10 + (level - 6) * 5; // Higher snowfall for higher levels
        int particleCount = radius * 5; // More particles for larger areas

        for (int i = 0; i < particleCount; i++) {
            double x = center.getX() + (world.random.nextDouble() - 0.5) * radius * 2;
            double z = center.getZ() + (world.random.nextDouble() - 0.5) * radius * 2;
            double y = center.getY() + snowHeight + world.random.nextDouble() * 5;

            world.spawnParticles(
                    ParticleTypes.SNOWFLAKE,
                    x, y, z,
                    3, // count
                    0.5, 0.5, 0.5, // spread
                    0.1 // falling speed
            );
        }
    }

    // Helper method to find surface position (reuse from BIND effect or create simple version)
    private static BlockPos findSurfacePosition(World world, BlockPos pos) {
        // Check from build height down to find the first non-air block
        for (int y = 320; y >= -64; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (!world.getBlockState(checkPos).isAir()) {
                return checkPos;
            }
        }
        return null;
    }

    // FEATHER Effect - Grants progressive flight abilities with scaling duration
    public static final SpellEffect FEATHER = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);

        // Calculate duration based on level
        int baseDuration;
        if (level <= 5) {
            baseDuration = 20 * level; // Level 1: 20s, Level 2: 40s, etc.
        } else {
            // Level 6+: 100s base + 20s per level beyond 5, with speed doubling each level
            baseDuration = 100 + (level - 5) * 20;
        }

        int durationTicks = baseDuration * 20; // Convert to ticks

        if (world instanceof ServerWorld serverWorld) {
            // Apply effects based on level
            switch (level) {
                case 1:
                    // Slow Falling for 20 seconds
                    user.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOW_FALLING,
                            durationTicks,
                            0, // Amplifier 0
                            false, false, true
                    ));
                    break;

                case 2:
                    // Jump Boost + Slow Falling for 40 seconds
                    user.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.JUMP_BOOST,
                            durationTicks,
                            0, // Jump Boost I
                            false, false, true
                    ));
                    user.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOW_FALLING,
                            durationTicks,
                            0,
                            false, false, true
                    ));
                    break;

                case 3:
                    // Levitation control for gliding (60 seconds)
                    user.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.LEVITATION,
                            durationTicks,
                            0, // Gentle levitation for gliding
                            false, false, true
                    ));
                    // Add slow falling to soften the landing
                    user.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOW_FALLING,
                            durationTicks,
                            0,
                            false, false, true
                    ));
                    break;

                case 4:
                    // Stronger levitation for upward lift + gliding (80 seconds)
                    user.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.LEVITATION,
                            60, // 3 seconds of upward lift
                            2, // Stronger levitation
                            false, false, true
                    ));
                    user.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.LEVITATION,
                            durationTicks - 60, // Rest of the time for gliding
                            0, // Gentle levitation
                            false, false, true
                    ));
                    user.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOW_FALLING,
                            durationTicks,
                            0,
                            false, false, true
                    ));
                    break;

                default: // Level 5 and above
                    // Creative-like flight for levels 5+
                    grantCreativeFlight(user, durationTicks, level);
                    break;
            }

            // Spawn feather particles around the player
            spawnFeatherParticles(serverWorld, user, level);

            // Play feather sound
            world.playSound(null, user.getBlockPos(),
                    SoundEvents.ITEM_ELYTRA_FLYING,
                    SoundCategory.PLAYERS,
                    1.0f, 0.8f + (level * 0.1f));

            return true;
        }

        return false;
    };

    // Helper method to grant creative-like flight for levels 5+
    private static void grantCreativeFlight(PlayerEntity player, int durationTicks, int level) {
        // Store the player's original abilities
        if (!player.getAbilities().allowFlying) {
            // Only grant flight if they don't already have it
            player.getAbilities().allowFlying = true;

            // Scale flight speed based on level
            float baseSpeed = 0.05f; // Default creative flight speed
            float speedMultiplier = (float) Math.pow(2, level - 5); // Double speed each level beyond 5
            player.getAbilities().setFlySpeed(baseSpeed * speedMultiplier);

            player.sendAbilitiesUpdate();

            // Schedule ability removal after duration
            player.getServer().execute(() -> {
                try {
                    Thread.sleep(durationTicks * 50L); // Convert ticks to milliseconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                // Only remove flight if we granted it (player might have creative mode now)
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().allowFlying = false;
                    player.getAbilities().setFlySpeed(0.05f); // Reset to default
                    player.sendAbilitiesUpdate();
                }
            });
        }

        // Also apply slow falling for safety
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOW_FALLING,
                durationTicks,
                0,
                false, false, true
        ));
    }

    // Helper method to spawn feather particles
    private static void spawnFeatherParticles(ServerWorld world, PlayerEntity player, int level) {
        int particleCount = 10 + (level * 5);
        double radius = 1.0 + (level * 0.2);

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;

            // Spawn particles in a circle around the player
            world.spawnParticles(
                    ParticleTypes.CLOUD,
                    player.getX() + dx,
                    player.getY() + 1.0,
                    player.getZ() + dz,
                    2, // count
                    0.1, 0.1, 0.1, // spread
                    0.02 // speed
            );

            // Add some upward-moving particles for levels 3+
            if (level >= 3) {
                world.spawnParticles(
                        ParticleTypes.END_ROD,
                        player.getX() + (world.random.nextDouble() - 0.5) * radius,
                        player.getY() + world.random.nextDouble() * 2,
                        player.getZ() + (world.random.nextDouble() - 0.5) * radius,
                        1, // count
                        0.05, 0.1, 0.05, // spread
                        0.1 // upward motion
                );
            }
        }
    }

    // NECROTIC Effect - Applies withering decay that scales with level
    public static final SpellEffect NECROTIC = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);

        // Scaling system for duration and effects
        int witherDuration;
        int witherAmplifier;
        float armorReduction;

        switch (level) {
            case 1:
                witherDuration = 5 * 20; // 5 seconds
                witherAmplifier = 0;     // Wither I
                armorReduction = 0.20f;  // 20% reduction
                break;
            case 2:
                witherDuration = 7 * 20; // 7 seconds
                witherAmplifier = 1;     // Wither II
                armorReduction = 0.40f;  // 40% reduction
                break;
            case 3:
                witherDuration = 10 * 20; // 10 seconds
                witherAmplifier = 1;      // Wither II
                armorReduction = 0.60f;   // 60% reduction
                break;
            case 4:
                witherDuration = 12 * 20; // 12 seconds
                witherAmplifier = 2;      // Wither III
                armorReduction = 0.80f;   // 80% reduction
                break;
            default:
                // Continue scaling beyond level 4
                witherDuration = (12 + (level - 4) * 2) * 20;
                witherAmplifier = Math.min(2 + (level - 4), 5); // Cap at Wither VI
                armorReduction = Math.min(0.80f + (level - 4) * 0.10f, 0.95f); // Cap at 95%
        }

        if (world instanceof ServerWorld serverWorld) {
            // Find target (similar to sword slash targeting)
            Vec3d lookVec = user.getRotationVec(1.0f);
            Vec3d start = user.getEyePos();
            double range = 20.0;
            Vec3d end = start.add(lookVec.multiply(range));

            Box hitBox = new Box(start, end).expand(1.0);
            List<Entity> targets = world.getOtherEntities(user, hitBox);

            // Find the closest valid target in front of the player
            LivingEntity closest = null;
            double closestDist = Double.MAX_VALUE;

            for (Entity target : targets) {
                if (target instanceof LivingEntity living && target != user) {
                    Vec3d toTarget = living.getPos().subtract(start).normalize();
                    if (lookVec.dotProduct(toTarget) > 0.3) {
                        double dist = living.squaredDistanceTo(start);
                        if (dist < closestDist) {
                            closest = living;
                            closestDist = dist;
                        }
                    }
                }
            }

            if (closest != null) {
                // Apply wither effect
                closest.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WITHER,
                        witherDuration,
                        witherAmplifier,
                        false, true, true
                ));

                // Apply armor reduction effect (using custom logic)
                applyArmorReduction(closest, armorReduction, witherDuration);

                // Level 3+ effects
                if (level >= 3) {
                    // Nullify active regeneration/absorption
                    closest.removeStatusEffect(StatusEffects.REGENERATION);
                    closest.removeStatusEffect(StatusEffects.ABSORPTION);

                    // Apply hunger to prevent natural healing
                    closest.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.HUNGER,
                            witherDuration,
                            1, // Hunger II
                            false, true, true
                    ));
                }

                // Level 4+ effects
                if (level >= 4) {
                    // Apply weakness to further reduce damage
                    closest.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.WEAKNESS,
                            witherDuration,
                            Math.min(level - 3, 3), // Scales with level
                            false, true, true
                    ));

                    // Apply slowness to complete the decay theme
                    closest.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOWNESS,
                            witherDuration,
                            Math.min(level - 3, 2),
                            false, true, true
                    ));
                }

                // Track the entity for death effects if needed
                if (level >= 3) {
                    NecroticDeathTracker.trackEntity(closest, level);
                }

                // Spawn necrotic particles
                spawnNecroticParticles(serverWorld, closest, level);

                // Play necrotic sound
                world.playSound(null, closest.getBlockPos(),
                        SoundEvents.ENTITY_WITHER_SHOOT,
                        SoundCategory.HOSTILE,
                        0.8f, 0.7f + (level * 0.1f));

                world.playSound(null, closest.getBlockPos(),
                        SoundEvents.ENTITY_SKELETON_HURT,
                        SoundCategory.HOSTILE,
                        0.6f, 0.5f);

                return true;
            }
        }

        return false;
    };

    // Helper method to apply armor reduction (simulated effect)
    private static void applyArmorReduction(LivingEntity entity, float reduction, int duration) {
        // Since Minecraft doesn't have a direct armor reduction effect,
        // we'll simulate it by applying weakness which reduces attack damage
        // and making the entity take more damage temporarily

        // Apply weakness to simulate reduced defense
        int weaknessLevel = (int)(reduction * 4); // Convert 20% reduction to Weakness I, etc.
        if (weaknessLevel > 0) {
            entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.WEAKNESS,
                    duration,
                    Math.min(weaknessLevel - 1, 3), // Cap at Weakness IV
                    false, true, true
            ));
        }
    }

    // Helper method to spawn necrotic particles
    private static void spawnNecroticParticles(ServerWorld world, LivingEntity target, int level) {
        int particleCount = 15 + (level * 5);

        for (int i = 0; i < particleCount; i++) {
            world.spawnParticles(
                    ParticleTypes.SMOKE,
                    target.getX() + (world.random.nextDouble() - 0.5) * target.getWidth(),
                    target.getY() + world.random.nextDouble() * target.getHeight(),
                    target.getZ() + (world.random.nextDouble() - 0.5) * target.getWidth(),
                    2, // count
                    0.1, 0.1, 0.1, // spread
                    0.03 // speed
            );

            world.spawnParticles(
                    ParticleTypes.ASH,
                    target.getX() + (world.random.nextDouble() - 0.5) * target.getWidth(),
                    target.getY() + world.random.nextDouble() * target.getHeight(),
                    target.getZ() + (world.random.nextDouble() - 0.5) * target.getWidth(),
                    1, // count
                    0.08, 0.08, 0.08, // spread
                    0.02 // speed
            );

            // Wither skull particles for higher levels
            if (level >= 2) {
                world.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        target.getX() + (world.random.nextDouble() - 0.5) * target.getWidth(),
                        target.getY() + world.random.nextDouble() * target.getHeight(),
                        target.getZ() + (world.random.nextDouble() - 0.5) * target.getWidth(),
                        1, // count
                        0.05, 0.05, 0.05, // spread
                        0.01 // speed
                );
            }
        }
    }

    // DEFENSE Effect - Grants temporary invulnerability with scaling duration
    public static final SpellEffect DEFENSE = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);
        int durationTicks = (3 * (int)Math.pow(2, level - 1)) * 20;

        // Just use maximum resistance instead - it's simpler and won't break mob AI
        user.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE,
                durationTicks,
                255, // Maximum possible resistance
                false, false, false
        ));

        return true;
    };

    // HASTE Effect - Grants haste with scaling level and duration
    public static final SpellEffect HASTE = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);

        // Scaling duration: Level 1: 10s, Level 2: 20s, Level 3: 40s, etc.
        int durationSeconds = 10 * (int)Math.pow(2, level - 1);
        int durationTicks = durationSeconds * 20;

        // Haste level scales with spell level (cap at Haste V)
        int hasteLevel = Math.min(level - 1, 4); // Level 1 = Haste I, Level 2 = Haste II, etc.

        if (world instanceof ServerWorld serverWorld) {
            // Apply haste effect
            user.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.HASTE,
                    durationTicks,
                    hasteLevel,
                    false, false, true
            ));

            // Spawn enchantment particles around the player
            spawnHasteParticles(serverWorld, user, level);

            // Play a subtle magic sound
            world.playSound(null, user.getBlockPos(),
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                    SoundCategory.PLAYERS,
                    0.7f, 1.0f + (level * 0.1f));

            return true;
        }

        return false;
    };

    // Helper method to spawn haste particles
    private static void spawnHasteParticles(ServerWorld world, PlayerEntity player, int level) {
        int particleCount = 15 + (level * 5);
        double radius = 1.0 + (level * 0.2);

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;

            world.spawnParticles(
                    ParticleTypes.ENCHANT,
                    player.getX() + dx,
                    player.getY() + 1.0,
                    player.getZ() + dz,
                    1, // count
                    0.1, 0.1, 0.1, // spread
                    0.02 // speed
            );

            // Add some electric spark particles for higher levels
            if (level >= 3) {
                world.spawnParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        player.getX() + dx * 0.7,
                        player.getY() + 1.2,
                        player.getZ() + dz * 0.7,
                        1, // count
                        0.05, 0.05, 0.05, // spread
                        0.01 // speed
                );
            }
        }
    }

} // Closing Line.



