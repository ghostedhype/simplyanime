package net.ghostedhype.simplyanime.client;

import net.ghostedhype.simplyanime.Constants;
import net.ghostedhype.simplyanime.client.hud.CalloutHud;
import net.ghostedhype.simplyanime.client.hud.RhittaHud;
import net.ghostedhype.simplyanime.client.pose.FirstPerson;
import net.ghostedhype.simplyanime.client.registry.SimplyAnimeEntityRenderRegistryNeoForge;
import net.ghostedhype.simplyanime.client.registry.SimplyAnimeParticleRegistryNeoForge;
import net.ghostedhype.simplyanime.client.renderer.ArmChainRenderLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Constants.MOD_ID, dist = {Dist.CLIENT})
public class SimplyAnimeNeoForgeClient {

    public SimplyAnimeNeoForgeClient(IEventBus modEventBus) {
        SimplyAnimeClientCommon.initializeClient();
        modEventBus.addListener(SimplyAnimeParticleRegistryNeoForge::register);
        modEventBus.addListener(SimplyAnimeEntityRenderRegistryNeoForge::register);
        modEventBus.addListener(SimplyAnimeNeoForgeClient::onRegisterGuiLayers);
        modEventBus.addListener(SimplyAnimeNeoForgeClient::onAddLayers);

        IEventBus gameEventBus = NeoForge.EVENT_BUS;
        gameEventBus.addListener(SimplyAnimeNeoForgeClient::onClickInput);
        gameEventBus.addListener(SimplyAnimeNeoForgeClient::onRenderHand);
        gameEventBus.addListener((ClientTickEvent.Post event) -> SimplyAnimeClientCommon.tick(Minecraft.getInstance()));
        gameEventBus.addListener(SimplyAnimeNeoForgeClient::onFov);
        gameEventBus.addListener(SimplyAnimeNeoForgeClient::onRenderLevelStage);
        gameEventBus.addListener((MovementInputUpdateEvent event) ->
                SimplyAnimeClientCommon.holdInput(event.getEntity(), event.getInput()));
        gameEventBus.addListener(SimplyAnimeNeoForgeClient::onCameraAngles);
        gameEventBus.addListener(EventPriority.LOWEST, SimplyAnimeNeoForgeClient::onRenderLivingPre);
        gameEventBus.addListener(EventPriority.LOWEST, SimplyAnimeNeoForgeClient::onRenderLivingPost);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Constants.tryId("callout"), (graphics, delta) ->
                CalloutHud.INSTANCE.render(graphics, delta.getGameTimeDeltaPartialTick(false)));
        event.registerAboveAll(Constants.tryId("rhitta_sun"), (graphics, delta) ->
                RhittaHud.INSTANCE.render(graphics, delta.getGameTimeDeltaPartialTick(false)));
        event.registerBelowAll(Constants.tryId("rhitta_screen"), (graphics, delta) ->
                SimplyAnimeClientCommon.renderOverlay(graphics, delta.getGameTimeDeltaPartialTick(false)));
    }

    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new ArmChainRenderLayer(renderer));
            }
        }
    }

    private static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && SimplyAnimeClientCommon.blocksSwing()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    private static void onRenderHand(RenderHandEvent event) {
        if (FirstPerson.render(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getHand(),
                event.getItemStack(), event.getPartialTick(), event.getEquipProgress(), event.getSwingProgress())) {
            event.setCanceled(true);
        }
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            SimplyAnimeClientCommon.afterTranslucent();
        }
    }

    private static void onFov(ViewportEvent.ComputeFov event) {
        event.setFOV(SimplyAnimeClientCommon.fov(event.getFOV(), (float) event.getPartialTick(), event.usedConfiguredFov()));
    }

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float[] shake = SimplyAnimeClientCommon.shake((float) event.getPartialTick());
        if (shake != null) {
            event.setYaw(event.getYaw() + shake[0]);
            event.setPitch(event.getPitch() + shake[1]);
            event.setRoll(event.getRoll() + shake[2]);
        }
    }

    private static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.isCanceled()) {
            return;
        }
        event.getPoseStack().pushPose();
        SimplyAnimeClientCommon.beforeRender(event.getEntity(), event.getPoseStack(), event.getPartialTick());
    }

    private static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        event.getPoseStack().popPose();
    }
}
