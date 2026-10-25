package net.ghostedhype.simplyanime.client.pose;

import net.ghostedhype.simplyanime.client.ClientTicks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.ghostedhype.simplyanime.ability.BladeSkill;
import net.ghostedhype.simplyanime.animation.AnimationClip;
import net.ghostedhype.simplyanime.animation.AnimationPose;
import net.ghostedhype.simplyanime.animation.AttackStyle;
import net.ghostedhype.simplyanime.config.ClientConfig;
import net.ghostedhype.simplyanime.entity.BladeSkillEntity;
import net.ghostedhype.simplyanime.entity.CruelSunEntity;
import net.ghostedhype.simplyanime.entity.EaVisualEntity;
import net.ghostedhype.simplyanime.entity.HeavenChainVisualEntity;
import net.ghostedhype.simplyanime.item.AnimeWeaponItem;
import net.ghostedhype.simplyanime.item.DarkRepulserItem;
import net.ghostedhype.simplyanime.item.ElucidatorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

public class BladePoses {

    private static final Map<LivingEntity, BladePoseState> STATES = new WeakHashMap<>();

    private static final float SKILL_BLEND = 2.0F;
    private static final float ATTACK_IN = 1.0F;
    private static final float ATTACK_OUT = 3.0F;
    private static final float CHAIN_PAUSE = 16.0F;
    private static final double REACH = 4.5;
    private static final float SWORD_SCALE = 0.85F;
    private static final float ELUCIDATOR_GRIP = -9.875F;
    private static final float DARK_REPULSER_GRIP = -9.125F;

    private static final Quaternionf SCRATCH = new Quaternionf();
    private static final Quaternionf REST_WRIST = new Quaternionf(0.5F, -0.5F, 0.5F, -0.5F);
    private static final Quaternionf REST = new Quaternionf();
    private static Boolean attacks;

    @Nullable
    public static AnimationPose pose(LivingEntity entity, float partialTick) {
        BladeSkillEntity skill = BladeSkillEntity.CLIENT_BY_OWNER.get(entity.getId());
        if (skill != null && (skill.isRemoved() || skill.getOwner() != entity)) {
            skill = null;
        }
        boolean wields = style(entity.getMainHandItem()) != null;
        BladePoseState state = STATES.get(entity);
        if (state == null) {
            if (skill == null && !wields) {
                return null;
            }
            state = new BladePoseState();
            STATES.put(entity, state);
        }
        AnimationPose pose = state.pose;
        if (state.tick == entity.tickCount && state.partialTick == partialTick) {
            return state.posed ? pose : null;
        }
        state.tick = entity.tickCount;
        state.partialTick = partialTick;
        state.hasApplied = false;
        pose.clear();
        if (skill != null) {
            state.attack = null;
            skill(entity, skill, pose, partialTick);
        } else if (wields) {
            attack(entity, state, pose, entity.tickCount + partialTick);
        } else {
            STATES.remove(entity);
        }
        state.posed = !pose.isEmpty();
        return state.posed ? pose : null;
    }

    private static void skill(LivingEntity entity, BladeSkillEntity skill, AnimationPose pose, float partialTick) {
        AnimationClip clip = skill.getSkill().clip();
        float clock = skill.clock(partialTick);
        float amount = Mth.clamp(Math.min(clock, clip.lengthTicks() - clock) / SKILL_BLEND, 0.0F, 1.0F);
        pose.add(clip, skill.frame(partialTick), amount);
        pose.locked = amount;
        if (skill.getSkill() == BladeSkill.STARBURST_STREAM) {
            float walking = Mth.clamp(entity.walkAnimation.speed(partialTick) * 3.0F, 0.0F, 1.0F);
            pose.weight[AnimationClip.RIGHT_LEG] *= 1.0F - walking;
            pose.weight[AnimationClip.LEFT_LEG] *= 1.0F - walking;
            pose.position[AnimationClip.ROOT].y *= 1.0F - walking;
        }
        if (skill.isMirror()) {
            pose.mirror();
        }
    }

    private static void attack(LivingEntity entity, BladePoseState state, AnimationPose pose, float time) {
        if (!attacks()) {
            return;
        }
        AttackStyle style = style(entity.getMainHandItem());
        if (state.swingTick != entity.tickCount) {
            state.swingTick = entity.tickCount;
            boolean swinging = entity.swinging && entity.swingingArm == InteractionHand.MAIN_HAND;
            if (swinging && (!state.wasSwinging || entity.swingTime < state.lastSwingTime) && attacking(entity)) {
                if (state.style != style || time - state.attackStart > recovery(entity) + CHAIN_PAUSE) {
                    state.chain = 0;
                }
                boolean both = style.pairs() && style(entity.getOffhandItem()) == style;
                state.style = style;
                state.attack = style.clip(state.chain, both);
                state.attackMirror = entity.getMainArm() == HumanoidArm.LEFT != (both && state.chain == 1);
                state.attackStart = time;
                state.chain = (state.chain + 1) % style.length();
            }
            state.wasSwinging = swinging;
            state.lastSwingTime = entity.swingTime;
        }
        AnimationClip clip = state.attack;
        if (clip == null) {
            return;
        }
        float clock = time - state.attackStart;
        if (clock >= clip.lengthTicks() || state.style != style || casting(entity)) {
            state.attack = null;
            return;
        }
        pose.add(clip, clock * clip.framesPerTick(), Mth.clamp(Math.min(clock / ATTACK_IN, (clip.lengthTicks() - clock) / ATTACK_OUT), 0.0F, 1.0F));
        if (state.attackMirror) {
            pose.mirror();
        }
        boolean rightMain = entity.getMainArm() == HumanoidArm.RIGHT;
        pose.rightSword = sword(rightMain ? entity.getMainHandItem() : entity.getOffhandItem());
        pose.leftSword = sword(rightMain ? entity.getOffhandItem() : entity.getMainHandItem());
    }

    private static float recovery(LivingEntity entity) {
        return entity instanceof Player player ? player.getCurrentItemAttackStrengthDelay() : 20.0F;
    }

    @Nullable
    private static AttackStyle style(ItemStack stack) {
        return stack.getItem() instanceof AnimeWeaponItem weapon ? weapon.attackStyle() : null;
    }

    public static boolean attacks() {
        if (attacks == null) {
            attacks = !ConfigApiJava.platform().isModLoaded("bettercombat");
        }
        return attacks && ClientConfig.get().attackAnimations;
    }

    private static boolean attacking(LivingEntity entity) {
        if (entity.getPose() != Pose.STANDING || entity.isPassenger() || casting(entity)) {
            return false;
        }
        // `mc` is fine here. Context makes it clear that `mc` refers to the Minecraft instance.
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player && !mc.options.keyAttack.isDown()) {
            return false;
        }
        return !aimsAtBlock(entity);
    }

    private static boolean casting(LivingEntity entity) {
        int id = entity.getId();
        return HeavenChainVisualEntity.CLIENT_BY_OWNER.containsKey(id) || CruelSunEntity.CLIENT_BY_OWNER.containsKey(id)
                || EaVisualEntity.CLIENT_BY_OWNER.containsKey(id);
    }

    public static boolean aimsAtBlock(LivingEntity entity) {
        HitResult block = entity.pick(REACH, 1.0F, false);
        if (block.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        Vec3 eye = entity.getEyePosition();
        Vec3 look = entity.getViewVector(1.0F).scale(REACH);
        return ProjectileUtil.getEntityHitResult(entity, eye, eye.add(look), entity.getBoundingBox().expandTowards(look).inflate(1.0),
                other -> !other.isSpectator() && other.isPickable(), block.getLocation().distanceToSqr(eye)) == null;
    }

    private static int sword(ItemStack stack) {
        if (stack.getItem() instanceof ElucidatorItem) {
            return AnimationClip.ELUCIDATOR;
        }
        return stack.getItem() instanceof DarkRepulserItem ? AnimationClip.DARK_REPULSER : 0;
    }

    public static boolean inSkill(LivingEntity entity) {
        BladeSkillEntity skill = BladeSkillEntity.CLIENT_BY_OWNER.get(entity.getId());
        return skill != null && !skill.isRemoved() && skill.getOwner() == entity;
    }

    public static void apply(LivingEntity entity, HumanoidModel<?> model, float headYaw, float headPitch) {
        float partialTick = ClientTicks.partialTick();
        AnimationPose pose = pose(entity, partialTick);
        ModelPart[] parts = {model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
        if (pose == null) {
            return;
        }
        BladePoseState state = STATES.get(entity);
        for (int i = 0; i < parts.length; i++) {
            int part = i + AnimationClip.HEAD;
            float weight = pose.weight[part];
            ModelPart target = parts[i];
            if (weight > 0.0F) {
                Quaternionf rotation = SCRATCH.set(pose.rotation[part]);
                if (part == AnimationClip.HEAD) {
                    float free = 1.0F - pose.locked;
                    rotation.rotateY(headYaw * Mth.DEG_TO_RAD * free).rotateX(headPitch * Mth.DEG_TO_RAD * free);
                }
                Vector3f position = pose.position[part];
                if (weight < 1.0F) {
                    Quaternionf vanillaRotation = new Quaternionf().rotationZYX(target.zRot, target.yRot, target.xRot);
                    rotation = vanillaRotation.nlerp(rotation, weight);
                    target.x = Mth.lerp(weight, target.x, position.x);
                    target.y = Mth.lerp(weight, target.y, position.y);
                    target.z = Mth.lerp(weight, target.z, position.z);
                } else {
                    target.x = position.x;
                    target.y = position.y;
                    target.z = position.z;
                }
                turn(target, rotation);
            }
            float[] kept = state.applied[i];
            kept[0] = target.x;
            kept[1] = target.y;
            kept[2] = target.z;
            kept[3] = target.xRot;
            kept[4] = target.yRot;
            kept[5] = target.zRot;
        }
        model.hat.copyFrom(model.head);
        state.hasApplied = pose.locked > 0.0F;
    }

    public static void turn(ModelPart part, Quaternionf rotation) {
        part.xRot = (float) Math.atan2(2.0F * (rotation.y * rotation.z + rotation.w * rotation.x), 1.0F - 2.0F * (rotation.x * rotation.x + rotation.y * rotation.y));
        part.yRot = (float) Math.asin(Mth.clamp(-2.0F * (rotation.x * rotation.z - rotation.w * rotation.y), -1.0F, 1.0F));
        part.zRot = (float) Math.atan2(2.0F * (rotation.x * rotation.y + rotation.w * rotation.z), 1.0F - 2.0F * (rotation.y * rotation.y + rotation.z * rotation.z));
    }

    public static void enforce(LivingEntity entity, HumanoidModel<?> model) {
        BladePoseState state = STATES.get(entity);
        if (state == null || !state.hasApplied || state.tick != entity.tickCount) {
            return;
        }
        ModelPart[] parts = {model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
        for (int i = 0; i < parts.length; i++) {
            float[] kept = state.applied[i];
            ModelPart target = parts[i];
            target.x = kept[0];
            target.y = kept[1];
            target.z = kept[2];
            target.xRot = kept[3];
            target.yRot = kept[4];
            target.zRot = kept[5];
        }
        model.hat.copyFrom(model.head);
    }

    public static void root(LivingEntity entity, PoseStack poseStack, float partialTick) {
        AnimationPose pose = pose(entity, partialTick);
        if (pose == null || pose.weight[AnimationClip.ROOT] <= 0.0F) {
            return;
        }
        float weight = pose.weight[AnimationClip.ROOT];
        float yaw = 180.0F - Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        Vector3f position = pose.position[AnimationClip.ROOT];
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(position.x * AnimationClip.PIXEL * weight, position.y * AnimationClip.PIXEL * weight,
                position.z * AnimationClip.PIXEL * weight);
        poseStack.mulPose(new Quaternionf().nlerp(pose.rotation[AnimationClip.ROOT], weight));
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
    }

    public static boolean renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                                     PoseStack poseStack, MultiBufferSource buffers, int light) {
        AnimationPose pose = pose(entity, ClientTicks.partialTick());
        int part = arm == HumanoidArm.RIGHT ? AnimationClip.RIGHT_ITEM : AnimationClip.LEFT_ITEM;
        if (pose == null || pose.weight[part] < 0.5F || stack.isEmpty()) {
            return false;
        }
        int sword = arm == HumanoidArm.RIGHT ? pose.rightSword : pose.leftSword;
        Vector3f position = pose.position[part];
        poseStack.pushPose();
        poseStack.translate(position.x / 16.0F, position.y / 16.0F, position.z / 16.0F);
        poseStack.mulPose(pose.rotation[part]);
        if (sword != 0 && sword == sword(stack)) {
            poseStack.scale(SWORD_SCALE, SWORD_SCALE, SWORD_SCALE);
            // item models are drawn centred on 8, 8, 8
            poseStack.translate(0.0F, -((sword == AnimationClip.ELUCIDATOR ? ELUCIDATOR_GRIP : DARK_REPULSER_GRIP) - 8.0F) / 16.0F, 0.0F);
            Minecraft.getInstance().getItemRenderer().renderStatic(entity, stack, ItemDisplayContext.NONE, false, poseStack, buffers,
                    entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId());
        } else if (pose.locked > 0.0F) {
            poseStack.popPose();
            return false;
        } else {
            boolean left = arm == HumanoidArm.LEFT;
            float side = left ? -1.0F : 1.0F;
            poseStack.mulPose(REST.set(REST_WRIST.x, side * REST_WRIST.y, side * REST_WRIST.z, REST_WRIST.w).conjugate());
            poseStack.translate(side / 16.0F, -9.0F / 16.0F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(side / 16.0F, 0.125F, -0.625F);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(entity, stack, context, left,
                    poseStack, buffers, light);
        }
        poseStack.popPose();
        return true;
    }
}
