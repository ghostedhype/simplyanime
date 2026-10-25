package net.ghostedhype.simplyanime.client.cinematic;

import net.ghostedhype.simplyanime.client.ClientTicks;
import net.ghostedhype.simplyanime.ability.BladeSkill;
import net.ghostedhype.simplyanime.config.ClientConfig;
import net.ghostedhype.simplyanime.config.Config;
import net.ghostedhype.simplyanime.entity.BladeSkillEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public class BladeCinematics {

    // prev, current
    private static final float[] PULL = new float[2];
    private static final float[] FOV = new float[2];
    private static final float[] SHAKE = new float[2];
    private static final float[] RED = new float[2];
    private static final float[] TEAL = new float[2];
    private static final float[] WHITE = new float[2];

    private static final Map<BladeSkillEntity, float[]> SEEN = new WeakHashMap<>();

    public static void tick(Minecraft client) {
        for (float[] v : new float[][]{PULL, FOV, SHAKE, RED, TEAL, WHITE}) {
            v[0] = v[1];
        }
        if (client.player == null || client.level == null) {
            return;
        }
        Vec3 eye = client.player.getEyePosition();
        float pull = 0.0F;
        float fov = 0.0F;
        for (BladeSkillEntity skill : BladeSkillEntity.CLIENT_ACTIVE) {
            if (skill.isRemoved() || skill.level() != client.level) {
                continue;
            }
            float near = (float) Mth.clamp(1.0 - skill.position().distanceTo(eye) / 40.0, 0.0, 1.0);
            float[] seen = SEEN.computeIfAbsent(skill, bladeSkillEntity -> new float[]{-100.0F, -1.0F, -1.0F, -1.0F});
            boolean mine = skill.getOwner() == client.player;
            if (mine && skill.isRunning()) {
                boolean stream = skill.getSkill() == BladeSkill.STARBURST_STREAM;
                pull = Math.max(pull, stream ? 5.0F : 3.0F);
                fov = Math.max(fov, stream ? 9.0F : 5.0F);
            }
            if (skill.lastHitAt != seen[0]) {
                SHAKE[1] = Math.max(SHAKE[1], (mine ? 0.55F : 0.3F) * near);
                WHITE[1] = Math.max(WHITE[1], 0.05F * near);
            }
            if (skill.impactAt != seen[1]) {
                SHAKE[1] = Math.max(SHAKE[1], 3.0F * near);
                RED[1] = Math.max(RED[1], 0.3F * near);
                FOV[1] += mine ? 10.0F : 0.0F;
            }
            if (skill.releasedAt != seen[2]) {
                SHAKE[1] = Math.max(SHAKE[1], 2.4F * near);
                TEAL[1] = Math.max(TEAL[1], 0.28F * near);
            }
            if (skill.finaleAt != seen[3]) {
                SHAKE[1] = Math.max(SHAKE[1], 5.0F * near);
                WHITE[1] = Math.max(WHITE[1], 0.5F * near);
                FOV[1] += mine ? 12.0F : 0.0F;
            }
            seen[0] = skill.lastHitAt;
            seen[1] = skill.impactAt;
            seen[2] = skill.releasedAt;
            seen[3] = skill.finaleAt;
        }
        PULL[1] += (pull - PULL[1]) * (pull > PULL[1] ? 0.14F : 0.06F);
        FOV[1] += (fov - FOV[1]) * (fov > FOV[1] ? 0.14F : 0.08F);
        SHAKE[1] *= 0.84F;
        RED[1] *= 0.84F;
        TEAL[1] *= 0.84F;
        WHITE[1] *= 0.86F;
    }

    private static float get(float[] v, float partialTick) {
        return Mth.lerp(partialTick, v[0], v[1]);
    }

    private static boolean thirdPerson() {
        return !Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    public static double cameraPullBack() {
        return ClientConfig.get().skillCamera && thirdPerson() ? get(PULL, ClientTicks.partialTick()) : 0.0;
    }

    public static double fov(double fov, float partialTick, boolean configured) {
        return ClientConfig.get().skillCamera && configured ? Math.min(150.0, fov + get(FOV, partialTick)) : fov;
    }

    public static float[] shake(float partialTick) {
        float shake = get(SHAKE, partialTick);
        if (!ClientConfig.get().skillShake || shake < 0.01F) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        float clientTickProgress = (client.player == null ? 0 : client.player.tickCount) + partialTick;
        return new float[]{shake * Mth.sin(clientTickProgress * 3.1F), shake * 0.8F * Mth.sin(clientTickProgress * 3.7F + 1.0F), shake * 0.5F * Mth.sin(clientTickProgress * 2.3F + 2.0F)};
    }

    public static void renderOverlay(GuiGraphics context, float partialTick) {
        if (!ClientConfig.get().skillFlash || Minecraft.getInstance().options.hideGui) {
            return;
        }
        fill(context, 0xFF2038, get(RED, partialTick));
        fill(context, 0x7AFFE8, get(TEAL, partialTick));
        fill(context, 0xEEF6FF, get(WHITE, partialTick));
    }

    private static void fill(GuiGraphics context, int rgb, float alpha) {
        int a = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255);
        if (a > 1) {
            context.fill(0, 0, context.guiWidth(), context.guiHeight(), (a << 24) | rgb);
        }
    }

    public static void holdInput(LivingEntity player, Input input) {
        BladeSkillEntity skill = BladeSkillEntity.CLIENT_BY_OWNER.get(player.getId());
        if (skill == null || skill.isRemoved() || !skill.isRunning()) {
            return;
        }
        float left = switch (skill.getSkill()) {
            case STARBURST_STREAM -> Config.weapons.starburst_stream.moveSpeed;
            case VORPAL_STRIKE -> skill.impactAt < 0 && skill.frame(1.0F) < skill.getSkill().clip().marker("lunge_start") ? 0.3F : 0.0F;
            default -> 0.0F;
        };
        player.setSprinting(false);
        input.forwardImpulse *= left;
        input.leftImpulse *= left;
        input.jumping = false;
    }
}
