package net.ghostedhype.simplyanime.entity;

import net.ghostedhype.simplyanime.ability.AbilityHelper;
import net.ghostedhype.simplyanime.ability.BladeSkill;
import net.ghostedhype.simplyanime.ability.BladeSkillShapes;
import net.ghostedhype.simplyanime.ability.SlashWave;
import net.ghostedhype.simplyanime.animation.AnimationClip;
import net.ghostedhype.simplyanime.config.Config;
import net.ghostedhype.simplyanime.config.StarburstSettings;
import net.ghostedhype.simplyanime.registry.EntityRegistry;
import net.ghostedhype.simplyanime.registry.ParticlesRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

public class BladeSkillEntity extends PhasedEffectEntity {

    public static final Map<Integer, BladeSkillEntity> CLIENT_BY_OWNER = new HashMap<>();
    public static final Set<BladeSkillEntity> CLIENT_ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());

    private static final EntityDataAccessor<Integer> SKILL =
            SynchedEntityData.defineId(BladeSkillEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MIRROR =
            SynchedEntityData.defineId(BladeSkillEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> YAW =
            SynchedEntityData.defineId(BladeSkillEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Vector3f> AIM =
            SynchedEntityData.defineId(BladeSkillEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> LIFETIME =
            SynchedEntityData.defineId(BladeSkillEntity.class, EntityDataSerializers.INT);

    // how long a spent cut still shows while it fades
    public static final float WAVE_FADE = 5.0F;

    public final BladeTrail[] trails = {new BladeTrail(), new BladeTrail()};
    public final List<SlashWave> waves = new ArrayList<>();
    public final List<Vec3> corners = new ArrayList<>();
    public final List<Float> cornerTimes = new ArrayList<>();
    public float releasedAt = -1.0F;
    public float impactAt = -1.0F;
    public float finaleAt = -1.0F;
    public float lastHitAt = -100.0F;
    public final List<SlashMark> marks = new ArrayList<>();
    public Vec3 crossCenter = Vec3.ZERO;

    public BladeSkillEntity(EntityType<? extends BladeSkillEntity> type, Level world) {
        super(type, world);
    }

    public BladeSkillEntity(Level world, LivingEntity owner, BladeSkill skill, boolean mirror, int lifetime) {
        this(EntityRegistry.BLADE_SKILL.get(), world);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setOwner(owner);
        this.entityData.set(SKILL, skill.ordinal());
        this.entityData.set(MIRROR, mirror);
        this.entityData.set(YAW, owner.getYHeadRot());
        this.entityData.set(LIFETIME, lifetime);
        this.setPhaseLength(skill.clip().lengthTicks());
    }

    @Override
    protected void defineData(DataDefiner definer) {
        super.defineData(definer);
        definer.define(SKILL, 0);
        definer.define(MIRROR, false);
        definer.define(YAW, 0.0F);
        definer.define(AIM, new Vector3f(0, 0, 1));
        definer.define(LIFETIME, 200);
    }

    public BladeSkill getSkill() {
        return BladeSkill.values()[Mth.clamp(this.entityData.get(SKILL), 0, BladeSkill.values().length - 1)];
    }

    public boolean isMirror() {
        return this.entityData.get(MIRROR);
    }

    // the facing a skill was locked to
    public float getYaw() {
        return this.entityData.get(YAW);
    }

    public Vec3 getAim() {
        return unpack(this.entityData.get(AIM));
    }

    public void aim(Vec3 direction, float yaw) {
        this.entityData.set(AIM, pack(direction));
        this.entityData.set(YAW, yaw);
    }

    // true while the wielder is still inside the animation
    public boolean isRunning() {
        return this.getPhaseTicks(0.0F) < this.getSkill().clip().lengthTicks();
    }

    public float clock(float partialTick) {
        return Math.max(0.0F, this.getPhaseTicks(partialTick) - 1.0F);
    }

    // frame of the clip the wielder is drawn at
    public float frame(float partialTick) {
        return this.clock(partialTick) * this.getSkill().clip().framesPerTick();
    }

    // which way the wielder's body points for this skill
    public float facing(LivingEntity owner, float partialTick) {
        return this.getSkill() == BladeSkill.STARBURST_STREAM || this.getSkill() == BladeSkill.VORPAL_STRIKE && this.impactAt < 0
                ? Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot) : this.getYaw();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.tickCount > this.entityData.get(LIFETIME)) {
                this.discard();
            }
            return;
        }
        CLIENT_ACTIVE.add(this);
        LivingEntity owner = this.getOwner();
        BladeSkill skill = this.getSkill();
        AnimationClip clip = skill.clip();
        int tick = (int) this.getPhaseTicks(0.0F);
        if (owner != null && tick <= clip.lengthTicks()) {
            CLIENT_BY_OWNER.put(owner.getId(), this);
            float facing = this.facing(owner, 1.0F);
            owner.yBodyRotO = owner.yBodyRot;
            owner.yBodyRot = facing;
            this.follow(owner, skill, clip, tick);
        } else if (owner != null && CLIENT_BY_OWNER.get(owner.getId()) == this) {
            CLIENT_BY_OWNER.remove(owner.getId());
        }
        this.waves.removeIf(wave -> {
            wave.age++;
            return wave.speed * (wave.age - WAVE_FADE) > wave.range;
        });
        this.marks.removeIf(mark -> tick - mark.bornAt > SlashMark.LIFE);
        this.ambience(owner, skill, clip, tick);
        this.starfall(tick);
        this.lights(owner);
    }

    private void follow(LivingEntity owner, BladeSkill skill, AnimationClip clip, int tick) {
        int perTick = Math.round(clip.framesPerTick());
        Vector3f local = new Vector3f();
        boolean mirror = this.isMirror();
        for (int step = 1; step <= perTick; step++) {
            int frame = (tick - 1) * perTick + step;
            if (frame < 0 || frame >= clip.frames) {
                continue;
            }
            float part = (float) step / perTick;
            Vec3 position = new Vec3(Mth.lerp(part, owner.xo, owner.getX()), Mth.lerp(part, owner.yo, owner.getY()),
                    Mth.lerp(part, owner.zo, owner.getZ()));
            float yaw = this.facing(owner, part);
            float time = (float) frame / perTick;
            for (int hand = 0; hand < 2; hand++) {
                if (hand == 1 && clip.leftSword == 0) {
                    continue;
                }
                Vec3 base = AbilityHelper.toWorld(clip.point(hand * 2 + 1, frame, local), position, yaw, mirror);
                Vec3 tip = AbilityHelper.toWorld(clip.point(hand * 2, frame, local), position, yaw, mirror);
                this.trails[hand].add(base, tip, time, heat(skill, clip, hand, frame));
            }
            for (AnimationClip.Marker marker : clip.markers()) {
                if (marker.frame() == frame) {
                    this.marked(owner, skill, clip, marker, position, yaw, time);
                }
            }
        }
    }

    private static float heat(BladeSkill skill, AnimationClip clip, int hand, int frame) {
        switch (skill) {
            case VORPAL_STRIKE -> {
                int lunge = clip.marker("lunge_start");
                if (frame < lunge) {
                    return 0.3F * Mth.clamp((frame - clip.marker("charge_start")) / 20.0F, 0.0F, 1.0F);
                }
                return Mth.clamp(1.0F - (frame - clip.marker("lunge_end")) / 12.0F, 0.0F, 1.0F);
            }
            case HORIZONTAL_SQUARE -> {
                return frame >= clip.marker("corner_1") && frame <= clip.marker("finish") + 4 ? 1.0F : 0.0F;
            }
            default -> {
                float best = frame >= clip.marker("launch") && frame <= clip.marker("recover_start") ? 0.3F : 0.0F;
                char letter = hand == 0 ? 'R' : 'L';
                for (AnimationClip.Marker marker : clip.markers()) {
                    if (BladeSkillShapes.isHit(marker) && marker.name().indexOf(letter, 7) >= 0) {
                        best = Math.max(best, 1.0F - Math.abs(frame - marker.frame()) / 8.0F);
                    }
                }
                return best;
            }
        }
    }

    private void marked(LivingEntity owner, BladeSkill skill, AnimationClip clip, AnimationClip.Marker marker, Vec3 position, float yaw,
                        float time) {
        String name = marker.name();
        boolean mirror = this.isMirror();
        switch (skill) {
            case VORPAL_STRIKE -> {
                if (name.equals("impact")) {
                    this.impactAt = time;
                }
            }
            case HORIZONTAL_SQUARE -> {
                Vec3 center = this.position();
                if (name.startsWith("corner_") || name.equals("finish")) {
                    this.corners.add(AbilityHelper.toWorld(clip.point(AnimationClip.RIGHT_TIP, marker.frame(), new Vector3f()),
                            center, this.getYaw(), mirror));
                    this.cornerTimes.add(time);
                    this.spray(this.corners.get(this.corners.size() - 1), ParticlesRegistry.BLADE_SPARK_FROST.get(), 14, 0.35);
                } else if (name.startsWith("slash_")) {
                    this.waves.add(BladeSkillShapes.squareSide(clip, name.charAt(6) - '0', center, this.getYaw(), mirror,
                            (float) Config.weapons.dark_repulser.waveRange));
                } else if (name.equals("release")) {
                    this.releasedAt = time;
                    for (Vec3 corner : this.corners) {
                        this.spray(corner, ParticlesRegistry.BLADE_SPARK_FROST.get(), 30, 0.7);
                    }
                }
            }
            case STARBURST_STREAM -> {
                if (!BladeSkillShapes.isHit(marker)) {
                    return;
                }
                StarburstSettings settings = Config.weapons.starburst_stream;
                Vec3 look = AbilityHelper.look(owner);
                this.lastHitAt = time;
                if (BladeSkillShapes.hitNumber(marker) == BladeSkillShapes.hitCount(clip)) {
                    this.finaleAt = time;
                    this.crossCenter = BladeSkillShapes.crossCenter(owner.getEyePosition(), look);
                    float reach = (float) settings.crossReach * 2.0F;
                    for (Vec3 bar : BladeSkillShapes.crossBars(look)) {
                        Vec3 across = look.cross(bar).normalize();
                        this.marks.add(SlashMark.streak(this.crossCenter, bar, across, reach, 1.5F, SlashMark.STAR, time, marker.frame()));
                    }
                    this.spray(this.crossCenter, ParticlesRegistry.BLADE_SPARK_STAR.get(), 260, 1.5);
                    this.spray(this.crossCenter, ParticlesRegistry.BLADE_SPARK_CRIMSON.get(), 90, 1.1);
                    return;
                }
                int window = BladeSkillShapes.window(clip, marker);
                for (int hand = 0; hand < 2; hand++) {
                    if (BladeSkillShapes.uses(marker, hand == 0 ? 'R' : 'L')) {
                        int blade = hand == 0 ? SlashMark.DARK : SlashMark.TEAL;
                        this.marks.add(SlashMark.swing(clip, hand * 2, marker.frame(), window, position, yaw, mirror, settings.slashScale,
                                blade, time));
                        this.shards(position, look, (float) settings.reach, blade, time, marker.frame() + hand);
                    }
                }
            }
        }
    }

    private void shards(Vec3 feet, Vec3 look, float reach, int blade, float time, int seed) {
        RandomSource random = this.random;
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 heart = feet.add(flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(reach * 0.4)).add(0, 1.3, 0);
        for (int i = 0; i < 5; i++) {
            Vec3 position = heart.add((random.nextDouble() - 0.5) * reach, (random.nextDouble() - 0.4) * 2.6, (random.nextDouble() - 0.5) * reach);
            Vec3 direction = new Vec3(random.nextDouble() - 0.5, (random.nextDouble() - 0.5) * 0.9, random.nextDouble() - 0.5).normalize();
            this.marks.add(SlashMark.streak(position, direction, SlashWave.perpendicular(direction), reach * (0.9F + random.nextFloat()),
                    0.16F + random.nextFloat() * 0.2F, blade, time + i * 0.5F, seed * 7 + i));
        }
        ParticleOptions own = blade == SlashMark.TEAL ? ParticlesRegistry.BLADE_SPARK_FROST.get() : ParticlesRegistry.BLADE_SPARK_STAR.get();
        for (int i = 0; i < 10; i++) {
            Vec3 position = heart.add((random.nextDouble() - 0.5) * reach * 0.8, (random.nextDouble() - 0.4) * 2.0,
                    (random.nextDouble() - 0.5) * reach * 0.8);
            Vec3 speed = position.subtract(feet.add(0, 1.2, 0)).normalize().scale(0.25 + random.nextDouble() * 0.6);
            this.level().addParticle(i % 3 == 0 ? ParticlesRegistry.BLADE_SPARK_CRIMSON.get() : own, position.x, position.y, position.z, speed.x, speed.y, speed.z);
        }
    }

    private void spray(Vec3 position, ParticleOptions particle, int count, double speed) {
        RandomSource random = this.random;
        for (int i = 0; i < count; i++) {
            double yaw = random.nextDouble() * Mth.TWO_PI;
            double pitch = (random.nextDouble() - 0.5) * Math.PI;
            double pace = speed * (0.3 + random.nextDouble() * 0.7);
            this.level().addParticle(particle, position.x, position.y, position.z, Math.cos(yaw) * Math.cos(pitch) * pace, Math.sin(pitch) * pace,
                    Math.sin(yaw) * Math.cos(pitch) * pace);
        }
    }

    private void ambience(LivingEntity owner, BladeSkill skill, AnimationClip clip, int tick) {
        if (owner == null || tick > clip.lengthTicks()) {
            return;
        }
        RandomSource random = this.random;
        Level world = this.level();
        int frame = Math.round(tick * clip.framesPerTick());
        switch (skill) {
            case VORPAL_STRIKE -> {
                if (frame >= clip.marker("charge_start") && frame < clip.marker("lunge_start") && this.trails[0].size() > 0) {
                    // sparks pulled in along the blade while it charges
                    BladeTrail trail = this.trails[0];
                    Vec3 base = trail.base(trail.size() - 1);
                    Vec3 tip = trail.tip(trail.size() - 1);
                    for (int i = 0; i < 5; i++) {
                        Vec3 to = base.lerp(tip, random.nextDouble());
                        Vec3 from = to.add((random.nextDouble() - 0.5) * 5.0, (random.nextDouble() - 0.5) * 4.0,
                                (random.nextDouble() - 0.5) * 5.0);
                        Vec3 pull = to.subtract(from).scale(0.16);
                        world.addParticle(ParticlesRegistry.BLADE_SPARK_CRIMSON.get(), from.x, from.y, from.z, pull.x, pull.y, pull.z);
                    }
                } else if (this.impactAt >= 0 && tick - this.impactAt < 10) {
                    // embers shed all along the lance
                    Vec3 origin = BladeSkillShapes.lanceOrigin(clip, this.position(), this.getYaw(), this.isMirror());
                    Vec3 aim = this.getAim();
                    double length = Config.weapons.elucidator.lanceLength;
                    double radius = Config.weapons.elucidator.lanceRadius;
                    for (int i = 0; i < 26; i++) {
                        Vec3 side = SlashWave.perpendicular(aim);
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        Vec3 out = side.scale(Math.cos(angle)).add(aim.cross(side).scale(Math.sin(angle)));
                        Vec3 position = origin.add(aim.scale(random.nextDouble() * length)).add(out.scale(radius * (0.4 + random.nextDouble() * 0.7)));
                        Vec3 speed = out.scale(0.08 + random.nextDouble() * 0.2).add(aim.scale(0.25));
                        world.addParticle(ParticlesRegistry.BLADE_SPARK_CRIMSON.get(), position.x, position.y, position.z, speed.x, speed.y, speed.z);
                    }
                }
            }
            case HORIZONTAL_SQUARE -> {
                // frost falling off the edges already drawn
                for (int i = 0; i + 1 < this.corners.size() && this.releasedAt < 0; i++) {
                    Vec3 position = this.corners.get(i).lerp(this.corners.get(i + 1), random.nextDouble());
                    world.addParticle(ParticlesRegistry.BLADE_SPARK_FROST.get(), position.x, position.y, position.z, (random.nextDouble() - 0.5) * 0.04,
                            -0.02, (random.nextDouble() - 0.5) * 0.04);
                }
            }
            case STARBURST_STREAM -> {
                boolean coil = frame >= clip.marker("coil") && this.finaleAt < 0;
                int count = frame < clip.marker("launch") || coil ? 7 : 3;
                Vec3 heart = owner.position().add(0, owner.getBbHeight() * 0.6, 0);
                for (int i = 0; i < count; i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    double reach = 1.2 + random.nextDouble() * (coil ? 6.0 : 3.2);
                    Vec3 from = owner.position().add(Math.cos(angle) * reach, random.nextDouble() * 2.6, Math.sin(angle) * reach);
                    Vec3 speed = frame < clip.marker("launch") || coil ? heart.subtract(from).scale(0.12)
                            : new Vec3(-Math.sin(angle) * 0.08, 0.08 + random.nextDouble() * 0.1, Math.cos(angle) * 0.08);
                    world.addParticle(ParticlesRegistry.BLADE_SPARK_STAR.get(), from.x, from.y, from.z, speed.x, speed.y, speed.z);
                }
            }
        }
    }

    private void starfall(int tick) {
        if (this.finaleAt < 0 || tick - this.finaleAt > 45) {
            return;
        }
        RandomSource random = this.random;
        double radius = Config.weapons.starburst_stream.burstRadius * 1.3;
        for (int i = 0; i < 9; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double reach = Math.sqrt(random.nextDouble()) * radius;
            this.level().addParticle(ParticlesRegistry.BLADE_SPARK_STAR.get(), this.crossCenter.x + Math.cos(angle) * reach,
                    this.crossCenter.y + 5.0 + random.nextDouble() * 8.0, this.crossCenter.z + Math.sin(angle) * reach,
                    (random.nextDouble() - 0.5) * 0.1, -0.5 - random.nextDouble() * 0.5, (random.nextDouble() - 0.5) * 0.1);
        }
    }

    private void lights(LivingEntity owner) {
        if (this.tickCount % 2 != 0) {
            return;
        }
        List<Vec3> spots = new ArrayList<>();
        if (owner != null && this.isRunning()) {
            spots.add(owner.position().add(0, 1.2, 0));
        }
        for (SlashWave wave : this.waves) {
            if (spots.size() < 10 && !wave.spent(wave.age)) {
                spots.add(wave.centerAt(wave.age));
            }
        }
        if (this.finaleAt >= 0 && this.getPhaseTicks(0.0F) - this.finaleAt < 30) {
            spots.add(this.crossCenter);
        }
        SimplyAnimeEffectLights.set(this, spots);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        CLIENT_BY_OWNER.values().removeIf(visual -> visual == this);
        CLIENT_ACTIVE.remove(this);
        SimplyAnimeEffectLights.clear(this);
    }
}
