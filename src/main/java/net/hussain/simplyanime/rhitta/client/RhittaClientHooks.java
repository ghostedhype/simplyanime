package net.hussain.simplyanime.rhitta.client;

import net.hussain.simplyanime.rhitta.RhittaRegistry;
import net.hussain.simplyanime.rhitta.SunPower;
import net.hussain.simplyanime.rhitta.config.RhittaClientConfig;
import net.hussain.simplyanime.rhitta.entity.CruelSunEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.block.BlockState;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

// client only, called from common classes on the client side
public class RhittaClientHooks {

    public static float localSunPower() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player == null || client.world == null ? -1.0F : SunPower.sunPower(client.world, client.player);
    }

    public static void startFlightSound(CruelSunEntity sun) {
        MinecraftClient.getInstance().getSoundManager().play(new SunFlightSound(sun));
    }

    // fewer particles the further away it is, and scaled by the player's density setting
    private static float detail(Vec3d at) {
        MinecraftClient client = MinecraftClient.getInstance();
        float density = RhittaClientConfig.get().particleDensity;
        if (density <= 0 || client.gameRenderer == null) {
            return 0.0F;
        }
        double dist = client.gameRenderer.getCamera().getPos().distanceTo(at);
        float far = dist < 32 ? 1.0F : dist < 96 ? 0.5F : 0.2F;
        return density * far;
    }

    private static int count(float amount, Random random) {
        int n = (int) amount;
        return n + (random.nextFloat() < amount - n ? 1 : 0);
    }

    public static void sunParticles(CruelSunEntity sun, LivingEntity owner) {
        World world = sun.getWorld();
        Random random = world.getRandom();
        float heat = 0.5F + sun.getSun();
        switch (sun.getPhase()) {
            case CruelSunEntity.PHASE_CHARGE -> {
                if (owner == null) {
                    return;
                }
                float orb = sun.getOrbRadius(0.0F);
                Vec3d center = CruelSunEntity.chargeCenter(owner, 1.0F, orb);
                float detail = detail(center);
                float t = sun.getPhaseProgress(0.0F);
                // embers spiral in
                for (int i = count((2 + 5 * t) * heat * detail, random); i > 0; i--) {
                    double yaw = random.nextDouble() * MathHelper.TAU;
                    double pitch = (random.nextDouble() - 0.5) * Math.PI;
                    double dist = orb * 2.5 + 2.0 + random.nextDouble() * 2.5;
                    Vec3d out = new Vec3d(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
                    Vec3d from = center.add(out.multiply(dist));
                    Vec3d swirl = out.crossProduct(new Vec3d(0, 1, 0)).multiply(0.08);
                    Vec3d pull = center.subtract(from).multiply(0.1).add(swirl);
                    world.addParticle(RhittaRegistry.SUN_SPARK.get(), from.x, from.y, from.z, pull.x, pull.y, pull.z);
                }
                float charge = sun.getPhaseTicks(0.0F);
                float full = sun.getOrbBase();
                if (charge < CruelSunEntity.IGNITE_TICKS + 20) {
                    // ground sparks up the column
                    double wide = Math.max(4.0, full * 1.4);
                    for (int i = count((10 + full) * detail, random); i > 0; i--) {
                        double a = random.nextDouble() * MathHelper.TAU;
                        double r = wide * (0.3 + 0.7 * random.nextDouble());
                        Vec3d from = new Vec3d(center.x + Math.cos(a) * r, owner.getY() + 0.1, center.z + Math.sin(a) * r);
                        Vec3d in = center.subtract(from).normalize();
                        Vec3d swirl = new Vec3d(-Math.sin(a), 0, Math.cos(a)).multiply(0.35);
                        Vec3d v = in.multiply(0.5 + random.nextDouble() * 0.5).add(swirl).add(0, 0.35, 0);
                        world.addParticle(RhittaRegistry.SUN_SPARK.get(), from.x, from.y, from.z, v.x, v.y, v.z);
                    }
                }
                if (charge < CruelSunEntity.IGNITE_TICKS) {
                    return;
                }
                if ((int) charge == CruelSunEntity.IGNITE_TICKS) {
                    // ignition burst
                    for (int i = count(140 * detail, random); i > 0; i--) {
                        Vec3d out = scatter(random, 1.0).normalize();
                        Vec3d v = out.multiply(0.6 + random.nextDouble() * 1.4);
                        world.addParticle(RhittaRegistry.SUN_SPARK.get(), center.x, center.y, center.z, v.x, v.y, v.z);
                    }
                    for (int i = count(30 * detail, random); i > 0; i--) {
                        Vec3d out = scatter(random, 1.0).normalize();
                        Vec3d p = center.add(out.multiply(full * 0.3));
                        world.addParticle(RhittaRegistry.SUN_WISP.get(), p.x, p.y, p.z, out.x * 0.5, out.y * 0.5, out.z * 0.5);
                    }
                }
                flicker(world, random, center, orb, (2.0F + orb) * heat * detail);
                // ground debris
                for (int i = count((1.0F + orb * 0.4F) * detail, random); i > 0; i--) {
                    double a = random.nextDouble() * MathHelper.TAU;
                    double r = 1.0 + random.nextDouble() * orb * 1.6;
                    BlockPos ground = BlockPos.ofFloored(owner.getX() + Math.cos(a) * r, owner.getY() - 0.5, owner.getZ() + Math.sin(a) * r);
                    BlockState state = world.getBlockState(ground);
                    if (state.isAir()) {
                        continue;
                    }
                    world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), ground.getX() + 0.5,
                            ground.getY() + 1.05, ground.getZ() + 0.5, 0, 0.3 + random.nextDouble() * 0.5, 0);
                }
                if (random.nextFloat() < 0.5F * detail) {
                    double a = random.nextDouble() * MathHelper.TAU;
                    double r = 0.8 + random.nextDouble() * orb * 1.8;
                    world.addParticle(ParticleTypes.LARGE_SMOKE, owner.getX() + Math.cos(a) * r, owner.getY() + 0.1,
                            owner.getZ() + Math.sin(a) * r, 0, 0.05 + 0.05 * t, 0);
                }
            }
            case CruelSunEntity.PHASE_FLIGHT -> {
                float orb = sun.getOrbRadius(0.0F);
                Vec3d at = sun.getPos();
                Vec3d dir = sun.getDirection();
                float detail = detail(at);
                // trail
                for (int i = count(4 * heat * detail, random); i > 0; i--) {
                    Vec3d p = at.subtract(dir.multiply(orb * 0.6)).add(scatter(random, orb * 0.7));
                    Vec3d v = dir.multiply(-0.05).add(scatter(random, 0.03));
                    world.addParticle(RhittaRegistry.SUN_WISP.get(), p.x, p.y, p.z, v.x, v.y, v.z);
                }
                flicker(world, random, at, orb, 1.0F * heat * detail);
                for (int i = count(6 * heat * detail, random); i > 0; i--) {
                    Vec3d p = at.add(scatter(random, orb * 0.8));
                    Vec3d v = dir.multiply(-0.5 - random.nextDouble() * 0.6).add(scatter(random, 0.15));
                    world.addParticle(RhittaRegistry.SUN_SPARK.get(), p.x, p.y, p.z, v.x, v.y, v.z);
                }
            }
            case CruelSunEntity.PHASE_BLAST -> {
                float ticks = sun.getPhaseTicks(0.0F);
                float radius = sun.getBlastRadius();
                Vec3d at = sun.getPos();
                float detail = detail(at);
                if (ticks < 2) {
                    // debris
                    for (int i = count(Math.min(120, radius * 5.0F) * detail, random); i > 0; i--) {
                        double a = random.nextDouble() * MathHelper.TAU;
                        double r = random.nextDouble() * radius * 0.8;
                        BlockPos ground = BlockPos.ofFloored(at.x + Math.cos(a) * r, at.y - 1, at.z + Math.sin(a) * r);
                        BlockState state = world.getBlockState(ground);
                        for (int d = 0; d < 6 && state.isAir(); d++) {
                            ground = ground.down();
                            state = world.getBlockState(ground);
                        }
                        if (state.isAir()) {
                            continue;
                        }
                        double speed = 0.6 + random.nextDouble() * 1.2;
                        world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), ground.getX() + 0.5,
                                ground.getY() + 1.1, ground.getZ() + 0.5, Math.cos(a) * speed, 0.4 + random.nextDouble() * 0.9,
                                Math.sin(a) * speed);
                    }
                }
                if (ticks < 2) {
                    // sparks over the dome
                    for (int i = count(Math.min(260, 80 + radius * 6.0F) * detail, random); i > 0; i--) {
                        Vec3d out = scatter(random, 1.0).normalize();
                        out = new Vec3d(out.x, Math.abs(out.y), out.z);
                        Vec3d v = out.multiply(radius * (0.03 + 0.07 * random.nextDouble()));
                        world.addParticle(RhittaRegistry.SUN_SPARK.get(), at.x, at.y + 0.5, at.z, v.x, v.y + 0.2, v.z);
                    }
                }
                if (ticks <= CruelSunEntity.BLAST_GROW) {
                    float front = CruelSunEntity.blastFront(radius, ticks);
                    for (int i = count(Math.min(70, 16 + radius * 3.0F) * heat * detail, random); i > 0; i--) {
                        Vec3d out = scatter(random, 1.0).normalize();
                        Vec3d p = at.add(out.multiply(front * random.nextDouble()));
                        Vec3d v = out.multiply(0.15 + random.nextDouble() * 0.35);
                        world.addParticle(random.nextBoolean() ? RhittaRegistry.SUN_WISP.get() : RhittaRegistry.SUN_SPARK.get(),
                                p.x, p.y, p.z, v.x, v.y, v.z);
                    }
                } else if (CruelSunEntity.blastFade(ticks) < 0.95F) {
                    for (int i = count((4 + radius * 0.4F) * detail, random); i > 0; i--) {
                        Vec3d p = at.add(scatter(random, radius * 0.8));
                        world.addParticle(random.nextInt(3) == 0 ? ParticleTypes.LARGE_SMOKE : RhittaRegistry.SUN_WISP.get(),
                                p.x, p.y, p.z, 0, 0.04 + random.nextDouble() * 0.05, 0);
                    }
                    // gas off the ground
                    for (int i = count((3 + radius * 0.3F) * detail, random); i > 0; i--) {
                        double a = random.nextDouble() * MathHelper.TAU;
                        double r = radius * 0.85 * Math.sqrt(random.nextDouble());
                        world.addParticle(RhittaRegistry.SUN_WISP.get(), at.x + Math.cos(a) * r, at.y + 0.3 + random.nextDouble() * radius * 0.25,
                                at.z + Math.sin(a) * r, 0, 0.15 + random.nextDouble() * 0.2, 0);
                    }
                    // falling embers
                    for (int i = count((2 + radius * 0.25F) * detail, random); i > 0; i--) {
                        Vec3d p = at.add((random.nextDouble() - 0.5) * radius * 1.6, radius * (0.4 + random.nextDouble() * 0.6),
                                (random.nextDouble() - 0.5) * radius * 1.6);
                        world.addParticle(RhittaRegistry.SUN_SPARK.get(), p.x, p.y, p.z, (random.nextDouble() - 0.5) * 0.04,
                                -0.12 - random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.04);
                    }
                    // dust column
                    if (random.nextFloat() < 0.6F * detail) {
                        Vec3d p = at.add((random.nextDouble() - 0.5) * radius, -radius * 0.2, (random.nextDouble() - 0.5) * radius);
                        world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, p.x, p.y, p.z, 0, 0.08 + random.nextDouble() * 0.06, 0);
                    }
                }
            }
            default -> {
            }
        }
    }

    // surface glow and sparks
    private static void flicker(World world, Random random, Vec3d center, float orb, float amount) {
        for (int i = count(amount * 0.5F, random); i > 0; i--) {
            Vec3d out = scatter(random, 1.0).normalize();
            Vec3d p = center.add(out.multiply(orb * 1.05));
            Vec3d v = out.multiply(0.08 + random.nextDouble() * 0.12);
            world.addParticle(RhittaRegistry.SUN_WISP.get(), p.x, p.y, p.z, v.x, v.y, v.z);
        }
        for (int i = count(amount * 0.4F, random); i > 0; i--) {
            Vec3d out = scatter(random, 1.0).normalize();
            Vec3d p = center.add(out.multiply(orb));
            Vec3d v = out.multiply(0.25 + random.nextDouble() * 0.45);
            world.addParticle(RhittaRegistry.SUN_SPARK.get(), p.x, p.y, p.z, v.x, v.y, v.z);
        }
    }

    private static Vec3d scatter(Random random, double size) {
        return new Vec3d((random.nextDouble() - 0.5) * 2 * size, (random.nextDouble() - 0.5) * 2 * size,
                (random.nextDouble() - 0.5) * 2 * size);
    }
}
