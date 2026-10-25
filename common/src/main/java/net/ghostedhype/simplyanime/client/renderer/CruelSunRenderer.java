package net.ghostedhype.simplyanime.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ghostedhype.simplyanime.config.RhittaConfig;
import net.ghostedhype.simplyanime.entity.CruelSunEntity;
import net.ghostedhype.simplyanime.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

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

    // axis tilt, spin in radians per tick
    private static final float TILT = 0.42F;
    private static final float SPIN = 0.012F;

    public CruelSunRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CruelSunEntity entity) {
        return SunRenderTypes.GLOW;
    }

    @Override
    public boolean shouldRender(@NotNull CruelSunEntity entity, @NotNull Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(@NotNull CruelSunEntity entity, float yaw, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffers, int light) {
        if (Later.take(entity, poseStack, (pose, late) -> this.render(entity, yaw, partialTick, pose, late, light))) {
            return;
        }
        // shadow pass: only the lifted blocks cast shadows
        if (ShaderCompat.shadowPass()) {
            debrisOnly(entity, poseStack, buffers, partialTick);
            return;
        }
        Quaternionf rotation = this.entityRenderDispatcher.cameraOrientation();
        Vec3 right = VfxBuffer.rotate(rotation, 1, 0, 0);
        Vec3 upward = VfxBuffer.rotate(rotation, 0, 1, 0);
        Vec3 base = entity.getPosition(partialTick);
        Vec3 camera = this.entityRenderDispatcher.camera.getPosition();
        float time = entity.tickCount + partialTick;
        float sunLevel = entity.getSun();
        SunBatch batch = new SunBatch(right, upward, camera.subtract(base));

        switch (entity.getPhase()) {
            case CruelSunEntity.PHASE_CHARGE -> {
                LivingEntity owner = entity.getOwner();
                if (owner == null) {
                    return;
                }
                float ticks = entity.getPhaseTicks(partialTick);
                float orb = entity.getOrbRadius(partialTick);
                Vec3 center = CruelSunEntity.chargeCenter(owner, partialTick, orb).subtract(base);
                Vec3 feet = owner.getPosition(partialTick).subtract(base);
                boolean formed = ticks >= CruelSunEntity.IGNITE_TICKS;
                renderSunSpawningSequence(batch, feet, center, entity.getOrbBase(), time, ticks);
                if (formed) {
                    debris(entity, owner, base, poseStack, buffers, partialTick, time, ticks - CruelSunEntity.IGNITE_TICKS, 0.0F);
                    scorched(entity, owner, base, poseStack, buffers, partialTick, ticks - CruelSunEntity.IGNITE_TICKS, 1.0F);
                    renderGroundRings(batch, feet, orb, ticks - CruelSunEntity.IGNITE_TICKS, sunLevel);
                    // eruption flash
                    float since = ticks - CruelSunEntity.IGNITE_TICKS;
                    ignition(batch, feet, center, entity.getOrbBase(), since);
                    sun(batch, center, orb, time, sunLevel, detail(camera, base.add(center)), 1.0F);
                } else {
                    // small flickering light until it erupts
                    float gather = ticks / CruelSunEntity.IGNITE_TICKS;
                    float flick = 0.85F + 0.15F * Mth.sin(time * 1.3F);
                    float spark = entity.getOrbBase() * (0.35F + 0.75F * gather);
                    batch.drawHalo(center, spark * 2.8F * flick, FLAME, 0.7F);
                    batch.drawHalo(center, spark * 1.7F, GOLD, 0.9F);
                    batch.drawHalo(center, spark * 0.9F, WHITE, 1.0F);
                    renderGodRays(batch, center, spark * 0.4F, spark * (2.0F + 2.0F * gather), spark * 0.12F, 10, time * 3.0F, PALE, 0.7F);
                }
            }
            case CruelSunEntity.PHASE_FLIGHT -> {
                // rubble drops after the throw
                LivingEntity owner = entity.getOwner();
                float flight = entity.getPhaseTicks(partialTick);
                if (owner != null && flight < DEBRIS_FALL) {
                    debris(entity, owner, base, poseStack, buffers, partialTick, time, 1000.0F, flight / DEBRIS_FALL);
                }
                if (owner != null && flight < FIRE_FADE) {
                    scorched(entity, owner, base, poseStack, buffers, partialTick, 1000.0F, 1.0F - flight / FIRE_FADE);
                }
                float orb = entity.getOrbRadius(partialTick);
                Vec3 aimVec = entity.getAim();
                renderTrail(batch, aimVec, orb, time, base.subtract(camera));
                sun(batch, Vec3.ZERO, orb, time, sunLevel, detail(camera, base), 1.0F);
            }
            case CruelSunEntity.PHASE_BLAST -> renderBlastExplosion(batch, entity, partialTick, time, sunLevel, detail(camera, base));
            default -> {
            }
        }
        batch.flush(poseStack.last().pose(), buffers);
    }

    private static void debrisOnly(CruelSunEntity entity, PoseStack poseStack, MultiBufferSource buffers, float partialTick) {
        LivingEntity owner = entity.getOwner();
        if (owner == null) {
            return;
        }
        Vec3 base = entity.getPosition(partialTick);
        float time = entity.tickCount + partialTick;
        float ticks = entity.getPhaseTicks(partialTick);
        if (entity.getPhase() == CruelSunEntity.PHASE_CHARGE && ticks >= CruelSunEntity.IGNITE_TICKS) {
            debris(entity, owner, base, poseStack, buffers, partialTick, time, ticks - CruelSunEntity.IGNITE_TICKS, 0.0F);
        } else if (entity.getPhase() == CruelSunEntity.PHASE_FLIGHT && ticks < DEBRIS_FALL) {
            debris(entity, owner, base, poseStack, buffers, partialTick, time, 1000.0F, ticks / DEBRIS_FALL);
        }
    }

    private static final int DEBRIS = 26;
    private static final float DEBRIS_FALL = 14.0F;

    private static void debris(CruelSunEntity sun, LivingEntity owner, Vec3 base, PoseStack poseStack,
                               MultiBufferSource buffers, float partialTick, float time, float since, float fall) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        BlockRenderDispatcher blocks = client.getBlockRenderer();
        Vec3 feet = owner.getPosition(partialTick);
        float maxSpreadRadius = Math.max(3.0F, sun.getOrbBase() * 1.4F);
        for (int i = 0; i < DEBRIS; i++) {
            float horizontalRandomFactor = ((i * 37) % 23) / 22.0F;
            float verticalRandomFactor = ((i * 53) % 19) / 18.0F;
            float scaleRandomFactor = ((i * 71) % 17) / 16.0F;
            float spawnAngle = i * 2.399F + time * 0.004F;
            float centerOffsetDistance = 1.8F + horizontalRandomFactor * maxSpreadRadius;
            double x = feet.x + Mth.cos(spawnAngle) * centerOffsetDistance;
            double z = feet.z + Mth.sin(spawnAngle) * centerOffsetDistance;
            BlockPos.MutableBlockPos groundPos = BlockPos.containing(x, feet.y + 2, z).mutable();
            BlockState state = client.level.getBlockState(groundPos);
            for (int d = 0; d < 7 && (state.isAir() || !state.canOcclude()); d++) {
                groundPos.move(0, -1, 0);
                state = client.level.getBlockState(groundPos);
            }
            if (state.isAir() || !state.canOcclude()) {
                continue;
            }
            // staggered
            // Performs a smoothstep interpolation on the debris as it lifts
            float liftAnimationProgress = Mth.clamp((since - i * 0.6F) / 18.0F, 0.0F, 1.0F);
            liftAnimationProgress = liftAnimationProgress * liftAnimationProgress * (3.0F - 2.0F * liftAnimationProgress);

            // Combine lift with the vertical offset for the floating particles
            float renderYOffset = liftAnimationProgress * (1.2F + verticalRandomFactor * 3.8F) + liftAnimationProgress * 0.35F * Mth.sin(time * 0.09F + i * 1.3F);
            renderYOffset *= 1.0F - fall * fall;
            if (renderYOffset < 0.05F && fall > 0) {
                continue;
            }
            float size = (0.3F + scaleRandomFactor * 0.45F) * Math.max(0.35F, liftAnimationProgress);
            poseStack.pushPose();
            poseStack.translate(x - base.x, groundPos.getY() + 1.0 + renderYOffset - base.y, z - base.z);
            poseStack.mulPose(Axis.YP.rotation(time * 0.02F * (0.5F + horizontalRandomFactor) + i));
            poseStack.mulPose(Axis.XP.rotation(time * 0.015F * (0.5F + verticalRandomFactor) + i * 0.7F));
            poseStack.scale(size, size, size);
            poseStack.translate(-0.5, -0.5, -0.5);
            blocks.renderSingleBlock(state, poseStack, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    private static final int FIRE_SPOTS = 40;
    private static final float FIRE_FADE = 40.0F;

    private static void scorched(CruelSunEntity sun, LivingEntity owner, Vec3 base, PoseStack poseStack,
                                 MultiBufferSource buffers, float partialTick, float since, float keep) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !RhittaConfig.get().cruelSun.scorchSurroundings) {
            return;
        }
        BlockRenderDispatcher blocks = client.getBlockRenderer();
        BlockState fire = Blocks.FIRE.defaultBlockState();
        Vec3 feet = owner.getPosition(partialTick);
        float reach = Math.max(3.0F, sun.getOrbBase() * 1.8F);
        int shown = (int) (FIRE_SPOTS * keep);
        for (int i = 0; i < shown; i++) {
            if (since < i * 0.8F) {
                break;
            }
            float horizontalRandomFactor = ((i * 41) % 29) / 28.0F;
            float spiralAngle = i * 2.399F + 0.7F;
            float centerOffsetDistance = 1.5F + horizontalRandomFactor * reach;
            BlockPos.MutableBlockPos groundPos = BlockPos.containing(feet.x + Mth.cos(spiralAngle) * centerOffsetDistance, feet.y + 3,
                    feet.z + Mth.sin(spiralAngle) * centerOffsetDistance).mutable();
            BlockState state = client.level.getBlockState(groundPos);
            for (int step = 0; step < 7 && state.isAir(); step++) {
                groundPos.move(0, -1, 0);
                state = client.level.getBlockState(groundPos);
            }
            if (state.isAir() || !Services.PLATFORM.isFlammable(state, client.level, groundPos, Direction.UP)) {
                continue;
            }
            // plants burn in place, leaves and wood on top
            if (!state.canBeReplaced()) {
                groundPos.move(0, 1, 0);
                if (!client.level.getBlockState(groundPos).isAir()) {
                    continue;
                }
            }
            poseStack.pushPose();
            poseStack.translate(groundPos.getX() - base.x, groundPos.getY() - base.y, groundPos.getZ() - base.z);
            blocks.renderSingleBlock(fire, poseStack, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    // fewer flames when it is far away
    private static float detail(Vec3 cameraVec, Vec3 positionVec) {
        double cameraDistance = cameraVec.distanceTo(positionVec);
        return cameraDistance < 64 ? 1.0F : cameraDistance < 160 ? 0.6F : 0.35F;
    }

    private static void sun(SunBatch sunBatch, Vec3 center, float baseRadius, float time, float sunLevel, float detail, float baseAlpha) {
        float pulseMultiplier = 1.0F + 0.035F * Mth.sin(time * 0.5F) + 0.02F * Mth.sin(time * 1.7F);
        float glowBrightness = 0.8F + 0.2F * sunLevel;
        float currentSunRadius = baseRadius * pulseMultiplier;

        float rotationAngle = time * SPIN;
        float slide = -rotationAngle / Mth.PI;

        // It is easier to make changes with declared variables than it is to hunt them down in your methods. Even when
        // the method is only one line, it is good practice that, once it becomes habit, can make your life a lot easier.
        // Remember: the compiler doesn't care how you organize your variables. It is going to plug them in where they
        // belong anyway. This is mostly when you have an extremely long method. Having all of the variables close at
        // hand makes it easier to make changes, debug and read the code more easily.

        // It is also worth noting that by declaring your variables like this, it will help you notice where you have
        // duplicate code. Now, granted, leaving everything inline can help the IDE recognize duplicate code, and it can
        // make suggestions; but, it is also just as easy for it to miss something because the individual values are
        // different. This is why it is essential to audit your own code.

        float sunHaloRadius = baseRadius * 2.5F * pulseMultiplier;
        float sunHaloAlpha = 0.5F * baseAlpha;
        float sunGlowRadius = baseRadius * 4.2F * pulseMultiplier;
        float sunGlowAlpha = 0.15F * baseAlpha * glowBrightness;

        sunBatch.drawHalo(center, sunHaloRadius, FLAME, sunHaloAlpha);
        sunBatch.drawGlow(center, sunGlowRadius, GOLD, sunGlowAlpha);

        float rayInnerRadius = baseRadius * 0.9F;
        float rayOuterRadius = baseRadius * 3.6F;
        float rayThickness = baseRadius * 0.12F;
        int rayCount = 10;
        float rayAlpha = 0.3F * baseAlpha * glowBrightness;
        renderGodRays(sunBatch, center, rayInnerRadius, rayOuterRadius, rayThickness, rayCount, time, GOLD, rayAlpha);

        // Plasma A Variables
        float bodyRadiusA = currentSunRadius * 1.005F;
        float coreAlphaA = 0.45F * baseAlpha;
        float rimAlphaA = 0.6F * baseAlpha;

        // Plasma B Variables
        float coreAlphaB = 0.3F * baseAlpha;
        float bodyRadiusB = currentSunRadius * 1.02F;
        float tiltB = -TILT * 1.7F;
        float slideB = -slide * 0.6F;
        float rimAlphaB = 0.2F * baseAlpha;

        // Haze Variables
        float hazeRadius = currentSunRadius * 1.08F;
        float hazeRimAlpha = 0.7F * baseAlpha;

        // Glow Variables
        float glowRadius = currentSunRadius * 0.9F;
        float glowAlpha = 0.25F * baseAlpha;

        sunBatch.drawBall(true,  SunRenderTypes.PLASMA_A, center, currentSunRadius, TILT, slide, BODY, LIMB, baseAlpha, baseAlpha);
        sunBatch.drawBall(false, SunRenderTypes.PLASMA_A, center, bodyRadiusA, TILT, slide, ORANGE, FLAME, coreAlphaA, rimAlphaA);
        sunBatch.drawBall(false, SunRenderTypes.PLASMA_B, center, bodyRadiusB, tiltB, slideB, GOLD, GOLD, coreAlphaB, rimAlphaB);
        // haze on the limb
        sunBatch.drawBall(true, SimplyAnimeRenderTypes.WHITE, center, hazeRadius, 0.0F, 0.0F, ORANGE, FLAME, 0.0F, hazeRimAlpha);
        // core
        sunBatch.drawGlow(center, glowRadius, WHITE, glowAlpha);

        float heatEdgeRadius = baseRadius * CruelSunEntity.HEAT_EDGE;
        int maxFlameCount = Math.max(40, (int) (110 * detail));
        for (int flameCount = 0; flameCount < maxFlameCount; flameCount++) {
            Vec3 normalDirection = calculateSphereSurfacePoint(flameCount, maxFlameCount, rotationAngle, TILT);
            float cameraFacingFactor = sunBatch.facing(center, normalDirection, currentSunRadius);
            float cameraFacingMin = -0.35F;
            float cameraFacingMax = 0.6F;

            if (cameraFacingFactor < cameraFacingMin || cameraFacingFactor > cameraFacingMax) {
                continue;
            }

            float horizontalRandomFactor = ((flameCount * 37) % 23) / 22.0F;
            float verticalRandomFactor = ((flameCount * 53) % 19) / 18.0F;

            float flickerIntensity = 0.5F + 0.5F * Mth.sin(time * (0.25F + 0.2F * horizontalRandomFactor) + flameCount * 2.39F);
            float reachPercentage = flameCount % 3 == 0
                    ? 0.45F + 0.5F * flickerIntensity * (0.6F + 0.4F * horizontalRandomFactor)
                    : 0.2F + 0.35F * flickerIntensity * (0.5F + verticalRandomFactor);

            float edgeVisibilityAlpha = baseAlpha * Mth.clamp(1.0F - cameraFacingFactor, 0.0F, 1.0F);

            float tongueStartRadius = currentSunRadius * 0.88F;
            float flameTongueLength = Math.min(currentSunRadius * reachPercentage, heatEdgeRadius - tongueStartRadius);
            float flameTongueWidth = currentSunRadius * (0.2F + 0.14F * verticalRandomFactor);
            float flameTongueAlpha = 0.9F * edgeVisibilityAlpha;

            sunBatch.drawTongue(center, normalDirection, tongueStartRadius, flameTongueLength, flameTongueWidth, flameCount, FLAME, flameTongueAlpha);

            float goldTongueLength = flameTongueLength * 0.6F;
            float goldTongueWidth = flameTongueWidth * 0.6F;
            int goldTongueCount = flameCount + 1;
            float goldTongueAlpha = 0.8F * edgeVisibilityAlpha;

            sunBatch.drawTongue(center, normalDirection, tongueStartRadius, goldTongueLength, goldTongueWidth, goldTongueCount, GOLD, goldTongueAlpha);
        }

        // loops
        int maxLoopCount = detail > 0.5F ? 8 : 4;
        for (int k = 0; k < maxLoopCount; k++) {
            Vec3 normalDirection = calculateSphereSurfacePoint(k * 7 + 3, 61, rotationAngle, TILT);
            if (sunBatch.facing(center, normalDirection, currentSunRadius) < -0.3F) {
                continue;
            }

            float horizontalRandomFactor = ((k * 37) % 23) / 22.0F;
            float loopLifecycle = Mth.sin(Mth.PI * Mth.frac(time / 110.0F + k * 0.37F));

            Vec3 tangentAcross = normalDirection.cross(new Vec3(0.3, 1.0, 0.2 + k)).normalize();
            float loopArcSpan = 0.55F + 0.25F * horizontalRandomFactor;
            float loopMaxHeight = (0.18F + 0.3F * horizontalRandomFactor) * loopLifecycle;

            Vec3[] ribbonPath = new Vec3[11];
            for (int j = 0; j < ribbonPath.length; j++) {
                float pathProgress = j / (float) (ribbonPath.length - 1);
                float currentArcAngle = (pathProgress - 0.5F) * loopArcSpan;
                Vec3 arcDirection = normalDirection.scale(Mth.cos(currentArcAngle)).add(tangentAcross.scale(Mth.sin(currentArcAngle)));
                float ribbonScaleFactor = currentSunRadius * (0.97F + loopMaxHeight * Mth.sin(Mth.PI * pathProgress));
                ribbonPath[j] = center.add(arcDirection.scale(ribbonScaleFactor));
            }

            float ribbonWidth0 = currentSunRadius * 0.09F;
            float ribbonAlpha0 = 0.8F * loopLifecycle * baseAlpha;

            float ribbonWidth1 = currentSunRadius * 0.035F;
            float ribbonAlpha1 = 0.7F * loopLifecycle * baseAlpha;

            sunBatch.drawRibbon(SunRenderTypes.RAY, ribbonPath, ribbonWidth0, ribbonWidth0, FLAME, ribbonAlpha0, ribbonAlpha0, 0.0F);
            sunBatch.drawRibbon(SunRenderTypes.RAY, ribbonPath, ribbonWidth1, ribbonWidth1, GOLD,  ribbonAlpha1, ribbonAlpha1, 0.0F);
        }

        // flares
        for (int flareCount = 0; flareCount < 3; flareCount++) {
            float flareInternalClock = time / 70.0F + flareCount * 0.41F;
            float flareLifecycle = Mth.frac(flareInternalClock);

            float flareFadeAlpha = 1.0F - Mth.clamp((flareLifecycle - 0.12F) / 0.5F, 0.0F, 1.0F);
            if (flareFadeAlpha <= 0.0F) {
                continue;
            }

            int flareBurst = Mth.floor(flareInternalClock);
            int flarePointCount = 97;
            int flareBurstIndex = Math.floorMod(flareBurst * 13 + flareCount * 7, flarePointCount);
            Vec3 normalDirection = calculateSphereSurfacePoint(flareBurstIndex, flarePointCount, rotationAngle, TILT);

            float cameraFacingFactor = sunBatch.facing(center, normalDirection, currentSunRadius);
            float cameraFacingMin = -0.3F;
            float cameraFacingMax = 0.75F;
            if (cameraFacingFactor < cameraFacingMin || cameraFacingFactor > cameraFacingMax) {
                continue;
            }

            float burstGrowthScale = Math.min(1.0F, flareLifecycle / 0.12F);

            float flareStartRadius = currentSunRadius * 0.9F;
            float flareLength = Math.min(currentSunRadius * 1.05F * burstGrowthScale, heatEdgeRadius - flareStartRadius);
            float flareWidth = currentSunRadius * 0.3F;
            int flareCountVariant = flareBurst + flareCount;
            float flareAlpha = flareFadeAlpha * baseAlpha;

            float flareLength1 = flareLength * 0.7F;
            float flareWidth1 = currentSunRadius * 0.18F;
            int flareCountVariant1 = flareCountVariant + 2;
            float flareAlpha1 = flareAlpha * 0.8F;

            sunBatch.drawTongue(center, normalDirection, flareStartRadius, flareLength,  flareWidth,  flareCountVariant,  ORANGE, flareAlpha);
            sunBatch.drawTongue(center, normalDirection, flareStartRadius, flareLength1, flareWidth1, flareCountVariant1, PALE,   flareAlpha1);
        }
    }

    private static Vec3 calculateSphereSurfacePoint(int index, int count, float rotationAngle, float tilt) {
        float verticalPosition = 1.0F - 2.0F * (index + 0.5F) / count;
        float horizontalRingRadius = Mth.sqrt(Math.max(0.0F, 1.0F - verticalPosition * verticalPosition));
        float spiralAngle = index * 2.39996F + rotationAngle;

        float untiltedX = horizontalRingRadius * Mth.cos(spiralAngle);
        float untiltedZ = horizontalRingRadius * Mth.sin(spiralAngle);

        float cosTilt = Mth.cos(tilt);
        float sinTilt = Mth.sin(tilt);

        float tiltedY = verticalPosition * cosTilt - untiltedZ * sinTilt;
        float tiltedZ = verticalPosition * sinTilt + untiltedZ * cosTilt;

        return new Vec3(untiltedX, tiltedY, tiltedZ);
    }

    private static void renderSunSpawningSequence(SunBatch sunBatch, Vec3 ownerFeetVec, Vec3 sunCenterVec, float baseRadius, float time, float ticks) {

        float fadeInAlpha = Math.min(1.0F, ticks / 6.0F);
        float fadeOutAlpha = 1.0F - Mth.clamp((ticks - CruelSunEntity.IGNITE_TICKS - 6.0F) / 24.0F, 0.0F, 1.0F);
        float structuralAlpha = fadeInAlpha * fadeOutAlpha;

        if (structuralAlpha <= 0.01F) {
            return;
        }

        float ignitionProgress = Math.min(1.0F, ticks / CruelSunEntity.IGNITE_TICKS);
        float effectRadius = Math.max(4.0F, baseRadius * 1.2F);

        Vec3 groundVec = new Vec3(sunCenterVec.x, ownerFeetVec.y + 0.08, sunCenterVec.z);

        // ground rings
        float glowGoldRadius = effectRadius * 1.2F;
        float glowGoldAlpha = 0.55F * structuralAlpha;
        sunBatch.drawFlatGlow(groundVec, glowGoldRadius, GOLD, glowGoldAlpha);

        float glowWhiteRadius = effectRadius * 0.5F;
        float glowWhiteAlpha = 0.5F * structuralAlpha * ignitionProgress;
        sunBatch.drawFlatGlow(groundVec, glowWhiteRadius, WHITE, glowWhiteAlpha);

        for (int rings = 0; rings < 4; rings++) {
            float ringCycleProgress = Mth.frac(time * (0.05F + 0.04F * ignitionProgress) + rings * 0.25F);
            Vec3 staggeredGroundVec = groundVec.add(0, 0.02 * rings, 0);

            float collapseRadius = effectRadius * (1.0F - ringCycleProgress * ringCycleProgress);
            int[] ringColor = rings % 2 == 0 ? ORANGE : GOLD;
            float ringAlpha = 0.9F * structuralAlpha * Mth.sin(Mth.PI * ringCycleProgress);

            sunBatch.drawFlatRing(staggeredGroundVec, collapseRadius, ringColor, ringAlpha);
        }

        // sky shaft
        Vec3[] skyShaftPath = {sunCenterVec, sunCenterVec.add(0, baseRadius * 4.5F, 0)};

        float skyShaftGoldStartHalfWidth = baseRadius * (0.25F + 0.45F * ignitionProgress);
        float skyShaftGoldEndHalfWidth = baseRadius * 0.9F;
        float skyShaftGoldStartAlpha = structuralAlpha * 0.8F;
        sunBatch.drawRibbon(SunRenderTypes.RAY, skyShaftPath, skyShaftGoldStartHalfWidth, skyShaftGoldEndHalfWidth, GOLD, skyShaftGoldStartAlpha, 0.0F, 0.0F);

        float skyShaftWhiteStartHalfWidth = baseRadius * (0.08F + 0.16F * ignitionProgress);
        float skyShaftWhiteEndHalfWidth = baseRadius * 0.25F;
        float skyShaftWhiteStartAlpha = structuralAlpha * 0.9F;
        sunBatch.drawRibbon(SunRenderTypes.RAY, skyShaftPath, skyShaftWhiteStartHalfWidth, skyShaftWhiteEndHalfWidth, WHITE, skyShaftWhiteStartAlpha, 0.0F, 0.0F);

        // ground column
        Vec3[] columnPath = {groundVec, sunCenterVec};

        float columnGoldStartHalfWidth = baseRadius * 0.3F;
        float columnGoldEndHalfWidth = baseRadius * 0.15F;
        float columnGoldStartAlpha = structuralAlpha * 0.5F;
        float columnGoldEndAlpha = structuralAlpha * 0.8F;
        sunBatch.drawRibbon(SunRenderTypes.RAY, columnPath, columnGoldStartHalfWidth, columnGoldEndHalfWidth, GOLD, columnGoldStartAlpha, columnGoldEndAlpha, 0.0F);

        float columnWhiteStartHalfWidth = baseRadius * 0.1F;
        float columnWhiteEndHalfWidth = baseRadius * 0.06F;
        float columnWhiteStartAlpha = structuralAlpha * 0.7F;
        float columnWhiteEndAlpha = structuralAlpha * 0.9F;
        sunBatch.drawRibbon(SunRenderTypes.RAY, columnPath, columnWhiteStartHalfWidth, columnWhiteEndHalfWidth, WHITE, columnWhiteStartAlpha, columnWhiteEndAlpha, 0.0F);

        // streams
        int maxStreamCount = 6;
        for (int streamCount = 0; streamCount < maxStreamCount; streamCount++) {
            Vec3[] streamSplinePath = new Vec3[26];

            for (int streamSplinePathPoint = 0; streamSplinePathPoint < streamSplinePath.length; streamSplinePathPoint++) {
                float pathInterpolationProgress = streamSplinePathPoint / (float) (streamSplinePath.length - 1);

                float spiralAngle = streamCount * Mth.TWO_PI / 6.0F + pathInterpolationProgress * 6.5F - time * 0.3F;
                float spiralRadius = effectRadius * (1.0F - pathInterpolationProgress);

                double linearHeight = Mth.lerp(pathInterpolationProgress, groundVec.y, sunCenterVec.y);
                double archOffset = Mth.sin(Mth.PI * pathInterpolationProgress) * baseRadius * 1.2F;
                double currentY = linearHeight + archOffset;

                streamSplinePath[streamSplinePathPoint] = new Vec3(sunCenterVec.x + Mth.cos(spiralAngle) * spiralRadius, currentY, sunCenterVec.z + Mth.sin(spiralAngle) * spiralRadius);
            }

            float baseRibbonThickness = baseRadius * 0.22F;

            float ribbonFlameEndHalfWidth = baseRibbonThickness * 0.3F;
            float ribbonFlameStartAlpha = structuralAlpha * 0.7F;
            sunBatch.drawRibbon(SunRenderTypes.RAY, streamSplinePath, baseRibbonThickness, ribbonFlameEndHalfWidth, FLAME, ribbonFlameStartAlpha, structuralAlpha, 0.0F);

            float ribbonGoldStartHalfWidth = baseRibbonThickness * 0.5F;
            float ribbonGoldEndHalfWidth = baseRibbonThickness * 0.15F;
            float ribbonGoldStartAlpha = structuralAlpha * 0.8F;
            sunBatch.drawRibbon(SunRenderTypes.RAY, streamSplinePath, ribbonGoldStartHalfWidth, ribbonGoldEndHalfWidth, GOLD, ribbonGoldStartAlpha, structuralAlpha, 0.0F);

            float ribbonWhiteStartHalfWidth = baseRibbonThickness * 0.18F;
            float ribbonWhiteEndHalfWidth = baseRibbonThickness * 0.06F;
            float ribbonWhiteStartAlpha = structuralAlpha * 0.6F;
            sunBatch.drawRibbon(SunRenderTypes.RAY, streamSplinePath, ribbonWhiteStartHalfWidth, ribbonWhiteEndHalfWidth, WHITE, ribbonWhiteStartAlpha, structuralAlpha, 0.0F);
        }
    }

    // ignition flash
    private static void ignition(SunBatch sunBatch, Vec3 ignitedEntityFeetVec, Vec3 ignitionCenterVec, float baseRadius, float ticks) {
        if (ticks >= 16.0F) {
            return;
        }
        float ignitionProgress = ticks / 16.0F;

        float visibilityFadeFactor = (1.0F - ignitionProgress) * (1.0F - ignitionProgress);

        float haloFlameRadius = baseRadius * (2.5F + 4.0F * ignitionProgress);
        float haloFlameAlpha = visibilityFadeFactor * 0.5F;
        sunBatch.drawHalo(ignitionCenterVec, haloFlameRadius, FLAME, haloFlameAlpha);

        float haloGoldRadius = baseRadius * (1.6F + 3.0F * ignitionProgress);
        float haloGoldAlpha = visibilityFadeFactor * 0.8F;
        sunBatch.drawHalo(ignitionCenterVec, haloGoldRadius, GOLD, haloGoldAlpha);

        float glowWhiteRadius = baseRadius * (1.2F + 2.0F * ignitionProgress);
        sunBatch.drawGlow(ignitionCenterVec, glowWhiteRadius, WHITE, visibilityFadeFactor);

        Vec3 groundVec = new Vec3(ignitionCenterVec.x, ignitedEntityFeetVec.y + 0.1, ignitionCenterVec.z);
        float verticalShaftOffsetY = baseRadius * 5.0F;
        Vec3[] verticalShaftPath = {groundVec, ignitionCenterVec.add(0, verticalShaftOffsetY, 0)};

        float ribbonWhiteStartHalfWidth = baseRadius * 0.7F * (1.0F - ignitionProgress);
        float ribbonWhiteEndHalfWidth = baseRadius * 0.4F * (1.0F - ignitionProgress);
        sunBatch.drawRibbon(SunRenderTypes.RAY, verticalShaftPath, ribbonWhiteStartHalfWidth, ribbonWhiteEndHalfWidth, WHITE, visibilityFadeFactor, 0.0F, 0.0F);

        // shell
        float shellGoldRadius = baseRadius * (0.3F + 2.6F * ignitionProgress);
        float shellGoldRimAlpha = visibilityFadeFactor * 0.8F;
        sunBatch.drawBall(true, SimplyAnimeRenderTypes.WHITE, ignitionCenterVec, shellGoldRadius, 0.0F, 0.0F, GOLD, GOLD, 0.0F, shellGoldRimAlpha);

        float flatRingPaleRadius = baseRadius * (0.5F + 4.0F * ignitionProgress);
        sunBatch.drawFlatRing(groundVec, flatRingPaleRadius, PALE, visibilityFadeFactor);

        Vec3 flatRingOrangeGroundVecOffset = groundVec.add(0, 0.03, 0);
        float flatRingOrangeRadius = baseRadius * (0.3F + 3.0F * ignitionProgress);
        float flatRingOrangeAlpha = visibilityFadeFactor * 0.8F;
        sunBatch.drawFlatRing(flatRingOrangeGroundVecOffset, flatRingOrangeRadius, ORANGE, flatRingOrangeAlpha);
    }

    // god rays
    private static void renderGodRays(SunBatch sunBatch, Vec3 sunCenterVec, float innerRadius, float maxReachDistance,
                                      float rayHalfWidth, int rayCount, float time, int[] rayColor, float baseAlpha) {
        for (int rays = 0; rays < rayCount; rays++) {
            // Distributes rays evenly, same as original
            float rayAngle = rays * Mth.TWO_PI / rayCount + time * 0.004F + (rays % 3) * 0.13F;

            // Calculates a sine wave scaling factor to pulse the ray's length over time
            float lengthPulsingFactor = 0.5F + 0.5F * Mth.sin(time * 0.21F + rays * 3.1F);
            float currentRayLength = maxReachDistance * (0.55F + 0.45F * lengthPulsingFactor);

            // Prevents the rays from inverting
            float clampedReachDistance = Math.max(innerRadius + 0.1F, currentRayLength);

            // Calculates secondary sine wave to offset the individual ray alphas over time. This gives a shimmering effect
            float shimmerFadeFactor = 0.6F + 0.4F * Mth.sin(time * 0.33F + rays * 1.3F);
            float dynamicRayAlpha = baseAlpha * shimmerFadeFactor;

            sunBatch.drawRay(sunCenterVec, rayAngle, innerRadius, clampedReachDistance, rayHalfWidth, rayColor, dynamicRayAlpha);
        }
    }

    // ground light
    private static void renderGroundRings(SunBatch sunBatch, Vec3 ownerFeetVec, float sunOrbBaseRadius, float timeSinceSpawn, float sunLevel) {
        Vec3 groundAnchorVec = ownerFeetVec.add(0, 0.05, 0);
        float spawnGrowthProgress = Math.min(1.0F, timeSinceSpawn / 20.0F);

        float flatGlowGoldRadius = sunOrbBaseRadius * 2.2F * spawnGrowthProgress;
        float flatGlowGoldAlpha = 0.35F + 0.1F * sunLevel;
        sunBatch.drawFlatGlow(groundAnchorVec, flatGlowGoldRadius, GOLD, flatGlowGoldAlpha);

        int maxRingCount = 3;
        for (int rings = 0; rings < maxRingCount; rings++) {
            float ringCycleProgress = ((timeSinceSpawn + rings * 13.0F) % 40.0F) / 40.0F;
            Vec3 layeredGroundPos = groundAnchorVec.add(0.0, 0.02 * rings, 0.0);
            float expandedRingRadius = sunOrbBaseRadius * (0.4F + 1.8F * ringCycleProgress) * spawnGrowthProgress;
            float ringAlpha = 0.75F * (1.0F - ringCycleProgress);
            sunBatch.drawFlatRing(layeredGroundPos, expandedRingRadius, ORANGE, ringAlpha);
        }
    }

    // trail, same taper as CruelSunEntity.TRAIL_*
    private static void renderTrail(SunBatch sunBatch, Vec3 directionVec, float baseRadius, float time, Vec3 cameraToEntityVec) {
        if (directionVec.lengthSqr() < 1.0E-4) {
            return;
        }
        directionVec = directionVec.normalize();

        Vec3 perpendicularTrailWidthVec = directionVec.cross(cameraToEntityVec);
        perpendicularTrailWidthVec = perpendicularTrailWidthVec.lengthSqr() < 1.0E-6
                ? sunBatch.right
                : perpendicularTrailWidthVec.normalize();

        Vec3 backwardTrailOffsetVec = directionVec.scale(-baseRadius * CruelSunEntity.TRAIL_LENGTH);
        float trailMaxWidth = baseRadius * CruelSunEntity.TRAIL_WIDTH;
        float trailTipScalingFactor = baseRadius * CruelSunEntity.TRAIL_TIP;

        float textureScrollOffset = time * 0.08F;

        sunBatch.drawStreak(SunRenderTypes.FLAMES, Vec3.ZERO, backwardTrailOffsetVec, perpendicularTrailWidthVec, trailMaxWidth, trailTipScalingFactor, ORANGE, 0.95F, textureScrollOffset);

        Vec3 streakGoldOffsetVec = backwardTrailOffsetVec.scale(0.75);
        float streakGoldWidth = trailMaxWidth * 0.6F;
        float textureScrollStreakGoldOffset = textureScrollOffset + 0.37F;
        sunBatch.drawStreak(SunRenderTypes.FLAMES, Vec3.ZERO, streakGoldOffsetVec, perpendicularTrailWidthVec, streakGoldWidth, trailTipScalingFactor, GOLD, 0.9F, textureScrollStreakGoldOffset);

        Vec3 streakWhiteOffsetVec = backwardTrailOffsetVec.scale(0.9);
        float streakWhiteWidth = trailMaxWidth * 0.35F;
        float streakWhiteTipScale = trailTipScalingFactor * 0.5F;
        sunBatch.drawStreak(SunRenderTypes.RAY, Vec3.ZERO, streakWhiteOffsetVec, perpendicularTrailWidthVec, streakWhiteWidth, streakWhiteTipScale, WHITE, 0.8F, 0.0F);
    }

    // SunLevel is never used, how does this factor in to the method that calls it? Used at net/ghostedhype/simplyanime/client/renderer/CruelSunRenderer.java:123
    private static void renderBlastExplosion(SunBatch sunBatch, CruelSunEntity cruelSunEntity, float partialTick, float time, float sunLevel, float detail) {
        float explosionSequenceTicks = cruelSunEntity.getPhaseTicks(partialTick);
        float explosionBlastRadius = cruelSunEntity.getBlastRadius();

        float explosionBlastFrontProgress = CruelSunEntity.blastFront(explosionBlastRadius, explosionSequenceTicks);
        float explosionBlastFadeProgress = CruelSunEntity.blastFade(explosionSequenceTicks);

        float explosionShockwaveRadius = explosionBlastFrontProgress * (1.0F + CruelSunEntity.FADE_SWELL * explosionBlastFadeProgress);

        float visibilityFadeFactor = (float) Math.pow(1.0F - explosionBlastFadeProgress, 1.3);

        if (explosionShockwaveRadius < 0.05F) {
            return;
        }

        // white -> orange -> red
        int[] shellColorGradient = SunBatch.mix(ORANGE, RED, explosionBlastFadeProgress);
        int[] coreColorGradient = SunBatch.mix(WHITE, GOLD, Math.min(1.0F, explosionBlastFadeProgress * 1.5F));

        float rotationAngle = time * 0.02F;
        float textureSlide = -rotationAngle / Mth.PI;

        // flash
        float maxExplosionDurationInTicks = 8.0F;
        if (explosionSequenceTicks < maxExplosionDurationInTicks) {
            float maxExplosionBlastRadius = explosionBlastRadius * 2.4F;
            float maxAlpha = 1.0F - explosionSequenceTicks / 8.0F;
            sunBatch.drawGlow(Vec3.ZERO, maxExplosionBlastRadius, WHITE, maxAlpha);
        }

        // ground glow outlasts the fire
        Vec3 groundVecFlameOffset = new Vec3(0, 0.06, 0);
        float flatGlowFlameRadius = explosionBlastRadius * 1.05F;
        float flatGlowFlameAlpha = 0.7F * (1.0F - explosionBlastFadeProgress * explosionBlastFadeProgress);
        sunBatch.drawFlatGlow(groundVecFlameOffset, flatGlowFlameRadius, FLAME, flatGlowFlameAlpha);

        Vec3 groundVecGoldOffset = new Vec3(0, 0.08, 0);
        float flatGlowGoldRadius = explosionShockwaveRadius * 0.5F;
        float flatGlowGoldAlpha = 0.5F * (1.0F - explosionBlastFadeProgress);
        sunBatch.drawFlatGlow(groundVecGoldOffset, flatGlowGoldRadius, GOLD, flatGlowGoldAlpha);

        // solid at first then thins out
        float solidnessFactor = explosionBlastFadeProgress < 0.25F
                ? 1.0F
                : (float) Math.pow(Math.max(0.0F, 1.0F - (explosionBlastFadeProgress - 0.25F) / 0.4F), 1.5);

        if (visibilityFadeFactor > 0.01F) {
            float shockwaveVisibilityFadeRadius = explosionShockwaveRadius * 1.8F;
            float shockwaveVisibilityFadeAlpha = visibilityFadeFactor * 0.25F;
            sunBatch.drawGlow(Vec3.ZERO, shockwaveVisibilityFadeRadius, GOLD, shockwaveVisibilityFadeAlpha);

            float shockwaveShellVisibilityFadeRadius = explosionShockwaveRadius * 1.15F;
            float shockwaveShellVisibilityFadeAlpha = visibilityFadeFactor * 0.3F;
            sunBatch.drawGlow(Vec3.ZERO, shockwaveShellVisibilityFadeRadius, shellColorGradient, shockwaveShellVisibilityFadeAlpha);

            float godRaysInnerRadius = explosionShockwaveRadius * 0.3F;
            float godRaysHalfWidth = explosionShockwaveRadius * 0.08F;
            float godRaysAlpha = 0.6F * visibilityFadeFactor * (1.0F - explosionBlastFadeProgress);
            renderGodRays(sunBatch, Vec3.ZERO, godRaysInnerRadius, explosionShockwaveRadius, godRaysHalfWidth, 18, time, PALE, godRaysAlpha);

            // dome, the skin sits on the damage front
            float domeThreshold = 0.5F;
            if (solidnessFactor > domeThreshold) {
                float domeRadius = explosionShockwaveRadius * 0.95F;
                sunBatch.drawDepthBall(Vec3.ZERO, domeRadius);
            }

            float domeMidShellRadius = explosionShockwaveRadius * 0.97F;
            int[] domeMidShellColorGradientA = SunBatch.mix(PALE, ORANGE, explosionBlastFadeProgress);
            int[] domeMidShellColorGradientB = SunBatch.mix(ORANGE, FLAME, explosionBlastFadeProgress);
            sunBatch.drawBall(true, SunRenderTypes.PLASMA_A, Vec3.ZERO, domeMidShellRadius, 0.3F, textureSlide, domeMidShellColorGradientA, domeMidShellColorGradientB, solidnessFactor, solidnessFactor);

            float domeMidShellHighlightRadius = explosionShockwaveRadius * 0.98F;
            float domeMidShellHighlightCoreAlpha = visibilityFadeFactor * 0.5F;
            float domeMidShellHighlightRimAlpha = visibilityFadeFactor * 0.7F;
            sunBatch.drawBall(false, SunRenderTypes.PLASMA_A, Vec3.ZERO, domeMidShellHighlightRadius, 0.3F, textureSlide, FLAME, FLAME, domeMidShellHighlightCoreAlpha, domeMidShellHighlightRimAlpha);

            float domeCoronaRadius = explosionShockwaveRadius * 0.99F;
            float domeCoronaTextureSlide = -textureSlide * 0.7F;
            float domeCoronaCoreAlpha = 0.45F * visibilityFadeFactor * (1.0F - explosionBlastFadeProgress);
            float domeCoronaRimAlpha = visibilityFadeFactor * 0.3F;
            sunBatch.drawBall(false, SunRenderTypes.PLASMA_B, Vec3.ZERO, domeCoronaRadius, -0.8F, domeCoronaTextureSlide, GOLD, ORANGE, domeCoronaCoreAlpha, domeCoronaRimAlpha);

            // hotter ball inside
            float coreRadius = explosionShockwaveRadius * 0.6F;
            float coreTextureSlide = textureSlide * 1.6F;
            float coreCoreAlpha = visibilityFadeFactor * 0.5F;
            float coreRimAlpha = visibilityFadeFactor * 0.2F;
            sunBatch.drawBall(false, SunRenderTypes.PLASMA_B, Vec3.ZERO, coreRadius, 0.5F, coreTextureSlide, coreColorGradient, GOLD, coreCoreAlpha, coreRimAlpha);

            // shock front
            float shockFrontTextureSlide = textureSlide * 0.4F;
            float shockFrontRimAlpha = 0.9F * visibilityFadeFactor * (1.0F - explosionBlastFadeProgress);
            sunBatch.drawBall(false, SunRenderTypes.PLASMA_B, Vec3.ZERO, explosionShockwaveRadius, 1.1F, shockFrontTextureSlide, PALE, PALE, 0.0F, shockFrontRimAlpha);

            // tongues, tips on the damage front
            int maxFlames = Math.max(24, (int) (70 * detail));
            float groundCullThreshold = -0.15F;
            float backfaceCullThreshold = -0.35F;
            float foreshorteningCullThreshold = 0.3F;

            for (int flames = 0; flames < maxFlames; flames++) {
                Vec3 normalDir = calculateSphereSurfacePoint(flames, maxFlames, rotationAngle * 0.3F, 0.3F);
                float cameraFacingFactor = sunBatch.facing(Vec3.ZERO, normalDir, explosionShockwaveRadius);

                if (normalDir.y < groundCullThreshold
                        || cameraFacingFactor < backfaceCullThreshold
                        || cameraFacingFactor > foreshorteningCullThreshold ) {
                    continue;
                }

                float flameFlickerIntensity = 0.5F + 0.5F * Mth.sin(time * 0.4F + flames * 2.13F);

                // fade with the solid fire, or they look like loose cones
                float edgeVisibilityFade = visibilityFadeFactor * solidnessFactor * Mth.clamp(1.15F - cameraFacingFactor, 0.0F, 1.0F);
                float tongueRadius = explosionShockwaveRadius * 0.72F;
                float tongueLengthA = explosionShockwaveRadius * (0.14F + 0.14F * flameFlickerIntensity);
                float tongueHalfWidthA = explosionShockwaveRadius * 0.12F;
                float tongueAlphaA = edgeVisibilityFade * 0.9F;
                sunBatch.drawTongue(Vec3.ZERO, normalDir, tongueRadius, tongueLengthA, tongueHalfWidthA, flames, FLAME, tongueAlphaA);

                float tongueLengthB = explosionShockwaveRadius * (0.08F + 0.08F * flameFlickerIntensity);
                float tongueHalfWidthB = explosionShockwaveRadius * 0.08F;
                float tongueAlphaB = edgeVisibilityFade * 0.8F;
                sunBatch.drawTongue(Vec3.ZERO, normalDir, tongueRadius, tongueLengthB, tongueHalfWidthB, flames + 2, GOLD, tongueAlphaB);
            }

            // ground shockwaves
            int maxGroundShockwaves = 3;
            for (int groundShockwaves = 0; groundShockwaves < maxGroundShockwaves; groundShockwaves++) {
                float shockwaveLag = Math.max(0.0F, 1.0F - groundShockwaves * 0.18F);
                Vec3 groundPosOffsetY = new Vec3(0, 0.1 * groundShockwaves, 0);
                float groundShockwaveRadius = explosionShockwaveRadius * shockwaveLag;
                int[] shockwaveColor = groundShockwaves == 0 ? PALE : ORANGE;
                float shockwaveAlpha = (0.9F - 0.2F * groundShockwaves) * visibilityFadeFactor;
                sunBatch.drawFlatRing(groundPosOffsetY, groundShockwaveRadius, shockwaveColor, shockwaveAlpha);
            }
        }

        // smoke, can go past the edge
        float smokeThreshold = 0.15F;
        if (explosionBlastFadeProgress > smokeThreshold) {
            float remainingSmokeTime = (explosionBlastFadeProgress - 0.15F) / 0.85F;
            float smokeAlpha = 0.6F * Mth.sin(Math.min(1.0F, remainingSmokeTime * 1.3F) * Mth.PI) + 0.1F * (1.0F - remainingSmokeTime);

            int maxSmokeParticles = 16;
            for (int smokeParticles = 0; smokeParticles < maxSmokeParticles; smokeParticles++) {
                float horizontalSpiralAngle = smokeParticles * 2.399F;

                float horizontalDistanceFactor = ((smokeParticles * 37) & 11) / 10.0F;
                float horizontalSpawnRadius = explosionBlastRadius * (0.2F + 0.5F * horizontalDistanceFactor);

                float verticalJitterMultiplier = 0.5F + 0.5F * ((smokeParticles * 13) % 7) / 6.0F;
                float verticalRiseAltitude = explosionBlastRadius * (0.1F + 0.9F * remainingSmokeTime) * verticalJitterMultiplier;

                Vec3 smokeSpawnPos = new Vec3(
                        Mth.cos(horizontalSpiralAngle) * horizontalSpawnRadius,
                        verticalRiseAltitude,
                        Mth.sin(horizontalSpiralAngle) * horizontalSpawnRadius
                );

                float scaleJitterMultiplier = 0.8F + 0.6F * remainingSmokeTime;
                float smokeParticleSize = explosionBlastRadius * (0.35F + 0.3F * ((smokeParticles * 7) % 5) / 4.0F) * scaleJitterMultiplier;
                float smokeAnimationSpinOffset = time * 0.01F + smokeParticles;
                sunBatch.drawSmoke(smokeSpawnPos, smokeParticleSize, smokeAnimationSpinOffset, SMOKE, smokeAlpha);
            }
        }
    }
}
