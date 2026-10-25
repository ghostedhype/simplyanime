package net.ghostedhype.simplyanime.client.registry;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.ghostedhype.simplyanime.client.renderer.*;
import net.ghostedhype.simplyanime.registry.EntityRegistry;

public class SimplyAnimeEntityRendererRegistryFabric {
    public static void register() {
        EntityRendererRegistry.register(EntityRegistry.HEAVEN_CHAIN_VISUAL.get(), HeavenChainVisualEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.HEAVEN_MARK_VISUAL.get(), HeavenMarkVisualEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.EA_VISUAL.get(), EaVisualEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.CRUEL_SUN.get(), CruelSunRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.BLADE_SKILL.get(), BladeSkillRenderer::new);
    }
}
