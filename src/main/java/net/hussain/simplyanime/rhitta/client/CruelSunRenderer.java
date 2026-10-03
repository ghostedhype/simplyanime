package net.hussain.simplyanime.rhitta.client;

import net.hussain.simplyanime.client.renderer.AddonRenderLayers;
import net.hussain.simplyanime.client.renderer.ShaderCompat;
import net.hussain.simplyanime.client.renderer.VfxBuffer;
import net.hussain.simplyanime.rhitta.entity.CruelSunEntity;
import net.hussain.simplyanime.rhitta.config.RhittaConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// sun and fireball are sphere meshes shaded by how much each bit faces the camera, flames on top.
// fire stays inside what CruelSunEntity damages (HEAT_EDGE, TRAIL_*, blast front), only light and smoke go past.
// fire is drawn blended first then a bit of additive on top, additive alone goes white against a day sky
public class CruelSunRenderer extends EntityRenderer<CruelSunEntity> {

    private static final int[] WHITE = {255, 250, 235};
    private static final int[] PALE = {255, 238, 170};
    private static final int[] GOLD = {255, 196, 70};
    private static final int[] ORANGE = {255, 120, 20};
    private static final int[] RED = {220, 50, 10};
    private static final int[] SMOKE = {58, 46, 40};
    private static final int[] BODY = {255, 222, 120};
    private static final int[] LIMB = {238, 104, 18};
    private static final int[] FLAME = {255, 96, 8};
    private static final int[] EMBER = {200, 36, 4};

    // axis tilt, spin in radians per tick
    private static final float TILT = 0.42F;
    private static final float SPIN = 0.012F;

    public CruelSunRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CruelSunEntity entity) {
        return RhittaRenderLayers.GLOW;
    }

    @Override
    public boolean shouldRender(CruelSunEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(CruelSunEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        // shadow pass: only the lifted blocks cast shadows
        if (ShaderCompat.shadowPass()) {
            debrisOnly(entity, matrices, consumers, tickDelta);
            return;
        }
        Quaternionf rotation = this.dispatcher.getRotation();
        Vec3d right = rotate(rotation, 1, 0, 0);
        Vec3d up = rotate(rotation, 0, 1, 0);
        Vec3d base = entity.getLerpedPos(tickDelta);
        Vec3d camera = this.dispatcher.camera.getPos();
        float time = entity.age + tickDelta;
        float sunLevel = entity.getSun();
        Batch batch = new Batch(right, up, camera.subtract(base));

        switch (entity.getPhase()) {
            case CruelSunEntity.PHASE_CHARGE -> {
                LivingEntity owner = entity.getOwner();
                if (owner == null) {
                    return;
                }
                float ticks = entity.getPhaseTicks(tickDelta);
                float orb = entity.getOrbRadius(tickDelta);
                Vec3d center = CruelSunEntity.chargeCenter(owner, tickDelta, orb).subtract(base);
                Vec3d feet = owner.getLerpedPos(tickDelta).subtract(base);
                boolean formed = ticks >= CruelSunEntity.IGNITE_TICKS;
                birth(batch, feet, center, entity.getOrbBase(), time, ticks);
                if (formed) {
                    debris(entity, owner, base, matrices, consumers, tickDelta, time, ticks - CruelSunEntity.IGNITE_TICKS, 0.0F);
                    scorched(entity, owner, base, matrices, consumers, tickDelta, ticks - CruelSunEntity.IGNITE_TICKS, 1.0F);
                    ground(batch, feet, orb, ticks - CruelSunEntity.IGNITE_TICKS, sunLevel);
                    // eruption flash
                    float since = ticks - CruelSunEntity.IGNITE_TICKS;
                    ignition(batch, feet, center, entity.getOrbBase(), since);
                    sun(batch, center, orb, time, sunLevel, detail(camera, base.add(center)), 1.0F);
                } else {
                    // small flickering light until it erupts
                    float gather = ticks / CruelSunEntity.IGNITE_TICKS;
                    float flick = 0.85F + 0.15F * MathHelper.sin(time * 1.3F);
                    float spark = entity.getOrbBase() * (0.35F + 0.75F * gather);
                    batch.halo(center, spark * 2.8F * flick, FLAME, 0.7F);
                    batch.halo(center, spark * 1.7F, GOLD, 0.9F);
                    batch.halo(center, spark * 0.9F, WHITE, 1.0F);
                    rays(batch, center, spark * 0.4F, spark * (2.0F + 2.0F * gather), spark * 0.12F, 10, time * 3.0F, PALE, 0.7F);
                }
            }
            case CruelSunEntity.PHASE_FLIGHT -> {
                // rubble drops after the throw
                LivingEntity owner = entity.getOwner();
                float flight = entity.getPhaseTicks(tickDelta);
                if (owner != null && flight < DEBRIS_FALL) {
                    debris(entity, owner, base, matrices, consumers, tickDelta, time, 1000.0F, flight / DEBRIS_FALL);
                }
                if (owner != null && flight < FIRE_FADE) {
                    scorched(entity, owner, base, matrices, consumers, tickDelta, 1000.0F, 1.0F - flight / FIRE_FADE);
                }
                float orb = entity.getOrbRadius(tickDelta);
                Vec3d dir = entity.getDirection();
                trail(batch, dir, orb, time, base.subtract(camera));
                sun(batch, Vec3d.ZERO, orb, time, sunLevel, detail(camera, base), 1.0F);
            }
            case CruelSunEntity.PHASE_BLAST -> blast(batch, entity, tickDelta, time, sunLevel, detail(camera, base));
            default -> {
            }
        }
        batch.flush(matrices.peek().getPositionMatrix(), consumers);
    }

    private static void debrisOnly(CruelSunEntity entity, MatrixStack matrices, VertexConsumerProvider consumers, float tickDelta) {
        LivingEntity owner = entity.getOwner();
        if (owner == null) {
            return;
        }
        Vec3d base = entity.getLerpedPos(tickDelta);
        float time = entity.age + tickDelta;
        float ticks = entity.getPhaseTicks(tickDelta);
        if (entity.getPhase() == CruelSunEntity.PHASE_CHARGE && ticks >= CruelSunEntity.IGNITE_TICKS) {
            debris(entity, owner, base, matrices, consumers, tickDelta, time, ticks - CruelSunEntity.IGNITE_TICKS, 0.0F);
        } else if (entity.getPhase() == CruelSunEntity.PHASE_FLIGHT && ticks < DEBRIS_FALL) {
            debris(entity, owner, base, matrices, consumers, tickDelta, time, 1000.0F, ticks / DEBRIS_FALL);
        }
    }

    private static final int DEBRIS = 26;
    private static final float DEBRIS_FALL = 14.0F;

    // lifted ground chunks, block models from whatever is under each spot. fall 0..1 after the throw
    private static void debris(CruelSunEntity sun, LivingEntity owner, Vec3d base, MatrixStack matrices,
                               VertexConsumerProvider consumers, float tickDelta, float time, float since, float fall) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        BlockRenderManager blocks = client.getBlockRenderManager();
        Vec3d feet = owner.getLerpedPos(tickDelta);
        float spread = Math.max(3.0F, sun.getOrbBase() * 1.4F);
        for (int i = 0; i < DEBRIS; i++) {
            float f1 = ((i * 37) % 23) / 22.0F;
            float f2 = ((i * 53) % 19) / 18.0F;
            float f3 = ((i * 71) % 17) / 16.0F;
            float angle = i * 2.399F + time * 0.004F;
            float dist = 1.8F + f1 * spread;
            double x = feet.x + MathHelper.cos(angle) * dist;
            double z = feet.z + MathHelper.sin(angle) * dist;
            BlockPos.Mutable pos = BlockPos.ofFloored(x, feet.y + 2, z).mutableCopy();
            BlockState state = client.world.getBlockState(pos);
            for (int d = 0; d < 7 && (state.isAir() || !state.isOpaque()); d++) {
                pos.move(0, -1, 0);
                state = client.world.getBlockState(pos);
            }
            if (state.isAir() || !state.isOpaque()) {
                continue;
            }
            // staggered
            float rise = MathHelper.clamp((since - i * 0.6F) / 18.0F, 0.0F, 1.0F);
            rise = rise * rise * (3.0F - 2.0F * rise);
            float height = rise * (1.2F + f2 * 3.8F) + rise * 0.35F * MathHelper.sin(time * 0.09F + i * 1.3F);
            height *= 1.0F - fall * fall;
            if (height < 0.05F && fall > 0) {
                continue;
            }
            float size = (0.3F + f3 * 0.45F) * Math.max(0.35F, rise);
            matrices.push();
            matrices.translate(x - base.x, pos.getY() + 1.0 + height - base.y, z - base.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(time * 0.02F * (0.5F + f1) + i));
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(time * 0.015F * (0.5F + f2) + i * 0.7F));
            matrices.scale(size, size, size);
            matrices.translate(-0.5, -0.5, -0.5);
            blocks.renderBlockAsEntity(state, matrices, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
    }

    private static final int FIRE_SPOTS = 40;
    private static final float FIRE_FADE = 40.0F;

    // fake fire on plants and leaves, only the fire model drawn there. keep 1..0 after the throw
    private static void scorched(CruelSunEntity sun, LivingEntity owner, Vec3d base, MatrixStack matrices,
                                 VertexConsumerProvider consumers, float tickDelta, float since, float keep) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || !RhittaConfig.get().cruelSun.scorchSurroundings) {
            return;
        }
        BlockRenderManager blocks = client.getBlockRenderManager();
        BlockState fire = Blocks.FIRE.getDefaultState();
        Vec3d feet = owner.getLerpedPos(tickDelta);
        float reach = Math.max(3.0F, sun.getOrbBase() * 1.8F);
        int shown = (int) (FIRE_SPOTS * keep);
        for (int i = 0; i < shown; i++) {
            if (since < i * 0.8F) {
                break;
            }
            float f1 = ((i * 41) % 29) / 28.0F;
            float angle = i * 2.399F + 0.7F;
            float dist = 1.5F + f1 * reach;
            BlockPos.Mutable pos = BlockPos.ofFloored(feet.x + MathHelper.cos(angle) * dist, feet.y + 3,
                    feet.z + MathHelper.sin(angle) * dist).mutableCopy();
            BlockState state = client.world.getBlockState(pos);
            for (int d = 0; d < 7 && state.isAir(); d++) {
                pos.move(0, -1, 0);
                state = client.world.getBlockState(pos);
            }
            if (state.isAir() || !state.isFlammable(client.world, pos, Direction.UP)) {
                continue;
            }
            // plants burn in place, leaves and wood on top
            if (!state.isReplaceable()) {
                pos.move(0, 1, 0);
                if (!client.world.getBlockState(pos).isAir()) {
                    continue;
                }
            }
            matrices.push();
            matrices.translate(pos.getX() - base.x, pos.getY() - base.y, pos.getZ() - base.z);
            blocks.renderBlockAsEntity(fire, matrices, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
    }

    // fewer flames when it is far away
    private static float detail(Vec3d camera, Vec3d at) {
        double d = camera.distanceTo(at);
        return d < 64 ? 1.0F : d < 160 ? 0.6F : 0.35F;
    }

    private static void sun(Batch b, Vec3d c, float r, float time, float sunLevel, float detail, float alpha) {
        float pulse = 1.0F + 0.035F * MathHelper.sin(time * 0.5F) + 0.02F * MathHelper.sin(time * 1.7F);
        float bright = 0.8F + 0.2F * sunLevel;
        float body = r * pulse;
        float turned = time * SPIN;
        // how far the surface texture has slid round, one full turn is 2
        float slide = -turned / MathHelper.PI;
        // bloom first, kept low so the ball still looks like a ball
        b.halo(c, r * 2.5F * pulse, FLAME, 0.5F * alpha);
        b.glow(c, r * 4.2F * pulse, GOLD, 0.15F * alpha * bright);
        rays(b, c, r * 0.9F, r * 3.6F, r * 0.12F, 10, time, GOLD, 0.3F * alpha * bright);

        // solid body so the sky doesn't show through, two fire layers sliding over it
        b.ball(true, RhittaRenderLayers.PLASMA_A, c, body, TILT, slide, BODY, LIMB, alpha, alpha);
        b.ball(false, RhittaRenderLayers.PLASMA_A, c, body * 1.005F, TILT, slide, ORANGE, FLAME, 0.45F * alpha, 0.6F * alpha);
        b.ball(false, RhittaRenderLayers.PLASMA_B, c, body * 1.02F, -TILT * 1.7F, -slide * 0.6F, GOLD, GOLD, 0.3F * alpha, 0.2F * alpha);
        // haze on the limb
        b.ball(true, AddonRenderLayers.WHITE, c, body * 1.08F, 0.0F, 0.0F, ORANGE, FLAME, 0.0F, 0.7F * alpha);
        // core
        b.glow(c, r * 0.9F, WHITE, 0.25F * alpha);

        // tongues all round, mostly short with a long one now and then. tips stop at HEAT_EDGE
        float edge = r * CruelSunEntity.HEAT_EDGE;
        int flames = Math.max(40, (int) (110 * detail));
        for (int i = 0; i < flames; i++) {
            Vec3d n = spot(i, flames, turned, TILT);
            float facing = b.facing(c, n, body);
            if (facing < -0.35F || facing > 0.6F) {
                continue;
            }
            float f1 = ((i * 37) % 23) / 22.0F;
            float f2 = ((i * 53) % 19) / 18.0F;
            float flick = 0.5F + 0.5F * MathHelper.sin(time * (0.25F + 0.2F * f1) + i * 2.39F);
            float reach = i % 3 == 0 ? 0.45F + 0.5F * flick * (0.6F + 0.4F * f1) : 0.2F + 0.35F * flick * (0.5F + f2);
            float from = body * 0.88F;
            float len = Math.min(body * reach, edge - from);
            float wide = body * (0.2F + 0.14F * f2);
            float show = alpha * MathHelper.clamp(1.0F - facing, 0.0F, 1.0F);
            b.tongue(c, n, from, len, wide, i, FLAME, 0.9F * show);
            b.tongue(c, n, from, len * 0.6F, wide * 0.6F, i + 1, GOLD, 0.8F * show);
        }

        // loops
        int loops = detail > 0.5F ? 8 : 4;
        for (int k = 0; k < loops; k++) {
            Vec3d n = spot(k * 7 + 3, 61, turned, TILT);
            if (b.facing(c, n, body) < -0.3F) {
                continue;
            }
            float f1 = ((k * 37) % 23) / 22.0F;
            float life = MathHelper.sin(MathHelper.PI * MathHelper.fractionalPart(time / 110.0F + k * 0.37F));
            Vec3d across = n.crossProduct(new Vec3d(0.3, 1.0, 0.2 + k)).normalize();
            float span = 0.55F + 0.25F * f1;
            float height = (0.18F + 0.3F * f1) * life;
            Vec3d[] path = new Vec3d[11];
            for (int j = 0; j < path.length; j++) {
                float t = j / (float) (path.length - 1);
                float ang = (t - 0.5F) * span;
                Vec3d dir = n.multiply(MathHelper.cos(ang)).add(across.multiply(MathHelper.sin(ang)));
                path[j] = c.add(dir.multiply(body * (0.97F + height * MathHelper.sin(MathHelper.PI * t))));
            }
            b.ribbon(RhittaRenderLayers.RAY, path, body * 0.09F, body * 0.09F, FLAME, 0.8F * life * alpha, 0.8F * life * alpha, 0.0F);
            b.ribbon(RhittaRenderLayers.RAY, path, body * 0.035F, body * 0.035F, GOLD, 0.7F * life * alpha, 0.7F * life * alpha, 0.0F);
        }

        // flares
        for (int j = 0; j < 3; j++) {
            float clock = time / 70.0F + j * 0.41F;
            float cycle = MathHelper.fractionalPart(clock);
            float fade = 1.0F - MathHelper.clamp((cycle - 0.12F) / 0.5F, 0.0F, 1.0F);
            if (fade <= 0.0F) {
                continue;
            }
            int burst = MathHelper.floor(clock);
            Vec3d n = spot(Math.floorMod(burst * 13 + j * 7, 97), 97, turned, TILT);
            float facing = b.facing(c, n, body);
            if (facing < -0.3F || facing > 0.75F) {
                continue;
            }
            float grow = Math.min(1.0F, cycle / 0.12F);
            float from = body * 0.9F;
            float len = Math.min(body * 1.05F * grow, edge - from);
            b.tongue(c, n, from, len, body * 0.3F, burst + j, ORANGE, fade * alpha);
            b.tongue(c, n, from, len * 0.7F, body * 0.18F, burst + j + 2, PALE, 0.8F * fade * alpha);
        }
    }

    // i of count spread evenly over a ball, turning with the surface
    private static Vec3d spot(int i, int count, float turned, float tilt) {
        float y = 1.0F - 2.0F * (i + 0.5F) / count;
        float ring = MathHelper.sqrt(Math.max(0.0F, 1.0F - y * y));
        float a = i * 2.39996F + turned;
        float x = ring * MathHelper.cos(a);
        float z = ring * MathHelper.sin(a);
        float ct = MathHelper.cos(tilt);
        float st = MathHelper.sin(tilt);
        return new Vec3d(x, y * ct - z * st, y * st + z * ct);
    }

    // birth: shaft from the sky, rings closing in on the ground, a column and six streams into the spark.
    // all light, none of it burns
    private static void birth(Batch b, Vec3d feet, Vec3d center, float full, float time, float ticks) {
        float in = Math.min(1.0F, ticks / 6.0F);
        float out = 1.0F - MathHelper.clamp((ticks - CruelSunEntity.IGNITE_TICKS - 6.0F) / 24.0F, 0.0F, 1.0F);
        float alpha = in * out;
        if (alpha <= 0.01F) {
            return;
        }
        float gather = Math.min(1.0F, ticks / CruelSunEntity.IGNITE_TICKS);
        float wide = Math.max(4.0F, full * 1.2F);
        Vec3d ground = new Vec3d(center.x, feet.y + 0.08, center.z);

        // ground rings
        b.flatGlow(ground, wide * 1.2F, GOLD, 0.55F * alpha);
        b.flatGlow(ground, wide * 0.5F, WHITE, 0.5F * alpha * gather);
        for (int k = 0; k < 4; k++) {
            float cycle = MathHelper.fractionalPart(time * (0.05F + 0.04F * gather) + k * 0.25F);
            b.flatRing(ground.add(0, 0.02 * k, 0), wide * (1.0F - cycle * cycle), k % 2 == 0 ? ORANGE : GOLD,
                    0.9F * alpha * MathHelper.sin(MathHelper.PI * cycle));
        }

        // sky shaft + ground column
        Vec3d[] shaft = {center, center.add(0, full * 4.5F, 0)};
        b.ribbon(RhittaRenderLayers.RAY, shaft, full * (0.25F + 0.45F * gather), full * 0.9F, GOLD, 0.8F * alpha, 0.0F, 0.0F);
        b.ribbon(RhittaRenderLayers.RAY, shaft, full * (0.08F + 0.16F * gather), full * 0.25F, WHITE, 0.9F * alpha, 0.0F, 0.0F);
        Vec3d[] column = {ground, center};
        b.ribbon(RhittaRenderLayers.RAY, column, full * 0.3F, full * 0.15F, GOLD, 0.5F * alpha, 0.8F * alpha, 0.0F);
        b.ribbon(RhittaRenderLayers.RAY, column, full * 0.1F, full * 0.06F, WHITE, 0.7F * alpha, 0.9F * alpha, 0.0F);

        // streams
        for (int k = 0; k < 6; k++) {
            Vec3d[] path = new Vec3d[26];
            for (int j = 0; j < path.length; j++) {
                float t = j / (float) (path.length - 1);
                float a = k * MathHelper.TAU / 6.0F + t * 6.5F - time * 0.3F;
                float r = wide * (1.0F - t);
                double y = MathHelper.lerp(t, ground.y, center.y) + MathHelper.sin(MathHelper.PI * t) * full * 1.2F;
                path[j] = new Vec3d(center.x + MathHelper.cos(a) * r, y, center.z + MathHelper.sin(a) * r);
            }
            float thick = full * 0.22F;
            b.ribbon(RhittaRenderLayers.RAY, path, thick, thick * 0.3F, FLAME, 0.7F * alpha, alpha, 0.0F);
            b.ribbon(RhittaRenderLayers.RAY, path, thick * 0.5F, thick * 0.15F, GOLD, 0.8F * alpha, alpha, 0.0F);
            b.ribbon(RhittaRenderLayers.RAY, path, thick * 0.18F, thick * 0.06F, WHITE, 0.6F * alpha, alpha, 0.0F);
        }
    }

    // ignition flash
    private static void ignition(Batch b, Vec3d feet, Vec3d center, float full, float since) {
        if (since >= 16.0F) {
            return;
        }
        float k = since / 16.0F;
        float gone = (1.0F - k) * (1.0F - k);
        b.halo(center, full * (2.5F + 4.0F * k), FLAME, 0.5F * gone);
        b.halo(center, full * (1.6F + 3.0F * k), GOLD, 0.8F * gone);
        b.glow(center, full * (1.2F + 2.0F * k), WHITE, gone);
        Vec3d ground = new Vec3d(center.x, feet.y + 0.1, center.z);
        Vec3d[] shaft = {ground, center.add(0, full * 5.0F, 0)};
        b.ribbon(RhittaRenderLayers.RAY, shaft, full * 0.7F * (1.0F - k), full * 0.4F * (1.0F - k), WHITE, gone, 0.0F, 0.0F);
        // shell
        b.ball(true, AddonRenderLayers.WHITE, center, full * (0.3F + 2.6F * k), 0.0F, 0.0F, GOLD, GOLD, 0.0F, 0.8F * gone);
        b.flatRing(ground, full * (0.5F + 4.0F * k), PALE, gone);
        b.flatRing(ground.add(0, 0.03, 0), full * (0.3F + 3.0F * k), ORANGE, 0.8F * gone);
    }

    // god rays
    private static void rays(Batch b, Vec3d c, float start, float reach, float width, int count, float time, int[] color, float alpha) {
        for (int i = 0; i < count; i++) {
            float a = i * MathHelper.TAU / count + time * 0.004F + (i % 3) * 0.13F;
            float len = reach * (0.55F + 0.45F * (0.5F + 0.5F * MathHelper.sin(time * 0.21F + i * 3.1F)));
            float fade = 0.6F + 0.4F * MathHelper.sin(time * 0.33F + i * 1.3F);
            b.ray(c, a, start, Math.max(start + 0.1F, len), width, color, alpha * fade);
        }
    }

    // ground light
    private static void ground(Batch b, Vec3d feet, float orb, float since, float sunLevel) {
        Vec3d at = feet.add(0, 0.05, 0);
        float grow = Math.min(1.0F, since / 20.0F);
        b.flatGlow(at, orb * 2.2F * grow, GOLD, 0.35F + 0.1F * sunLevel);
        for (int k = 0; k < 3; k++) {
            float cycle = ((since + k * 13.0F) % 40.0F) / 40.0F;
            b.flatRing(at.add(0, 0.02 * k, 0), orb * (0.4F + 1.8F * cycle) * grow, ORANGE, 0.75F * (1.0F - cycle));
        }
    }

    // trail, same taper as CruelSunEntity.TRAIL_*
    private static void trail(Batch b, Vec3d dir, float r, float time, Vec3d fromCamera) {
        if (dir.lengthSquared() < 1.0E-4) {
            return;
        }
        dir = dir.normalize();
        Vec3d w = dir.crossProduct(fromCamera);
        w = w.lengthSquared() < 1.0E-6 ? b.right : w.normalize();
        Vec3d back = dir.multiply(-r * CruelSunEntity.TRAIL_LENGTH);
        float wide = r * CruelSunEntity.TRAIL_WIDTH;
        float tip = r * CruelSunEntity.TRAIL_TIP;
        float scroll = time * 0.08F;
        b.streak(RhittaRenderLayers.FLAMES, Vec3d.ZERO, back, w, wide, tip, ORANGE, 0.95F, scroll);
        b.streak(RhittaRenderLayers.FLAMES, Vec3d.ZERO, back.multiply(0.75), w, wide * 0.6F, tip, GOLD, 0.9F, scroll + 0.37F);
        b.streak(RhittaRenderLayers.RAY, Vec3d.ZERO, back.multiply(0.9), w, wide * 0.35F, tip * 0.5F, WHITE, 0.8F, 0.0F);
    }

    private static void blast(Batch b, CruelSunEntity entity, float tickDelta, float time, float sunLevel, float detail) {
        float ticks = entity.getPhaseTicks(tickDelta);
        float radius = entity.getBlastRadius();
        float front = CruelSunEntity.blastFront(radius, ticks);
        float fade = CruelSunEntity.blastFade(ticks);
        float swell = front * (1.0F + CruelSunEntity.FADE_SWELL * fade);
        float a = (float) Math.pow(1.0F - fade, 1.3);
        if (swell < 0.05F) {
            return;
        }
        // white -> orange -> red
        int[] shell = mix(ORANGE, RED, fade);
        int[] heart = mix(WHITE, GOLD, Math.min(1.0F, fade * 1.5F));
        float turned = time * 0.02F;
        float slide = -turned / MathHelper.PI;

        // flash
        if (ticks < 8.0F) {
            b.glow(Vec3d.ZERO, radius * 2.4F, WHITE, 1.0F - ticks / 8.0F);
        }
        // ground glow outlasts the fire
        b.flatGlow(new Vec3d(0, 0.06, 0), swell * 1.05F, FLAME, 0.7F * (1.0F - fade * fade));
        b.flatGlow(new Vec3d(0, 0.08, 0), swell * 0.5F, GOLD, 0.5F * (1.0F - fade));
        // solid at first then thins out
        float solid = fade < 0.25F ? 1.0F : (float) Math.pow(Math.max(0.0F, 1.0F - (fade - 0.25F) / 0.4F), 1.5);

        if (a > 0.01F) {
            b.glow(Vec3d.ZERO, swell * 1.8F, GOLD, 0.25F * a);
            b.glow(Vec3d.ZERO, swell * 1.15F, shell, 0.3F * a);
            rays(b, Vec3d.ZERO, swell * 0.3F, swell, swell * 0.08F, 18, time, PALE, 0.6F * a * (1.0F - fade));

            // dome, the skin sits on the damage front
            if (solid > 0.5F) {
                b.depthBall(Vec3d.ZERO, swell * 0.95F);
            }
            b.ball(true, RhittaRenderLayers.PLASMA_A, Vec3d.ZERO, swell * 0.97F, 0.3F, slide, mix(PALE, ORANGE, fade), mix(ORANGE, FLAME, fade), solid, solid);
            b.ball(false, RhittaRenderLayers.PLASMA_A, Vec3d.ZERO, swell * 0.98F, 0.3F, slide, FLAME, FLAME, 0.5F * a, 0.7F * a);
            b.ball(false, RhittaRenderLayers.PLASMA_B, Vec3d.ZERO, swell * 0.99F, -0.8F, -slide * 0.7F, GOLD, ORANGE,
                    0.45F * a * (1.0F - fade), 0.3F * a);
            // hotter ball inside
            b.ball(false, RhittaRenderLayers.PLASMA_B, Vec3d.ZERO, swell * 0.6F, 0.5F, slide * 1.6F, heart, GOLD, 0.5F * a, 0.2F * a);
            // shock front
            b.ball(false, RhittaRenderLayers.PLASMA_B, Vec3d.ZERO, swell, 1.1F, slide * 0.4F, PALE, PALE, 0.0F, 0.9F * a * (1.0F - fade));

            // tongues, tips on the damage front
            int flames = Math.max(24, (int) (70 * detail));
            for (int i = 0; i < flames; i++) {
                Vec3d n = spot(i, flames, turned * 0.3F, 0.3F);
                float facing = b.facing(Vec3d.ZERO, n, swell);
                if (n.y < -0.15 || facing < -0.35F || facing > 0.3F) {
                    continue;
                }
                float flick = 0.5F + 0.5F * MathHelper.sin(time * 0.4F + i * 2.13F);
                // fade with the solid fire or they look like loose cones
                float show = a * solid * MathHelper.clamp(1.15F - facing, 0.0F, 1.0F);
                b.tongue(Vec3d.ZERO, n, swell * 0.72F, swell * (0.14F + 0.14F * flick), swell * 0.12F, i, FLAME, 0.9F * show);
                b.tongue(Vec3d.ZERO, n, swell * 0.72F, swell * (0.08F + 0.08F * flick), swell * 0.08F, i + 2, GOLD, 0.8F * show);
            }

            // ground shockwaves
            for (int k = 0; k < 3; k++) {
                float lag = Math.max(0.0F, 1.0F - k * 0.18F);
                b.flatRing(new Vec3d(0, 0.1 * k, 0), swell * lag, k == 0 ? PALE : ORANGE, (0.9F - 0.2F * k) * a);
            }
        }

        // smoke, can go past the edge
        if (fade > 0.15F) {
            float k = (fade - 0.15F) / 0.85F;
            float smokeAlpha = 0.6F * MathHelper.sin(Math.min(1.0F, k * 1.3F) * MathHelper.PI) + 0.1F * (1.0F - k);
            for (int i = 0; i < 16; i++) {
                float ang = i * 2.399F;
                float dist = radius * (0.2F + 0.5F * ((i * 37) % 11) / 10.0F);
                Vec3d at = new Vec3d(MathHelper.cos(ang) * dist, radius * (0.1F + 0.9F * k) * (0.5F + 0.5F * ((i * 13) % 7) / 6.0F),
                        MathHelper.sin(ang) * dist);
                float size = radius * (0.35F + 0.3F * ((i * 7) % 5) / 4.0F) * (0.8F + 0.6F * k);
                b.smoke(at, size, time * 0.01F + i, SMOKE, smokeAlpha);
            }
        }
    }

    private static int[] mix(int[] from, int[] to, float t) {
        return new int[]{(int) MathHelper.lerp(t, from[0], to[0]), (int) MathHelper.lerp(t, from[1], to[1]),
                (int) MathHelper.lerp(t, from[2], to[2])};
    }

    private static Vec3d rotate(Quaternionf rotation, float x, float y, float z) {
        Vector3f out = rotation.transform(new Vector3f(x, y, z));
        return new Vec3d(out.x(), out.y(), out.z());
    }

    // quads are collected per texture and written in one go, switching buffers mid way would flush
    // how much of a flame's colour gets added again on top
    private static final float LIT = 0.35F;
    private static final int LON = 40;
    private static final int LAT = 20;
    // unit ball, [ring][slice] = x, y, z and how far that ring is from the pole
    private static final float[][][] BALL = new float[LAT + 1][LON + 1][];

    static {
        for (int j = 0; j <= LAT; j++) {
            float lat = (j / (float) LAT - 0.5F) * MathHelper.PI;
            float ring = MathHelper.cos(lat);
            for (int i = 0; i <= LON; i++) {
                float lon = i * MathHelper.TAU / LON;
                BALL[j][i] = new float[]{ring * MathHelper.cos(lon), MathHelper.sin(lat), ring * MathHelper.sin(lon), ring};
            }
        }
    }

    private static final class Batch {
        private final Map<Identifier, List<float[]>> additive = new LinkedHashMap<>();
        private final Map<Identifier, List<float[]>> blended = new LinkedHashMap<>();
        private final List<float[]> depth = new ArrayList<>();
        private final Map<Identifier, List<float[]>> solid = new LinkedHashMap<>();
        // the soft glow discs turn into flat circles under packs, their bloom does that job
        private final float soft = ShaderCompat.packInUse() ? 0.12F : 1.0F;
        final Vec3d right;
        final Vec3d up;
        // camera relative to the entity
        final Vec3d eye;

        Batch(Vec3d right, Vec3d up, Vec3d eye) {
            this.right = right;
            this.up = up;
            this.eye = eye;
        }

        // 1 where the surface at n looks straight at the camera, 0 on the limb, negative round the back
        float facing(Vec3d c, Vec3d n, float radius) {
            Vec3d p = c.add(n.multiply(radius));
            return (float) n.dotProduct(this.eye.subtract(p).normalize());
        }

        // camera facing half of a ball, vertex colour by facing: core in the middle, rim at the edge.
        // slide scrolls the texture round the axis
        void ball(boolean solid, Identifier texture, Vec3d c, float radius, float tilt, float slide,
                  int[] core, int[] rim, float coreAlpha, float rimAlpha) {
            if (radius <= 0.001F || (coreAlpha <= 0.005F && rimAlpha <= 0.005F)) {
                return;
            }
            // opaque under packs when nothing shows through
            boolean opaque = solid && coreAlpha > 0.95F && rimAlpha > 0.95F && ShaderCompat.packInUse();
            List<float[]> list = (opaque ? this.solid : solid ? this.blended : this.additive).computeIfAbsent(texture, k -> new ArrayList<>());
            float ct = MathHelper.cos(tilt);
            float st = MathHelper.sin(tilt);
            // camera inside the ball
            boolean inside = this.eye.squaredDistanceTo(c) < radius * radius;
            float[][] corners = new float[4][];
            for (int j = 0; j < LAT; j++) {
                for (int i = 0; i < LON; i++) {
                    boolean seen = false;
                    for (int k = 0; k < 4; k++) {
                        int ii = i + (k == 1 || k == 2 ? 1 : 0);
                        int jj = j + (k >= 2 ? 1 : 0);
                        float[] unit = BALL[jj][ii];
                        Vec3d n = new Vec3d(unit[0], unit[1] * ct - unit[2] * st, unit[1] * st + unit[2] * ct);
                        Vec3d p = c.add(n.multiply(radius));
                        float facing = (float) n.dotProduct(this.eye.subtract(p).normalize());
                        if (inside) {
                            facing = -facing;
                        }
                        seen |= facing > 0.0F;
                        float f = MathHelper.sqrt(MathHelper.clamp(facing, 0.0F, 1.0F));
                        // uv pinches at the poles, fade it
                        float alpha = MathHelper.lerp(f, rimAlpha, coreAlpha) * (solid ? 1.0F : Math.min(1.0F, unit[3] * 2.5F));
                        corners[k] = corner(p, mix(rim, core, f), alpha, ii * 2.0F / LON + slide, jj / (float) LAT);
                    }
                    if (seen) {
                        for (float[] corner : corners) {
                            list.add(corner);
                        }
                    }
                }
            }
        }

        // packs only, see AddonRenderLayers.depth
        void depthBall(Vec3d c, float radius) {
            if (!ShaderCompat.packInUse() || radius <= 0.001F || this.eye.squaredDistanceTo(c) < radius * radius) {
                return;
            }
            float[][] corners = new float[4][];
            for (int j = 0; j < LAT; j++) {
                for (int i = 0; i < LON; i++) {
                    boolean seen = false;
                    for (int k = 0; k < 4; k++) {
                        float[] unit = BALL[j + (k >= 2 ? 1 : 0)][i + (k == 1 || k == 2 ? 1 : 0)];
                        Vec3d n = new Vec3d(unit[0], unit[1], unit[2]);
                        Vec3d p = c.add(n.multiply(radius));
                        seen |= n.dotProduct(this.eye.subtract(p)) > 0.0;
                        corners[k] = corner(p, WHITE, 1.0F, 0.5F, 0.5F);
                    }
                    if (seen) {
                        for (float[] corner : corners) {
                            this.depth.add(corner);
                        }
                    }
                }
            }
        }

        // flame on a ball at n, facing the camera
        void tongue(Vec3d c, Vec3d n, float from, float length, float halfWidth, int variant, int[] color, float alpha) {
            Vec3d foot = c.add(n.multiply(from));
            Vec3d side = n.crossProduct(this.eye.subtract(foot));
            if (side.lengthSquared() < 1.0E-6 || length <= 0.001F) {
                return;
            }
            float u0 = (variant & 3) * 0.25F;
            this.fire(RhittaRenderLayers.FLAMES, foot.add(n.multiply(length * 0.5F)), side.normalize(), n,
                    halfWidth, length * 0.5F, color, alpha, u0, 0, u0 + 0.25F, 1);
        }

        // camera facing strip along a path
        void ribbon(Identifier texture, Vec3d[] path, float halfFrom, float halfTo, int[] color, float alphaFrom, float alphaTo, float scroll) {
            if (alphaFrom <= 0.005F && alphaTo <= 0.005F) {
                return;
            }
            List<float[]> list = this.blended.computeIfAbsent(texture, k -> new ArrayList<>());
            List<float[]> lit = this.additive.computeIfAbsent(texture, k -> new ArrayList<>());
            int last = path.length - 1;
            Vec3d[] sides = new Vec3d[path.length];
            for (int i = 0; i <= last; i++) {
                Vec3d along = path[Math.min(last, i + 1)].subtract(path[Math.max(0, i - 1)]);
                Vec3d side = along.crossProduct(this.eye.subtract(path[i]));
                sides[i] = side.lengthSquared() < 1.0E-6 ? this.right : side.normalize();
            }
            for (int i = 0; i < last; i++) {
                float t0 = i / (float) last;
                float t1 = (i + 1) / (float) last;
                Vec3d w0 = sides[i].multiply(MathHelper.lerp(t0, halfFrom, halfTo));
                Vec3d w1 = sides[i + 1].multiply(MathHelper.lerp(t1, halfFrom, halfTo));
                float a0 = MathHelper.lerp(t0, alphaFrom, alphaTo);
                float a1 = MathHelper.lerp(t1, alphaFrom, alphaTo);
                for (int pass = 0; pass < 2; pass++) {
                    List<float[]> into = pass == 0 ? list : lit;
                    float k = pass == 0 ? 1.0F : LIT;
                    into.add(corner(path[i].subtract(w0), color, a0 * k, 0.0F, 1.0F - t0 + scroll));
                    into.add(corner(path[i].add(w0), color, a0 * k, 1.0F, 1.0F - t0 + scroll));
                    into.add(corner(path[i + 1].add(w1), color, a1 * k, 1.0F, 1.0F - t1 + scroll));
                    into.add(corner(path[i + 1].subtract(w1), color, a1 * k, 0.0F, 1.0F - t1 + scroll));
                }
            }
        }

        // additive
        void glow(Vec3d c, float radius, int[] color, float alpha) {
            alpha *= this.soft;
            this.quad(this.additive, RhittaRenderLayers.GLOW, c, this.right, this.up, radius, radius, color, alpha, 0, 0, 1, 1);
        }

        // blended
        void halo(Vec3d c, float radius, int[] color, float alpha) {
            alpha *= this.soft;
            this.fire(RhittaRenderLayers.GLOW, c, this.right, this.up, radius, radius, color, alpha, 0, 0, 1, 1);
        }

        void flatGlow(Vec3d c, float radius, int[] color, float alpha) {
            alpha *= this.soft;
            this.fire(RhittaRenderLayers.GLOW, c, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), radius, radius,
                    color, alpha, 0, 0, 1, 1);
        }

        void flatRing(Vec3d c, float radius, int[] color, float alpha) {
            this.fire(RhittaRenderLayers.RING, c, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), radius, radius,
                    color, alpha, 0, 0, 1, 1);
        }

        void facingRing(Vec3d c, float radius, int[] color, float alpha) {
            this.fire(RhittaRenderLayers.RING, c, this.right, this.up, radius, radius, color, alpha, 0, 0, 1, 1);
        }

        // blended, then a bit additive
        private void fire(Identifier texture, Vec3d c, Vec3d u, Vec3d v, float hu, float hv, int[] color, float alpha,
                          float u0, float v0, float u1, float v1) {
            this.quad(this.blended, texture, c, u, v, hu, hv, color, alpha, u0, v0, u1, v1);
            this.quad(this.additive, texture, c, u, v, hu, hv, color, alpha * LIT, u0, v0, u1, v1);
        }

        void smoke(Vec3d c, float radius, float spin, int[] color, float alpha) {
            Vec3d u = this.right.multiply(MathHelper.cos(spin)).add(this.up.multiply(MathHelper.sin(spin)));
            Vec3d v = this.right.multiply(-MathHelper.sin(spin)).add(this.up.multiply(MathHelper.cos(spin)));
            this.quad(this.blended, RhittaRenderLayers.SMOKE, c, u, v, radius, radius, color, alpha, 0, 0, 1, 1);
        }

        void ray(Vec3d c, float a, float from, float to, float halfWidth, int[] color, float alpha) {
            Vec3d dir = this.right.multiply(MathHelper.cos(a)).add(this.up.multiply(MathHelper.sin(a)));
            Vec3d side = this.right.multiply(-MathHelper.sin(a)).add(this.up.multiply(MathHelper.cos(a)));
            Vec3d mid = c.add(dir.multiply((from + to) * 0.5F));
            this.quad(this.additive, RhittaRenderLayers.RAY, mid, side, dir, halfWidth, (to - from) * 0.5F, color, alpha, 0, 0, 1, 1);
        }

        // tapering strip from a to b with the texture running along it
        void streak(Identifier texture, Vec3d a, Vec3d b, Vec3d w, float halfA, float halfB, int[] color, float alpha, float scroll) {
            List<float[]> list = this.blended.computeIfAbsent(texture, k -> new ArrayList<>());
            float u0 = 0.0F;
            // the flame sheet has four tongues side by side, use the first
            float u1 = texture == RhittaRenderLayers.FLAMES ? 0.25F : 1.0F;
            list.add(corner(a.add(w.multiply(-halfA)), color, alpha, u0, 1 + scroll));
            list.add(corner(a.add(w.multiply(halfA)), color, alpha, u1, 1 + scroll));
            list.add(corner(b.add(w.multiply(halfB)), color, alpha * 0.2F, u1, scroll));
            list.add(corner(b.add(w.multiply(-halfB)), color, alpha * 0.2F, u0, scroll));
        }

        private void quad(Map<Identifier, List<float[]>> target, Identifier texture, Vec3d c, Vec3d u, Vec3d v,
                          float hu, float hv, int[] color, float alpha, float u0, float v0, float u1, float v1) {
            if (alpha <= 0.005F || hu <= 0.001F || hv <= 0.001F) {
                return;
            }
            List<float[]> list = target.computeIfAbsent(texture, k -> new ArrayList<>());
            Vec3d du = u.multiply(hu);
            Vec3d dv = v.multiply(hv);
            // v0 is the top edge (along +v), v1 the bottom edge
            list.add(corner(c.subtract(du).subtract(dv), color, alpha, u0, v1));
            list.add(corner(c.add(du).subtract(dv), color, alpha, u1, v1));
            list.add(corner(c.add(du).add(dv), color, alpha, u1, v0));
            list.add(corner(c.subtract(du).add(dv), color, alpha, u0, v0));
        }

        private static float[] corner(Vec3d p, int[] color, float alpha, float u, float v) {
            return new float[]{(float) p.x, (float) p.y, (float) p.z, color[0], color[1], color[2],
                    MathHelper.clamp(alpha, 0.0F, 1.0F) * 255, u, v};
        }

        void flush(Matrix4f m, VertexConsumerProvider consumers) {
            this.solid.forEach((texture, list) -> emit(m, VfxBuffer.of(consumers, AddonRenderLayers.solid(texture)), list));
            if (!this.depth.isEmpty()) {
                emit(m, VfxBuffer.of(consumers, AddonRenderLayers.depth(AddonRenderLayers.WHITE)), this.depth);
            }
            // smoke first so the fire glows over it
            this.blended.forEach((texture, list) -> emit(m, VfxBuffer.of(consumers, RhittaRenderLayers.blended(texture)), list));
            this.additive.forEach((texture, list) -> emit(m, VfxBuffer.of(consumers, RhittaRenderLayers.additive(texture)), list));
        }

        private static void emit(Matrix4f m, VfxBuffer vc, List<float[]> list) {
            for (float[] q : list) {
                vc.vertex(m, q[0], q[1], q[2], (int) q[3], (int) q[4], (int) q[5], (int) q[6], q[7], q[8]);
            }
        }
    }
}
