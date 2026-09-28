package net.hussain.simplyanime.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.hussain.simplyanime.SimplyAnime;
import net.hussain.simplyanime.client.particle.HeavenSparkParticle;
import net.hussain.simplyanime.client.particle.RuptureEmberParticle;
import net.hussain.simplyanime.client.particle.SeveredAshParticle;
import net.hussain.simplyanime.client.renderer.ArmChainFeatureRenderer;
import net.hussain.simplyanime.client.renderer.EnumaElishVisualEntityRenderer;
import net.hussain.simplyanime.client.renderer.HeavenChainVisualEntityRenderer;
import net.hussain.simplyanime.client.renderer.HeavenMarkVisualEntityRenderer;
import net.hussain.simplyanime.registry.EntityRegistry;
import net.hussain.simplyanime.registry.ParticlesRegistry;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
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
