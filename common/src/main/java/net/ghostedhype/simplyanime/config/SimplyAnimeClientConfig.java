package net.ghostedhype.simplyanime.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.ghostedhype.simplyanime.Constants;
import net.minecraft.resources.ResourceLocation;

public class SimplyAnimeClientConfig extends Config {
    private static SimplyAnimeClientConfig instance;

    public SimplyAnimeClientConfig() {
        super(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "client"));
    }

    public static SimplyAnimeClientConfig get() {
        if (instance == null)
            instance = ConfigApiJava.registerAndLoadConfig(SimplyAnimeClientConfig::new, RegisterType.CLIENT);
        return instance;
    }

    @Translatable.Name("config.simplyanime.client.shader_glow.name")
    @Translatable.Desc("config.simplyanime.client.shader_glow.description")
    @ValidatedFloat.Restrict(min = 0.2f, max = 2.0f)
    public float shaderGlow = 1.0f;

    @Translatable.Name("config.simplyanime.client.dynamic_light.name")
    @Translatable.Desc("config.simplyanime.client.dynamic_light.description")
    public boolean dynamicLight = true;
}
