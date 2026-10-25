package net.ghostedhype.simplyanime.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ghostedhype.simplyanime.Constants;
import net.ghostedhype.simplyanime.ability.RhittaGrowth;
import net.ghostedhype.simplyanime.client.cinematic.BladeCinematics;
import net.ghostedhype.simplyanime.client.cinematic.RhittaCinematics;
import net.ghostedhype.simplyanime.client.effect.CruelSunEffects;
import net.ghostedhype.simplyanime.client.pose.BladePoses;
import net.ghostedhype.simplyanime.client.pose.RhittaPoses;
import net.ghostedhype.simplyanime.client.renderer.Later;
import net.ghostedhype.simplyanime.config.ClientConfig;
import net.ghostedhype.simplyanime.config.RhittaClientConfig;
import net.ghostedhype.simplyanime.item.AnimeWeaponItem;
import net.ghostedhype.simplyanime.item.EaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;

public class SimplyAnimeClientCommon {
    public static ClientHooks clientHooks = ClientHooks.NONE;

    public static void initializeClient() {
        clientHooks = new CruelSunEffects();
        ClientConfig.get();
        RhittaClientConfig.get();
        SimplySwordsClientAPI.registerUniqueTooltipNamespace(Constants.MOD_ID);
    }

    public static void tick(Minecraft client) {
        if (client.level != null) {
            RhittaPoses.tick(client.level);
            RhittaCinematics.tick(client);
            BladeCinematics.tick(client);
            holdAttack(client);
        }
    }

    private static void holdAttack(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.screen != null || client.gameMode == null || !client.options.keyAttack.isDown()
                || !ClientConfig.get().holdToAttack || !BladePoses.attacks() || player.isUsingItem() || player.isSpectator()
                || !(player.getMainHandItem().getItem() instanceof AnimeWeaponItem weapon) || weapon.attackStyle() == null
                || player.getAttackStrengthScale(0.0F) < 1.0F || BladePoses.inSkill(player) || BladePoses.aimsAtBlock(player)) {
            return;
        }
        if (client.hitResult instanceof EntityHitResult hit) {
            client.gameMode.attack(player, hit.getEntity());
        } else {
            player.resetAttackStrengthTicker();
        }
        player.swing(InteractionHand.MAIN_HAND);
    }

    public static void afterTranslucent() {
        Later.flush();
    }

    public static boolean blocksSwing() {
        Player player = Minecraft.getInstance().player;
        return player != null && player.getMainHandItem().getItem() instanceof EaItem;
    }

    public static void beforeRender(LivingEntity entity, PoseStack poseStack, float partialTick) {
        float scale = entity instanceof Player player ? RhittaGrowth.renderScale(player, partialTick) : 1.0F;
        poseStack.scale(scale, scale, scale);
        BladePoses.root(entity, poseStack, partialTick);
    }

    public static double fov(double fov, float partialTick, boolean configured) {
        return BladeCinematics.fov(RhittaCinematics.fov(fov, partialTick, configured), partialTick, configured);
    }

    public static float[] shake(float partialTick) {
        float[] sunShake = RhittaCinematics.shake(partialTick);
        float[] bladeShake = BladeCinematics.shake(partialTick);
        if (sunShake == null || bladeShake == null) {
            return sunShake == null ? bladeShake : sunShake;
        }
        return new float[]{sunShake[0] + bladeShake[0], sunShake[1] + bladeShake[1], sunShake[2] + bladeShake[2]};
    }

    public static double cameraPullBack() {
        return RhittaCinematics.cameraPullBack() + BladeCinematics.cameraPullBack();
    }

    public static void renderOverlay(GuiGraphics graphics, float partialTick) {
        RhittaCinematics.renderOverlay(graphics, partialTick);
        BladeCinematics.renderOverlay(graphics, partialTick);
    }

    public static void holdInput(Player player, Input input) {
        BladeCinematics.holdInput(player, input);
    }
}
