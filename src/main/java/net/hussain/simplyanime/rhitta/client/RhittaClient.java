package net.hussain.simplyanime.rhitta.client;

import dev.architectury.platform.forge.EventBuses;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.hussain.simplyanime.SimplyAnime;
import net.hussain.simplyanime.rhitta.RhittaGrowth;
import net.hussain.simplyanime.rhitta.RhittaRegistry;
import net.hussain.simplyanime.rhitta.config.RhittaClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

public class RhittaClient {

    public static void init() {
        RhittaClientConfig.get();
        EntityRendererRegistry.register(RhittaRegistry.CRUEL_SUN, CruelSunRenderer::new);

        IEventBus modBus = EventBuses.getModEventBus(SimplyAnime.MOD_ID).orElseThrow();
        modBus.addListener(RhittaClient::onParticles);
        modBus.addListener(RhittaClient::onOverlays);

        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, RhittaClient::onRenderPre);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, RhittaClient::onRenderPost);
        MinecraftForge.EVENT_BUS.addListener(RhittaClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(RhittaCinematics::onFov);
        MinecraftForge.EVENT_BUS.addListener(RhittaCinematics::onCameraAngles);
    }

    private static void onParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RhittaRegistry.SUN_FLAME.get(), SunParticle.FlameFactory::new);
        event.registerSpriteSet(RhittaRegistry.SUN_EMBER.get(), SunParticle.EmberFactory::new);
        event.registerSpriteSet(RhittaRegistry.SUN_SPARK.get(), SunGlowParticle.SparkFactory::new);
        event.registerSpriteSet(RhittaRegistry.SUN_WISP.get(), SunGlowParticle.WispFactory::new);
    }

    private static void onOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("rhitta_sun", RhittaHud.INSTANCE);
        // under the rest of the HUD so the hotbar and chat stay readable through a flash
        event.registerBelowAll("rhitta_screen", RhittaCinematics.OVERLAY);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && MinecraftClient.getInstance().world != null) {
            RhittaPoses.tick(MinecraftClient.getInstance().world);
            RhittaCinematics.tick(MinecraftClient.getInstance());
        }
    }

    // the wielder grows toward noon, scaled round their feet. Pre and Post always pair up
    // because nothing runs after LOWEST to cancel the render in between
    private static void onRenderPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.isCanceled() || !(event.getEntity() instanceof PlayerEntity player)) {
            return;
        }
        MatrixStack matrices = event.getPoseStack();
        matrices.push();
        float scale = RhittaGrowth.renderScale(player, event.getPartialTick());
        matrices.scale(scale, scale, scale);
    }

    private static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof PlayerEntity) {
            event.getPoseStack().pop();
        }
    }
}
