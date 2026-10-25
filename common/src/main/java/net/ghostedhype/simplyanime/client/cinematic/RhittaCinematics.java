package net.ghostedhype.simplyanime.client.cinematic;

import net.ghostedhype.simplyanime.client.ClientTicks;
import net.ghostedhype.simplyanime.config.RhittaClientConfig;
import net.ghostedhype.simplyanime.entity.CruelSunEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public class RhittaCinematics {

    // prev, current
    private static final float[] PULL = new float[2];
    private static final float[] FOV = new float[2];
    private static final float[] DARK = new float[2];
    private static final float[] GOLD = new float[2];
    private static final float[] FLASH = new float[2];
    private static final float[] SHAKE = new float[2];

    private static final Map<CruelSunEntity, float[]> SEEN = new WeakHashMap<>();

    public static void tick(Minecraft client) {
        for (float[] v : new float[][]{PULL, FOV, DARK, GOLD, FLASH, SHAKE}) {
            v[0] = v[1];
        }
        if (client.player == null || client.level == null) {
            return;
        }
        Vec3 eye = client.player.getEyePosition();
        float pull = 0, fov = 0, dark = 0, gold = 0;

        for (CruelSunEntity sun : CruelSunEntity.CLIENT_ACTIVE) {
            if (sun.isRemoved() || sun.level() != client.level) {
                continue;
            }
            int phase = sun.getPhase();
            float ticks = sun.getPhaseTicks(0.0F);
            float[] seen = SEEN.computeIfAbsent(sun, cruelSunEntity -> new float[]{-1, -1});
            LivingEntity owner = sun.getOwner();

            if (phase == CruelSunEntity.PHASE_CHARGE && owner != null) {
                float orb = sun.getOrbRadius(0.0F);
                Vec3 positionVec = CruelSunEntity.chargeCenter(owner, 1.0F, orb);
                float near = (float) Mth.clamp(1.0 - positionVec.distanceTo(eye) / (48.0 + orb * 4.0), 0.0, 1.0);
                if (owner == client.player) {
                    // far enough back to fit the whole sun
                    float boost = 20.0F * Math.min(1.0F, orb / 6.0F);
                    double top = positionVec.y - owner.getY() + orb * CruelSunEntity.HEAT_EDGE;
                    double wide = orb * CruelSunEntity.HEAT_EDGE;
                    double half = Math.toRadians((client.options.fov().get() + boost) * 0.5);
                    double needed = Math.max(top, wide) / Math.tan(half) * 1.2;
                    pull = Math.max(pull, (float) Math.max(0.0, needed - 4.0));
                    fov = Math.max(fov, boost);
                }
                if (ticks < CruelSunEntity.IGNITE_TICKS) {
                    dark = Math.max(dark, 0.5F * near * Math.min(1.0F, ticks / 20.0F));
                } else {
                    gold = Math.max(gold, 0.2F * near * Math.min(1.0F, (ticks - CruelSunEntity.IGNITE_TICKS) / 20.0F));
                    if (seen[0] == CruelSunEntity.PHASE_CHARGE && seen[1] < CruelSunEntity.IGNITE_TICKS) {
                        FLASH[1] = Math.max(FLASH[1], 0.75F * near);
                        SHAKE[1] = Math.max(SHAKE[1], 1.5F * near);
                    }
                }
            } else if (phase == CruelSunEntity.PHASE_BLAST) {
                float radius = sun.getBlastRadius();
                float near = (float) Mth.clamp(1.0 - sun.position().distanceTo(eye) / (radius * 5.0), 0.0, 1.0);
                if (seen[0] != CruelSunEntity.PHASE_BLAST) {
                    FLASH[1] = Math.max(FLASH[1], near);
                    SHAKE[1] = Math.max(SHAKE[1], 4.0F * near);
                }
                gold = Math.max(gold, 0.35F * near * (1.0F - CruelSunEntity.blastFade(ticks)));
            }
            seen[0] = phase;
            seen[1] = ticks;
        }

        // out fast, back slow
        PULL[1] += (pull - PULL[1]) * (pull > PULL[1] ? 0.12F : 0.045F);
        FOV[1] += (fov - FOV[1]) * (fov > FOV[1] ? 0.1F : 0.045F);
        DARK[1] += (dark - DARK[1]) * 0.15F;
        GOLD[1] += (gold - GOLD[1]) * 0.12F;
        FLASH[1] *= 0.86F;
        SHAKE[1] *= 0.88F;
    }

    private static float get(float[] v, float partialTick) {
        return Mth.lerp(partialTick, v[0], v[1]);
    }

    private static boolean thirdPerson() {
        return !Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    public static double cameraPullBack() {
        RhittaClientConfig config = RhittaClientConfig.get();
        if (!config.cameraZoom || !thirdPerson()) {
            return 0.0;
        }
        return get(PULL, ClientTicks.partialTick()) * config.cameraZoomStrength;
    }

    public static double fov(double fov, float partialTick, boolean configured) {
        RhittaClientConfig config = RhittaClientConfig.get();
        if (config.cameraZoom && thirdPerson() && configured) {
            return Math.min(150.0, fov + get(FOV, partialTick) * config.cameraZoomStrength);
        }
        return fov;
    }

    public static float[] shake(float partialTick) {
        float shake = get(SHAKE, partialTick);
        if (!RhittaClientConfig.get().cameraShake || shake < 0.01F) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        float clientTickProgress = (client.player == null ? 0 : client.player.tickCount) + partialTick;
        return new float[]{shake * Mth.sin(clientTickProgress * 2.7F), shake * 0.8F * Mth.sin(clientTickProgress * 3.3F + 1.0F), shake * 0.6F * Mth.sin(clientTickProgress * 2.1F + 2.0F)};
    }

    public static void renderOverlay(GuiGraphics context, float partialTick) {
        if (!RhittaClientConfig.get().screenEffects || Minecraft.getInstance().options.hideGui) {
            return;
        }
        int width = context.guiWidth();
        int height = context.guiHeight();
        fill(context, width, height, 0x0A0414, get(DARK, partialTick));
        fill(context, width, height, 0xFFC040, get(GOLD, partialTick));
        fill(context, width, height, 0xFFF8E6, get(FLASH, partialTick));
    }

    private static void fill(GuiGraphics context, int width, int height, int rgb, float alpha) {
        int a = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255);
        if (a > 1) {
            context.fill(0, 0, width, height, (a << 24) | rgb);
        }
    }
}
