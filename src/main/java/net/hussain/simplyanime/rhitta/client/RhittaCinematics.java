package net.hussain.simplyanime.rhitta.client;

import net.hussain.simplyanime.rhitta.config.RhittaClientConfig;
import net.hussain.simplyanime.rhitta.entity.CruelSunEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Map;
import java.util.WeakHashMap;

// camera pull back and the screen darken/flash for Cruel Sun
public class RhittaCinematics {

    public static final IGuiOverlay OVERLAY = RhittaCinematics::renderOverlay;

    // prev, current
    private static final float[] PULL = new float[2];
    private static final float[] FOV = new float[2];
    private static final float[] DARK = new float[2];
    private static final float[] GOLD = new float[2];
    private static final float[] FLASH = new float[2];
    private static final float[] SHAKE = new float[2];

    // the last phase and tick each sun was seen at, to catch the moment it erupts or lands
    private static final Map<CruelSunEntity, float[]> SEEN = new WeakHashMap<>();

    public static void tick(MinecraftClient client) {
        for (float[] v : new float[][]{PULL, FOV, DARK, GOLD, FLASH, SHAKE}) {
            v[0] = v[1];
        }
        if (client.player == null || client.world == null) {
            return;
        }
        Vec3d eye = client.player.getEyePos();
        float pull = 0, fov = 0, dark = 0, gold = 0;

        for (CruelSunEntity sun : CruelSunEntity.CLIENT_ACTIVE) {
            if (sun.isRemoved() || sun.getWorld() != client.world) {
                continue;
            }
            int phase = sun.getPhase();
            float ticks = sun.getPhaseTicks(0.0F);
            float[] seen = SEEN.computeIfAbsent(sun, s -> new float[]{-1, -1});
            LivingEntity owner = sun.getOwner();

            if (phase == CruelSunEntity.PHASE_CHARGE && owner != null) {
                float orb = sun.getOrbRadius(0.0F);
                Vec3d at = CruelSunEntity.chargeCenter(owner, 1.0F, orb);
                float near = (float) MathHelper.clamp(1.0 - at.distanceTo(eye) / (48.0 + orb * 4.0), 0.0, 1.0);
                if (owner == client.player) {
                    // far enough back to fit the whole sun
                    float boost = 20.0F * Math.min(1.0F, orb / 6.0F);
                    double top = at.y - owner.getY() + orb * CruelSunEntity.HEAT_EDGE;
                    double wide = orb * CruelSunEntity.HEAT_EDGE;
                    double half = Math.toRadians((client.options.getFov().getValue() + boost) * 0.5);
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
                float near = (float) MathHelper.clamp(1.0 - sun.getPos().distanceTo(eye) / (radius * 5.0), 0.0, 1.0);
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

    private static float get(float[] v, float tickDelta) {
        return MathHelper.lerp(tickDelta, v[0], v[1]);
    }

    private static boolean thirdPerson() {
        return !MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
    }

    // extra distance for the third person camera, read by CameraMixin
    public static double cameraPullBack() {
        RhittaClientConfig cfg = RhittaClientConfig.get();
        if (!cfg.cameraZoom || !thirdPerson()) {
            return 0.0;
        }
        return get(PULL, MinecraftClient.getInstance().getTickDelta()) * cfg.cameraZoomStrength;
    }

    public static void onFov(ViewportEvent.ComputeFov event) {
        RhittaClientConfig cfg = RhittaClientConfig.get();
        if (cfg.cameraZoom && thirdPerson() && event.usedConfiguredFov()) {
            event.setFOV(Math.min(150.0, event.getFOV() + get(FOV, (float) event.getPartialTick()) * cfg.cameraZoomStrength));
        }
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float shake = get(SHAKE, (float) event.getPartialTick());
        if (!RhittaClientConfig.get().cameraShake || shake < 0.01F) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        float t = (client.player == null ? 0 : client.player.age) + (float) event.getPartialTick();
        event.setYaw(event.getYaw() + shake * MathHelper.sin(t * 2.7F));
        event.setPitch(event.getPitch() + shake * 0.8F * MathHelper.sin(t * 3.3F + 1.0F));
        event.setRoll(event.getRoll() + shake * 0.6F * MathHelper.sin(t * 2.1F + 2.0F));
    }

    private static void renderOverlay(ForgeGui gui, DrawContext context, float partialTick, int width, int height) {
        if (!RhittaClientConfig.get().screenEffects) {
            return;
        }
        fill(context, width, height, 0x0A0414, get(DARK, partialTick));
        fill(context, width, height, 0xFFC040, get(GOLD, partialTick));
        fill(context, width, height, 0xFFF8E6, get(FLASH, partialTick));
    }

    private static void fill(DrawContext context, int width, int height, int rgb, float alpha) {
        int a = (int) (MathHelper.clamp(alpha, 0.0F, 1.0F) * 255);
        if (a > 1) {
            context.fill(0, 0, width, height, (a << 24) | rgb);
        }
    }
}
