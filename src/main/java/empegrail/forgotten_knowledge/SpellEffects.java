package empegrail.forgotten_knowledge;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

import java.util.List;

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

    // Weapon Enchantments (1/8) implemented:

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


}



