package net.hussain.simplyanime.client;

import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.hussain.simplyanime.SimplyAnime;
import net.hussain.simplyanime.callout.Callout;
import net.hussain.simplyanime.callout.Callouts;
import net.hussain.simplyanime.client.particle.HeavenSparkParticle;
import net.hussain.simplyanime.client.particle.RuptureEmberParticle;
import net.hussain.simplyanime.client.particle.SeveredAshParticle;
import net.hussain.simplyanime.client.renderer.ArmChainFeatureRenderer;
import net.hussain.simplyanime.client.renderer.EnumaElishVisualEntityRenderer;
import net.hussain.simplyanime.client.renderer.HeavenChainVisualEntityRenderer;
import net.hussain.simplyanime.client.renderer.HeavenMarkVisualEntityRenderer;
import net.hussain.simplyanime.registry.EntityRegistry;
import net.hussain.simplyanime.registry.ParticlesRegistry;
import net.hussain.simplyanime.rhitta.client.RhittaClient;
import net.hussain.simplyanime.item.EnumaElishItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;

@Mod.EventBusSubscriber(modid = SimplyAnime.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SimplyAnimeClient {

    public static void init() {
        SimplySwordsClientAPI.registerUniqueTooltipNamespace(SimplyAnime.MOD_ID);
        EntityRendererRegistry.register(EntityRegistry.HEAVEN_CHAIN_VISUAL, HeavenChainVisualEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.HEAVEN_MARK_VISUAL, HeavenMarkVisualEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.ENUMA_ELISH_VISUAL, EnumaElishVisualEntityRenderer::new);
        RhittaClient.init();
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, Callouts.PACKET, (buf, context) -> {
            Callout callout = buf.readEnumConstant(Callout.class);
            context.queue(() -> CalloutHud.INSTANCE.show(callout));
        });
        MinecraftForge.EVENT_BUS.addListener(SimplyAnimeClient::onClickInput);
    }

    // no swing and no hit for Ea's left click, the attack is cancelled on the server too
    private static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (event.isAttack() && player != null && player.getMainHandStack().getItem() instanceof EnumaElishItem) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("callout", CalloutHud.INSTANCE);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerEntityRenderer renderer) {
                renderer.addFeature(new ArmChainFeatureRenderer(renderer, skin.equals("slim")));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticlesRegistry.HEAVEN_SPARK.get(), HeavenSparkParticle.Factory::new);
        event.registerSpriteSet(ParticlesRegistry.SEVERED_ASH.get(), SeveredAshParticle.Factory::new);
        event.registerSpriteSet(ParticlesRegistry.RUPTURE_EMBER.get(), RuptureEmberParticle.Factory::new);
    }
}
