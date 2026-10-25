package net.ghostedhype.simplyanime.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.ghostedhype.simplyanime.client.hud.CalloutHud;
import net.ghostedhype.simplyanime.client.hud.RhittaHud;
import net.ghostedhype.simplyanime.client.registry.SimplyAnimeEntityRendererRegistryFabric;
import net.ghostedhype.simplyanime.client.registry.SimplyAnimeParticleRegistryFabric;
import net.ghostedhype.simplyanime.client.renderer.ArmChainRenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;

public class SimplyAnimeFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SimplyAnimeClientCommon.initializeClient();
        SimplyAnimeEntityRendererRegistryFabric.register();
        SimplyAnimeParticleRegistryFabric.register();

        ClientTickEvents.END_CLIENT_TICK.register(SimplyAnimeClientCommon::tick);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> SimplyAnimeClientCommon.afterTranslucent());
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            float partialTick = tickCounter.getGameTimeDeltaPartialTick(false);
            SimplyAnimeClientCommon.renderOverlay(graphics, partialTick);
            CalloutHud.INSTANCE.render(graphics, partialTick);
            RhittaHud.INSTANCE.render(graphics, partialTick);
        });
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, registrationHelper, context) -> {
            if (entityType == EntityType.PLAYER && renderer instanceof PlayerRenderer playerRenderer) {
                registrationHelper.register(new ArmChainRenderLayer(playerRenderer));
            }
        });
    }
}
