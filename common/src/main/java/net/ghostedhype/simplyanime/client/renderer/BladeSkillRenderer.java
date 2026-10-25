package net.ghostedhype.simplyanime.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ghostedhype.simplyanime.ability.BladeSkill;
import net.ghostedhype.simplyanime.ability.BladeSkillShapes;
import net.ghostedhype.simplyanime.ability.SlashWave;
import net.ghostedhype.simplyanime.animation.AnimationClip;
import net.ghostedhype.simplyanime.animation.AnimationPose;
import net.ghostedhype.simplyanime.client.pose.BladePoses;
import net.ghostedhype.simplyanime.config.Config;
import net.ghostedhype.simplyanime.entity.BladeSkillEntity;
import net.ghostedhype.simplyanime.entity.BladeTrail;
import net.ghostedhype.simplyanime.entity.SlashMark;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class BladeSkillRenderer extends EntityRenderer<BladeSkillEntity> {

    private static final int[] WHITE = {255, 255, 255};
    private static final int[] NIGHT = {6, 4, 18};
    private static final int[] STARLIGHT = {150, 195, 255};
    private static final int[] DEEP_BLUE = {40, 80, 255};
    private static final int[] TEAL = {50, 255, 222};
    private static final int[] FROST = {205, 255, 248};
    private static final int[] CRIMSON = {255, 26, 46};
    private static final int[] BLOOD = {120, 0, 16};
    private static final int[] EMBER = {255, 120, 70};
    private static final int[] HOT = {255, 236, 226};
    private static final int[] GHOST_PINK = {255, 70, 140};
    private static final int[] GHOST_BLUE = {110, 120, 255};

    private static final Vec3 UP = new Vec3(0, 1, 0);

    private Matrix4f matrix;
    private Vec3 origin;
    private Vec3 eye;
    private Vec3 camRight;
    private Vec3 camUp;
    private double ground;
    private float dim;

    public BladeSkillRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BladeSkillEntity entity) {
        return SimplyAnimeRenderTypes.WHITE;
    }

    @Override
    public boolean shouldRender(BladeSkillEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(BladeSkillEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light) {
        if (Later.take(entity, poseStack, (pose, late) -> this.render(entity, yaw, partialTick, pose, late, light))) {
            return;
        }
        // light doesn't cast shadows
        if (ShaderCompat.shadowPass()) {
            return;
        }
        this.matrix = poseStack.last().pose();
        this.origin = entity.getPosition(partialTick);
        this.eye = this.entityRenderDispatcher.camera.getPosition();
        this.ground = this.origin.y;
        this.dim = ShaderCompat.packNameHas("rethinking") ? 0.35F : ShaderCompat.packNameHas("complementary") ? 0.6F
                : ShaderCompat.packNameHas("photon") ? 1.6F : 1.0F;
        Quaternionf rotation = this.entityRenderDispatcher.cameraOrientation();
        this.camRight = VfxBuffer.rotate(rotation, 1, 0, 0);
        this.camUp = VfxBuffer.rotate(rotation, 0, 1, 0);

        BladeSkill skill = entity.getSkill();
        LivingEntity owner = entity.getOwner();
        float now = entity.clock(partialTick);
        float spin = entity.tickCount + partialTick;

        if (owner != null) {
            this.firstPerson(entity, owner, partialTick, poseStack, buffers);
        }

        VfxBuffer shade = VfxBuffer.of(buffers, SimplyAnimeRenderTypes.shade());
        if (skill == BladeSkill.STARBURST_STREAM) {
            this.ribbon(shade, entity.trails[0], now, 9.0F, 0.2F, 1.25F, NIGHT, 0.0F, NIGHT, 0.85F);
            for (SlashMark mark : entity.marks) {
                if (mark.blade == SlashMark.DARK) {
                    this.mark(shade, mark, now, spin, true);
                }
            }
        }

        VfxBuffer glow = VfxBuffer.of(buffers, SimplyAnimeRenderTypes.glow());
        switch (skill) {
            case VORPAL_STRIKE -> {
                this.ribbon(glow, entity.trails[0], now, 12.0F, 0.1F, 1.0F, BLOOD, 0.0F, CRIMSON, 0.85F);
                this.ribbon(glow, entity.trails[0], now, 12.0F, 1.0F, 1.5F, EMBER, 0.9F, HOT, 0.0F);
                this.vorpal(glow, entity, owner, now, spin, partialTick);
            }
            case HORIZONTAL_SQUARE -> {
                this.ribbon(glow, entity.trails[0], now, 8.0F, 0.1F, 1.0F, TEAL, 0.0F, TEAL, 0.75F);
                this.ribbon(glow, entity.trails[0], now, 8.0F, 1.0F, 1.5F, FROST, 0.95F, WHITE, 0.0F);
                this.square(glow, entity, now, spin);
            }
            case STARBURST_STREAM -> {
                this.ribbon(glow, entity.trails[0], now, 9.0F, 0.8F, 1.25F, DEEP_BLUE, 0.0F, STARLIGHT, 0.95F);
                this.ribbon(glow, entity.trails[0], now, 9.0F, 1.25F, 1.7F, WHITE, 0.95F, STARLIGHT, 0.0F);
                this.ribbon(glow, entity.trails[1], now, 9.0F, 0.15F, 1.15F, TEAL, 0.0F, TEAL, 0.8F);
                this.ribbon(glow, entity.trails[1], now, 9.0F, 1.15F, 1.7F, FROST, 0.95F, WHITE, 0.0F);
                this.starburst(glow, entity, owner, now, spin, partialTick);
            }
        }
        for (SlashMark mark : entity.marks) {
            this.mark(glow, mark, now, spin, false);
        }
        for (SlashWave wave : entity.waves) {
            this.wave(glow, wave, partialTick, TEAL, FROST, 0.35F, 1.0F);
        }
    }

    private void mark(VfxBuffer buffer, SlashMark mark, float now, float spin, boolean dark) {
        float age = now - mark.bornAt;
        if (age < 0.0F || age > SlashMark.LIFE) {
            return;
        }
        int[] body = mark.blade == SlashMark.TEAL ? TEAL : mark.blade == SlashMark.DARK ? DEEP_BLUE : STARLIGHT;
        int[] rim = mark.blade == SlashMark.TEAL ? FROST : WHITE;
        int[] ghost = mark.blade == SlashMark.TEAL ? GHOST_BLUE : GHOST_PINK;
        int last = mark.outer.length - 1;
        float shown = Mth.clamp(age / 1.5F, 0.0F, 1.0F) * last;
        float burn = Mth.clamp(1.0F - age / 9.0F, 0.0F, 1.0F);
        burn *= burn;
        float left = Mth.clamp(age / 3.0F, 0.0F, 1.0F) * (1.0F - age / SlashMark.LIFE);
        int beat = (int) (spin * 0.5F);
        for (int i = 0; i < last && i < shown; i++) {
            float thin = Mth.sin(Mth.PI * i / last);
            float thinNext = Mth.sin(Mth.PI * (i + 1) / last);
            Vec3 out = mark.outer[i];
            Vec3 outNext = mark.outer[i + 1];
            Vec3 in = out.lerp(mark.inner[i], thin * 0.7F);
            Vec3 inNext = outNext.lerp(mark.inner[i + 1], thinNext * 0.7F);
            if (dark) {
                this.quad(buffer, in, NIGHT, 0.0F, out, NIGHT, 0.9F * burn, outNext, NIGHT, 0.9F * burn, inNext, NIGHT, 0.0F);
                continue;
            }
            if (burn > 0.01F) {
                Vec3 edge = in.lerp(out, 0.6);
                Vec3 edgeNext = inNext.lerp(outNext, 0.6);
                Vec3 over = in.lerp(out, 1.14);
                Vec3 overNext = inNext.lerp(outNext, 1.14);
                float soft = mark.blade == SlashMark.DARK ? 0.0F : 0.4F * burn;
                this.quad(buffer, in, body, 0.0F, edge, body, soft, edgeNext, body, soft, inNext, body, 0.0F);
                this.quad(buffer, edge, body, Math.max(soft, 0.5F * burn), out, rim, burn, outNext, rim, burn,
                        edgeNext, body, Math.max(soft, 0.5F * burn));
                this.quad(buffer, out, rim, burn, over, rim, 0.0F, overNext, rim, 0.0F, outNext, rim, burn);
            }
            if (left <= 0.01F) {
                continue;
            }
            float key = hash(mark.seed + i * 13 + beat, 3);
            if (key < 0.28F) {
                continue;
            }
            // slid along the screen, in steps
            float slide = (float) Math.floor((hash(mark.seed + i * 7 + beat, 4) - 0.5F) * 6.0F) * 0.14F * Math.min(1.0F, age / 8.0F);
            Vec3 shift = this.camRight.scale(slide);
            Vec3 a = in.lerp(out, 0.84).add(shift);
            Vec3 b = out.add(shift);
            Vec3 c = outNext.add(shift);
            Vec3 d = inNext.lerp(outNext, 0.84).add(shift);
            int[] color = key > 0.86F ? WHITE : body;
            float alpha = left * (0.45F + 0.55F * key);
            this.quad(buffer, a, color, alpha * 0.4F, b, color, alpha, c, color, alpha, d, color, alpha * 0.4F);
            Vec3 apart = this.camRight.scale(0.22);
            this.quad(buffer, a.add(apart), ghost, 0.0F, b.add(apart), ghost, alpha * 0.45F, c.add(apart), ghost, alpha * 0.45F,
                    d.add(apart), ghost, 0.0F);
            if (key > 0.9F) {
                // a square knocked loose
                float size = 0.07F + 0.12F * hash(mark.seed + i, 5);
                Vec3 at = out.add(this.camRight.scale(slide * 3.0F)).add(this.camUp.scale((hash(mark.seed + i, 6) - 0.5F) * 0.8F));
                Vec3 right = this.camRight.scale(size);
                Vec3 up = this.camUp.scale(size);
                this.quad(buffer, at.subtract(right).subtract(up), rim, left, at.add(right).subtract(up), rim, left,
                        at.add(right).add(up), rim, left, at.subtract(right).add(up), rim, left);
            }
        }
    }

    private void ribbon(VfxBuffer buffer, BladeTrail trail, float now, float life, float from, float to,
                        int[] inner, float innerAlpha, int[] outer, float outerAlpha) {
        Vec3 lastA = null;
        Vec3 lastB = null;
        float lastStrength = 0.0F;
        for (int i = 0; i < trail.size(); i++) {
            float age = now - trail.time(i);
            if (age < 0.0F) {
                break;
            }
            if (age > life) {
                continue;
            }
            Vec3 base = trail.base(i);
            Vec3 reach = trail.tip(i).subtract(base);
            Vec3 a = base.add(reach.scale(from));
            Vec3 b = base.add(reach.scale(to));
            float fade = 1.0F - age / life;
            float strength = trail.heat(i) * fade * fade;
            if (lastA != null && (strength > 0.01F || lastStrength > 0.01F)) {
                this.quad(buffer, lastA, inner, innerAlpha * lastStrength, lastB, outer, outerAlpha * lastStrength,
                        b, outer, outerAlpha * strength, a, inner, innerAlpha * strength);
            }
            lastA = a;
            lastB = b;
            lastStrength = strength;
        }
    }

    private void wave(VfxBuffer buffer, SlashWave wave, float partialTick, int[] body, int[] edge, float bodyAlpha, float edgeAlpha) {
        float age = Math.max(0.0F, wave.age - 1.0F + partialTick);
        float travel = wave.travelled(age);
        float fade = Mth.clamp(age * 1.5F, 0.0F, 1.0F) * (1.0F - Mth.clamp((wave.speed * age - wave.range * 0.7F)
                / (wave.range * 0.3F + wave.speed * BladeSkillEntity.WAVE_FADE), 0.0F, 1.0F));
        if (fade <= 0.01F) {
            return;
        }
        Vec3 center = wave.centerAt(age);
        float half = wave.halfAt(age);
        Vec3 across = wave.axis;
        Vec3 ahead = wave.direction;
        Vec3 normal = across.cross(ahead).normalize();
        float bulge = half * 0.42F;
        float depth = Math.min(3.0F, 0.6F + half * 0.32F) * (1.0F + travel * 0.02F);
        if (edgeAlpha > 0.0F) {
            float born = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
            this.ring(buffer, wave.center, across, normal, half * (0.25F + 1.0F * born), 0.25F * (1.0F - born), edge,
                    0.7F * (1.0F - born) * edgeAlpha, 0.0F, 40);
            Vec3 scar = new Vec3(ahead.x, 0, ahead.z);
            if (scar.lengthSqr() > 0.01 && this.ground > -1000.0) {
                Vec3 from = new Vec3(wave.center.x, this.ground + 0.06, wave.center.z);
                Vec3 to = new Vec3(center.x, this.ground + 0.06, center.z);
                Vec3 wide = new Vec3(scar.z, 0, -scar.x).normalize().scale(0.12 + half * 0.07);
                this.quad(buffer, from.subtract(wide), body, 0.0F, from.add(wide), body, 0.0F,
                        to.add(wide), edge, 0.55F * fade, to.subtract(wide), edge, 0.55F * fade);
            }
        }
        int steps = Math.max(10, Math.min(28, (int) (half * 4.0F)));
        Vec3 lastFront = null;
        Vec3 lastBack = null;
        Vec3 lastTail = null;
        float lastTaper = 0.0F;
        for (int i = 0; i <= steps; i++) {
            float s = -1.0F + 2.0F * i / steps;
            float taper = 1.0F - s * s;
            Vec3 front = center.add(across.scale(half * s)).add(ahead.scale(bulge * taper));
            Vec3 back = front.subtract(ahead.scale(depth * taper));
            Vec3 tail = front.subtract(ahead.scale(depth * 3.2F * taper));
            if (lastFront != null) {
                // bright leading edge
                this.quad(buffer, lastFront, edge, edgeAlpha * fade, front, edge, edgeAlpha * fade,
                        back, body, Math.max(bodyAlpha, edgeAlpha * 0.45F) * fade * taper,
                        lastBack, body, Math.max(bodyAlpha, edgeAlpha * 0.45F) * fade * lastTaper);
                // long soft tail
                this.quad(buffer, lastBack, body, Math.max(bodyAlpha, edgeAlpha * 0.45F) * fade * lastTaper,
                        back, body, Math.max(bodyAlpha, edgeAlpha * 0.45F) * fade * taper, tail, body, 0.0F, lastTail, body, 0.0F);
                // the same blade stood on its side
                float rise = depth * 0.55F;
                Vec3 top = front.add(normal.scale(rise * taper));
                Vec3 bottom = front.subtract(normal.scale(rise * taper));
                Vec3 lastTop = lastFront.add(normal.scale(rise * lastTaper));
                Vec3 lastBottom = lastFront.subtract(normal.scale(rise * lastTaper));
                this.quad(buffer, lastFront, edge, edgeAlpha * fade * 0.8F, front, edge, edgeAlpha * fade * 0.8F,
                        top, body, 0.0F, lastTop, body, 0.0F);
                this.quad(buffer, lastFront, edge, edgeAlpha * fade * 0.8F, front, edge, edgeAlpha * fade * 0.8F,
                        bottom, body, 0.0F, lastBottom, body, 0.0F);
            }
            lastFront = front;
            lastBack = back;
            lastTail = tail;
            lastTaper = taper;
        }
    }

    private void vorpal(VfxBuffer glow, BladeSkillEntity entity, LivingEntity owner, float now, float spin, float partialTick) {
        AnimationClip clip = entity.getSkill().clip();
        float perTick = clip.framesPerTick();
        BladeTrail trail = entity.trails[0];
        float chargeStart = clip.marker("charge_start") / perTick;
        float lunge = clip.marker("lunge_start") / perTick;

        if (trail.size() > 0 && now >= chargeStart && entity.impactAt < 0) {
            Vec3 base = trail.base(trail.size() - 1);
            Vec3 tip = trail.tip(trail.size() - 1);
            float charge = Mth.clamp((now - chargeStart) / (lunge - chargeStart), 0.0F, 1.0F);
            Vec3 lit = base.lerp(tip, Math.min(1.0F, charge * 1.4F));
            this.line(glow, base, lit, 0.16F + 0.3F * charge, CRIMSON, 0.7F, CRIMSON, 0.7F);
            this.line(glow, base, lit, 0.06F + 0.1F * charge, HOT, 0.95F, HOT, 0.95F);
            Vec3 along = tip.subtract(base).normalize();
            Vec3 side = SlashWave.perpendicular(along);
            Vec3 other = along.cross(side);
            for (int i = 0; i < 3; i++) {
                float t = Mth.frac(charge * 1.5F + i / 3.0F);
                float radius = 3.2F * (1.0F - t) + 0.25F;
                this.ring(glow, base.lerp(tip, 0.25F + 0.3F * i), side, other, radius, 0.1F + 0.12F * charge, CRIMSON,
                        0.85F * t * charge, spin * 0.25F, 40);
            }
            this.flare(glow, tip, 0.5F + 1.6F * charge, CRIMSON, 0.55F * charge);
            this.flare(glow, tip, 0.25F + 0.6F * charge, HOT, 0.9F * charge);
            if (owner != null) {
                Vec3 feet = owner.getPosition(partialTick).add(0, 0.06, 0);
                this.ring(glow, feet, new Vec3(1, 0, 0), new Vec3(0, 0, 1), 1.2F + 2.4F * charge, 0.14F, CRIMSON, 0.7F * charge,
                        spin * 0.1F, 48);
                this.ring(glow, feet, new Vec3(1, 0, 0), new Vec3(0, 0, 1), 0.8F + 1.2F * charge, 0.08F, EMBER, 0.6F * charge,
                        -spin * 0.16F, 48);
            }
        }
        if (entity.impactAt < 0) {
            return;
        }

        float since = now - entity.impactAt;
        float length = (float) Config.weapons.elucidator.lanceLength;
        float radius = (float) Config.weapons.elucidator.lanceRadius;
        Vec3 aim = entity.getAim();
        Vec3 start = BladeSkillShapes.lanceOrigin(clip, entity.getPosition(partialTick), entity.getYaw(), entity.isMirror());
        float out = 1.0F - (1.0F - Mth.clamp(since / 2.0F, 0.0F, 1.0F)) * (1.0F - Mth.clamp(since / 2.0F, 0.0F, 1.0F));
        float gone = Mth.clamp((since - 8.0F) / 14.0F, 0.0F, 1.0F);
        if (since > 34.0F) {
            return;
        }
        float thick = (1.0F - gone) * (1.0F - gone);
        float reach = length * out;
        float tail = reach * gone * 0.85F;
        int steps = 36;
        for (int layer = 0; layer < 3; layer++) {
            int[] color = layer == 0 ? CRIMSON : layer == 1 ? EMBER : HOT;
            float width = radius * (layer == 0 ? 1.0F : layer == 1 ? 0.62F : 0.3F) * thick;
            float alpha = layer == 0 ? 0.55F : layer == 1 ? 0.8F : 1.0F;
            Vec3 last = null;
            float lastWidth = 0.0F;
            for (int i = 0; i <= steps; i++) {
                float s = (float) i / steps;
                float at = Mth.lerp(s, tail, reach);
                float shape = s < 0.3F ? 0.35F + 0.65F * Mth.sin(s / 0.3F * Mth.HALF_PI) : Mth.sqrt(Math.max(0.0F, (1.0F - s) / 0.7F));
                float pulse = 1.0F + 0.08F * Mth.sin(spin * 1.3F - at * 0.9F);
                Vec3 point = start.add(aim.scale(at));
                float w = width * shape * pulse;
                if (last != null) {
                    this.segment(glow, last, lastWidth, point, w, color, alpha, aim);
                }
                last = point;
                lastWidth = w;
            }
        }
        Vec3 side = SlashWave.perpendicular(aim);
        Vec3 other = aim.cross(side);
        Vec3 lastA = null;
        Vec3 lastB = null;
        for (int i = 0; i <= 120; i++) {
            float at = Mth.lerp(i / 120.0F, tail, reach);
            float angle = at * 1.1F - spin * 0.9F;
            float wide = radius * 1.15F * thick * Math.min(1.0F, at / 3.0F) * Math.min(1.0F, (reach - at) / 4.0F);
            Vec3 around = side.scale(Mth.cos(angle)).add(other.scale(Mth.sin(angle)));
            Vec3 a = start.add(aim.scale(at)).add(around.scale(wide));
            Vec3 b = a.add(aim.scale(0.55)).add(around.scale(0.12));
            if (lastA != null) {
                this.quad(glow, lastA, CRIMSON, 0.75F * thick, lastB, EMBER, 0.0F, b, EMBER, 0.0F, a, CRIMSON, 0.75F * thick);
            }
            lastA = a;
            lastB = b;
        }
        for (int i = 0; i < 11; i++) {
            float at = 1.5F + i * (length - 3.0F) / 11.0F;
            float born = at / length * 2.0F;
            float life = (since - born) / 11.0F;
            if (life > 0.0F && life < 1.0F && at <= reach) {
                this.ring(glow, start.add(aim.scale(at)), side, other, radius * (0.8F + 3.8F * life), 0.32F * (1.0F - life),
                        i % 2 == 0 ? EMBER : CRIMSON, 0.85F * (1.0F - life), spin * 0.2F, 56);
            }
        }
        Vec3 flat = new Vec3(aim.x, 0, aim.z);
        if (flat.lengthSqr() > 0.01) {
            Vec3 wide = new Vec3(flat.z, 0, -flat.x).normalize().scale(radius * 0.9F);
            Vec3 from = new Vec3(start.x, this.ground + 0.07, start.z);
            Vec3 to = from.add(flat.scale(reach));
            float burn = 1.0F - Mth.clamp((since - 10.0F) / 22.0F, 0.0F, 1.0F);
            this.quad(glow, from.subtract(wide), CRIMSON, 0.0F, from, EMBER, 0.6F * burn, to, EMBER, 0.6F * burn, to.subtract(wide), CRIMSON, 0.0F);
            this.quad(glow, from, EMBER, 0.6F * burn, from.add(wide), CRIMSON, 0.0F, to.add(wide), CRIMSON, 0.0F, to, EMBER, 0.6F * burn);
        }
        this.flare(glow, start, radius * 2.4F * thick, CRIMSON, 0.7F * thick);
        this.flare(glow, start, radius * 1.1F * thick, HOT, 0.9F * thick);
    }

    private void square(VfxBuffer glow, BladeSkillEntity entity, float now, float spin) {
        List<Vec3> corners = entity.corners;
        Vec3 center = entity.position().add(0, corners.isEmpty() ? 1.1 : corners.get(0).y - entity.getY(), 0);
        float max = (float) Config.weapons.dark_repulser.releaseRadius;
        float released = entity.releasedAt < 0 ? -1.0F : now - entity.releasedAt;
        // out from where the blade drew it once it lets go
        float grow = released < 0 ? 1.0F : BladeSkillShapes.releaseHalf(released, max) / BladeSkillShapes.SQUARE_HALF;
        float gone = released < 0 ? 0.0F : Mth.clamp((released - BladeSkillShapes.RELEASE_TICKS * 0.6F) / 9.0F, 0.0F, 1.0F);
        if (gone >= 1.0F) {
            return;
        }
        float strength = 1.0F - gone;
        float tall = released < 0 ? 0.55F : 0.55F + 2.6F * Math.min(1.0F, released / 4.0F);

        for (int i = 0; i < corners.size(); i++) {
            Vec3 corner = center.add(corners.get(i).subtract(center).scale(grow));
            float lit = Mth.clamp((now - entity.cornerTimes.get(i)) / 2.0F, 0.0F, 1.0F);
            this.flare(glow, corner, (0.5F + 1.4F * (1.0F - lit) + 0.25F * grow) * strength, FROST, 0.9F * strength);
            this.line(glow, corner.subtract(0, 1.2 + tall, 0), corner.add(0, 2.4 + tall * 1.5, 0), 0.09F + 0.05F * grow, TEAL, 0.0F,
                    FROST, 0.85F * strength * lit);
            if (i == 0) {
                continue;
            }
            Vec3 before = center.add(corners.get(i - 1).subtract(center).scale(grow));
            // the edge itself, with a curtain of light standing on it
            this.line(glow, before, corner, 0.12F + 0.04F * grow, FROST, strength, FROST, strength);
            this.quad(glow, before, TEAL, 0.6F * strength, corner, TEAL, 0.6F * strength,
                    corner.add(0, tall, 0), TEAL, 0.0F, before.add(0, tall, 0), TEAL, 0.0F);
            this.quad(glow, before, TEAL, 0.6F * strength, corner, TEAL, 0.6F * strength,
                    corner.subtract(0, tall * 0.6, 0), TEAL, 0.0F, before.subtract(0, tall * 0.6, 0), TEAL, 0.0F);
            if (released >= 0) {
                // frost running along the ground under it
                Vec3 low = new Vec3(0, entity.getY() + 0.08 - corner.y, 0);
                Vec3 inward = center.subtract(before.add(corner).scale(0.5)).multiply(1, 0, 1).normalize().scale(1.6);
                this.quad(glow, before.add(low), FROST, 0.8F * strength, corner.add(low), FROST, 0.8F * strength,
                        corner.add(low).add(inward), TEAL, 0.0F, before.add(low).add(inward), TEAL, 0.0F);
            }
        }
        // the blade drawing the open side
        BladeTrail trail = entity.trails[0];
        if (released < 0 && !corners.isEmpty() && corners.size() < 5 && trail.size() > 0) {
            Vec3 tip = null;
            for (int i = trail.size() - 1; i >= 0 && tip == null; i--) {
                if (trail.time(i) <= now) {
                    tip = trail.tip(i);
                }
            }
            if (tip != null) {
                Vec3 from = corners.get(corners.size() - 1);
                this.line(glow, from, tip, 0.12F, FROST, 1.0F, WHITE, 1.0F);
                this.flare(glow, tip, 0.55F, WHITE, 0.9F);
            }
        }
        if (corners.size() >= 5) {
            float closed = Mth.clamp((now - entity.cornerTimes.get(4)) / 3.0F, 0.0F, 1.0F);
            float sheet = released < 0 ? 0.22F * closed : 0.6F * Math.max(0.0F, 1.0F - released / 5.0F);
            if (sheet > 0.01F) {
                Vec3 a = center.add(corners.get(0).subtract(center).scale(grow));
                Vec3 b = center.add(corners.get(1).subtract(center).scale(grow));
                Vec3 c = center.add(corners.get(2).subtract(center).scale(grow));
                Vec3 d = center.add(corners.get(3).subtract(center).scale(grow));
                this.quad(glow, a, TEAL, sheet, b, TEAL, sheet, c, TEAL, sheet, d, TEAL, sheet);
            }
            this.ring(glow, center, new Vec3(1, 0, 0), new Vec3(0, 0, 1), 0.9F + 0.2F * Mth.sin(spin * 0.4F), 0.07F, FROST,
                    0.7F * closed * strength, spin * 0.2F, 32);
        }
    }

    private void starburst(VfxBuffer glow, BladeSkillEntity entity, LivingEntity owner, float now, float spin, float partialTick) {
        AnimationClip clip = entity.getSkill().clip();
        float perTick = clip.framesPerTick();
        float length = clip.lengthTicks();
        float launch = clip.marker("launch") / perTick;
        float coil = clip.marker("coil") / perTick;
        Vec3 east = new Vec3(1, 0, 0);
        Vec3 south = new Vec3(0, 0, 1);

        if (owner != null && now < length) {
            Vec3 feet = owner.getPosition(partialTick).add(0, 0.07, 0);
            float rise = Mth.clamp(now / 6.0F, 0.0F, 1.0F) * Mth.clamp((length - now) / 10.0F, 0.0F, 1.0F);
            float sinceHit = now - entity.lastHitAt;
            float beat = sinceHit >= 0.0F && sinceHit < 5.0F ? 1.0F - sinceHit / 5.0F : 0.0F;
            this.stream(glow, feet.add(0, owner.getBbHeight() * 0.6, 0), entity.facing(owner, partialTick), spin,
                    rise * Mth.clamp((now - launch) / 10.0F, 0.0F, 1.0F), entity.finaleAt >= 0);
            this.line(glow, feet, feet.add(0, 11.0 + 3.0 * beat, 0), 1.3F + 0.5F * beat, STARLIGHT, 0.22F * rise, DEEP_BLUE, 0.0F);
            this.line(glow, feet, feet.add(0, 7.0 + 2.0 * beat, 0), 0.45F + 0.25F * beat, WHITE, (0.3F + 0.3F * beat) * rise, STARLIGHT, 0.0F);
            float wide = 2.6F + 2.2F * Mth.clamp(now / coil, 0.0F, 1.0F);
            this.ring(glow, feet, east, south, wide, 0.16F, STARLIGHT, 0.75F * rise, spin * 0.09F, 64);
            this.ring(glow, feet, east, south, wide * 0.66F, 0.1F, TEAL, 0.6F * rise, -spin * 0.13F, 48);
            this.ring(glow, feet, east, south, wide * 0.36F, 0.07F, WHITE, 0.6F * rise, spin * 0.2F, 32);
            for (int i = 0; i < 12; i++) {
                float angle = spin * 0.09F + i * Mth.TWO_PI / 12.0F;
                Vec3 spoke = east.scale(Mth.cos(angle)).add(south.scale(Mth.sin(angle)));
                this.star(glow, feet.add(spoke.scale(wide)), east, south, 0.42F, i % 2 == 0 ? WHITE : STARLIGHT, 0.9F * rise, -spin * 0.2F);
                // light climbing off the ring
                float climb = Mth.frac(spin * 0.045F + i * 0.37F);
                Vec3 foot = feet.add(spoke.scale(wide * (0.5F + 0.5F * Mth.frac(i * 0.618F))));
                this.line(glow, foot.add(0, climb * 4.5, 0), foot.add(0, climb * 4.5 + 1.3, 0), 0.05F, STARLIGHT, 0.0F, WHITE,
                        0.7F * rise * (1.0F - climb));
            }
            // every hit kicks a ring out along the ground
            float since = now - entity.lastHitAt;
            if (since >= 0.0F && since < 7.0F) {
                float t = since / 7.0F;
                this.ring(glow, feet, east, south, 1.0F + 7.5F * t, 0.3F * (1.0F - t), WHITE, 0.8F * (1.0F - t), 0.0F, 64);
            }
            boolean breath = now >= coil && entity.finaleAt < 0;
            if (now < launch || breath) {
                float t = breath ? Mth.clamp((now - coil) / 6.0F, 0.0F, 1.0F) : Mth.clamp(now / launch, 0.0F, 1.0F);
                Vec3 chest = owner.getPosition(partialTick).add(0, owner.getBbHeight() * 0.62, 0);
                for (int i = 0; i < 3; i++) {
                    float shrink = Mth.frac(t * 1.2F + i / 3.0F);
                    this.ring(glow, chest, this.camRight, this.camUp, (breath ? 7.0F : 4.5F) * (1.0F - shrink) + 0.3F, 0.1F,
                            i == 1 ? TEAL : STARLIGHT, 0.8F * shrink, spin * 0.1F, 48);
                }
                this.flare(glow, chest, 0.8F + 2.2F * t, STARLIGHT, 0.5F * t);
                this.flare(glow, chest, 0.4F + 0.9F * t, WHITE, 0.8F * t);
            }
        }

        if (entity.finaleAt < 0) {
            return;
        }
        float since = now - entity.finaleAt;
        float radius = (float) Config.weapons.starburst_stream.burstRadius;
        Vec3 center = entity.crossCenter;
        float open = 1.0F - (1.0F - Mth.clamp(since / 7.0F, 0.0F, 1.0F)) * (1.0F - Mth.clamp(since / 7.0F, 0.0F, 1.0F));
        float fade = 1.0F - Mth.clamp((since - 6.0F) / 26.0F, 0.0F, 1.0F);
        if (fade <= 0.0F) {
            return;
        }
        Vec3 toEye = this.eye.subtract(center).normalize();
        Vec3 flatRight = toEye.cross(UP).normalize();
        Vec3 flatUp = flatRight.cross(toEye).normalize();
        // shells blown outward in three planes
        Vec3[][] planes = {{east, south}, {east, UP}, {south, UP}};
        for (int i = 0; i < planes.length; i++) {
            float turn = spin * 0.03F + i;
            Vec3 a = planes[i][0].scale(Mth.cos(turn)).add(planes[i][1].scale(Mth.sin(turn)));
            Vec3 b = planes[i][0].scale(-Mth.sin(turn)).add(planes[i][1].scale(Mth.cos(turn)));
            Vec3 lift = a.cross(b);
            this.ring(glow, center, a, lift, radius * open, 0.5F * fade, i == 1 ? TEAL : STARLIGHT, 0.85F * fade, 0.0F, 72);
            this.ring(glow, center, a, lift, radius * open * 0.62F, 0.25F * fade, WHITE, 0.8F * fade, 0.0F, 56);
        }
        // points of the star
        for (int i = 0; i < 16; i++) {
            float angle = i * Mth.TWO_PI / 16.0F + 0.2F;
            Vec3 ray = flatRight.scale(Mth.cos(angle)).add(flatUp.scale(Mth.sin(angle)));
            float reach = radius * (i % 2 == 0 ? 1.7F : 1.0F) * open;
            this.line(glow, center, center.add(ray.scale(reach)), (i % 2 == 0 ? 0.55F : 0.32F) * fade, WHITE, fade, STARLIGHT, 0.0F);
        }
        this.flare(glow, center, radius * 1.5F * (0.4F + 0.6F * open), STARLIGHT, 0.75F * fade);
        this.flare(glow, center, radius * 0.7F * (0.4F + 0.6F * open), WHITE, fade);
        Vec3 floor = new Vec3(center.x, entity.getY() + 0.08, center.z);
        this.ring(glow, floor, east, south, radius * 1.6F * open, 0.6F * fade, WHITE, 0.8F * fade, 0.0F, 72);
        this.ring(glow, floor, east, south, radius * 1.05F * open, 0.3F * fade, TEAL, 0.7F * fade, 0.0F, 64);
        float late = Mth.clamp((since - 5.0F) / 12.0F, 0.0F, 1.0F);
        this.ring(glow, floor, east, south, radius * 2.6F * late, 0.9F * (1.0F - late), STARLIGHT, 0.7F * (1.0F - late), 0.0F, 96);
        float pillar = fade * Mth.clamp(since / 3.0F, 0.0F, 1.0F);
        this.line(glow, floor, floor.add(0, 80, 0), radius * 0.55F * (0.5F + 0.5F * fade), STARLIGHT, 0.5F * pillar, DEEP_BLUE, 0.0F);
        this.line(glow, floor, floor.add(0, 60, 0), radius * 0.2F, WHITE, 0.8F * pillar, STARLIGHT, 0.0F);
    }

    private void stream(VfxBuffer glow, Vec3 heart, float facing, float spin, float strength, boolean surge) {
        if (strength <= 0.01F) {
            return;
        }
        float angle = facing * Mth.DEG_TO_RAD;
        Vec3 ahead = new Vec3(-Mth.sin(angle), 0, Mth.cos(angle));
        Vec3 side = new Vec3(ahead.z, 0, -ahead.x);
        float reach = 26.0F;
        for (int i = 0; i < 90; i++) {
            float lane = hash(i, 1);
            float pace = (0.9F + hash(i, 2) * 1.6F) * (surge ? 2.2F : 1.0F);
            float at = Mth.frac(hash(i, 3) + spin * pace / reach);
            float around = hash(i, 4) * Mth.TWO_PI + spin * 0.01F;
            float out = 1.6F + lane * lane * 7.5F;
            Vec3 from = heart.add(ahead.scale(at * reach - reach * 0.3F))
                    .add(side.scale(Mth.cos(around) * out)).add(0, Mth.sin(around) * out * 0.75F + 0.6F, 0);
            float tail = 1.2F + hash(i, 5) * 3.6F;
            int[] color = i % 4 == 0 ? TEAL : i % 4 == 1 ? WHITE : STARLIGHT;
            float seen = strength * Mth.sin(at * Mth.PI) * (0.45F + 0.55F * hash(i, 6));
            this.line(glow, from.subtract(ahead.scale(tail)), from, 0.05F + 0.07F * hash(i, 7), color, 0.0F, color, seen);
        }
    }

    private static float hash(int index, int salt) {
        return Mth.frac(Mth.sin(index * 12.9898F + salt * 78.233F) * 43758.547F);
    }

    private void firstPerson(BladeSkillEntity entity, LivingEntity owner, float partialTick, PoseStack poseStack,
                             MultiBufferSource buffers) {
        Minecraft client = Minecraft.getInstance();
        if (owner != client.getCameraEntity() || !client.options.getCameraType().isFirstPerson()
                || !(owner instanceof AbstractClientPlayer player) || !entity.isRunning()
                || !(this.entityRenderDispatcher.getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        AnimationPose pose = BladePoses.pose(owner, partialTick);
        if (pose == null) {
            return;
        }
        int light = this.entityRenderDispatcher.getPackedLightCoords(owner, partialTick);
        Vec3 at = owner.getPosition(partialTick).subtract(this.origin);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        BladePoses.root(owner, poseStack, partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot)));
        poseStack.scale(-0.9375F, -0.9375F, 0.9375F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        RenderType skin = RenderType.entityTranslucent(player.getSkin().texture());
        ModelPart[] arms = {model.rightArm, model.leftArm};
        ModelPart[] sleeves = {model.rightSleeve, model.leftSleeve};
        for (int i = 0; i < 2; i++) {
            int part = AnimationClip.RIGHT_ARM + i;
            if (pose.weight[part] <= 0.0F) {
                continue;
            }
            ModelPart arm = arms[i];
            Vector3f position = pose.position[part];
            arm.x = position.x;
            arm.y = position.y;
            arm.z = position.z;
            BladePoses.turn(arm, pose.rotation[part]);
            arm.visible = true;
            sleeves[i].copyFrom(arm);
            sleeves[i].visible = true;
            arm.render(poseStack, buffers.getBuffer(skin), light, OverlayTexture.NO_OVERLAY);
            sleeves[i].render(poseStack, buffers.getBuffer(skin), light, OverlayTexture.NO_OVERLAY);
        }
        BladePoses.renderItem(owner, owner.getMainHandItem(), ItemDisplayContext.NONE, owner.getMainArm(), poseStack, buffers, light);
        BladePoses.renderItem(owner, owner.getOffhandItem(), ItemDisplayContext.NONE, owner.getMainArm().getOpposite(), poseStack, buffers, light);
        poseStack.popPose();
    }

    private void vertex(VfxBuffer buffer, Vec3 at, int[] color, float alpha) {
        buffer.vertex(this.matrix, (float) (at.x - this.origin.x), (float) (at.y - this.origin.y), (float) (at.z - this.origin.z),
                color[0], color[1], color[2], (int) (Mth.clamp(alpha * (color == NIGHT ? 1.0F : this.dim), 0.0F, 1.0F) * 255.0F), 0.0F, 0.0F);
    }

    private void quad(VfxBuffer buffer, Vec3 a, int[] colorA, float alphaA, Vec3 b, int[] colorB, float alphaB,
                      Vec3 c, int[] colorC, float alphaC, Vec3 d, int[] colorD, float alphaD) {
        this.vertex(buffer, a, colorA, alphaA);
        this.vertex(buffer, b, colorB, alphaB);
        this.vertex(buffer, c, colorC, alphaC);
        this.vertex(buffer, d, colorD, alphaD);
    }

    private void line(VfxBuffer buffer, Vec3 from, Vec3 to, float width, int[] colorFrom, float alphaFrom, int[] colorTo, float alphaTo) {
        Vec3 along = to.subtract(from);
        Vec3 side = along.cross(this.eye.subtract(from));
        if (side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize().scale(width);
        this.quad(buffer, from.subtract(side), colorFrom, 0.0F, from, colorFrom, alphaFrom, to, colorTo, alphaTo,
                to.subtract(side), colorTo, 0.0F);
        this.quad(buffer, from, colorFrom, alphaFrom, from.add(side), colorFrom, 0.0F, to.add(side), colorTo, 0.0F,
                to, colorTo, alphaTo);
    }

    // one length of a beam whose width changes along it
    private void segment(VfxBuffer buffer, Vec3 from, float widthFrom, Vec3 to, float widthTo, int[] color, float alpha, Vec3 axis) {
        Vec3 side = axis.cross(this.eye.subtract(from));
        if (side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize();
        this.quad(buffer, from.subtract(side.scale(widthFrom)), color, 0.0F, from, color, alpha, to, color, alpha,
                to.subtract(side.scale(widthTo)), color, 0.0F);
        this.quad(buffer, from, color, alpha, from.add(side.scale(widthFrom)), color, 0.0F, to.add(side.scale(widthTo)), color, 0.0F,
                to, color, alpha);
    }

    private void ring(VfxBuffer buffer, Vec3 center, Vec3 axisA, Vec3 axisB, float radius, float band, int[] color, float alpha,
                      float turn, int steps) {
        if (alpha <= 0.01F || radius <= 0.0F) {
            return;
        }
        Vec3 lastIn = null;
        Vec3 lastMid = null;
        Vec3 lastOut = null;
        for (int i = 0; i <= steps; i++) {
            float angle = turn + i * Mth.TWO_PI / steps;
            Vec3 spoke = axisA.scale(Mth.cos(angle)).add(axisB.scale(Mth.sin(angle)));
            Vec3 in = center.add(spoke.scale(Math.max(0.0F, radius - band)));
            Vec3 mid = center.add(spoke.scale(radius));
            Vec3 out = center.add(spoke.scale(radius + band));
            if (lastIn != null) {
                this.quad(buffer, lastIn, color, 0.0F, lastMid, color, alpha, mid, color, alpha, in, color, 0.0F);
                this.quad(buffer, lastMid, color, alpha, lastOut, color, 0.0F, out, color, 0.0F, mid, color, alpha);
            }
            lastIn = in;
            lastMid = mid;
            lastOut = out;
        }
    }

    // a soft round glow facing the camera
    private void flare(VfxBuffer buffer, Vec3 center, float radius, int[] color, float alpha) {
        if (alpha <= 0.01F || radius <= 0.0F) {
            return;
        }
        // from inside it a glow would be the whole screen
        alpha *= Mth.clamp((float) this.eye.distanceTo(center) / (radius * 2.5F), 0.12F, 1.0F);
        int steps = 16;
        Vec3 last = null;
        for (int i = 0; i <= steps; i++) {
            float angle = i * Mth.TWO_PI / steps;
            Vec3 rim = center.add(this.camRight.scale(Mth.cos(angle) * radius)).add(this.camUp.scale(Mth.sin(angle) * radius));
            if (last != null) {
                this.quad(buffer, center, color, alpha, center, color, alpha, rim, color, 0.0F, last, color, 0.0F);
            }
            last = rim;
        }
    }

    // a four pointed star lying in the plane of two axes
    private void star(VfxBuffer buffer, Vec3 center, Vec3 axisA, Vec3 axisB, float size, int[] color, float alpha, float turn) {
        if (alpha <= 0.01F) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            float angle = turn + i * Mth.HALF_PI;
            Vec3 point = axisA.scale(Mth.cos(angle)).add(axisB.scale(Mth.sin(angle)));
            Vec3 side = axisA.scale(-Mth.sin(angle)).add(axisB.scale(Mth.cos(angle))).scale(size * 0.18);
            this.quad(buffer, center.subtract(side), color, alpha, center.add(side), color, alpha,
                    center.add(point.scale(size)), color, 0.0F, center.add(point.scale(size)), color, 0.0F);
        }
    }
}
