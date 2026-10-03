package net.hussain.simplyanime.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.hussain.simplyanime.SimplyAnime;
import net.minecraft.util.Identifier;

// config/simplyanime/client.toml, each player's own
public class ClientConfig extends Config {

    private static ClientConfig instance;

    public ClientConfig() {
        super(new Identifier(SimplyAnime.MOD_ID, "client"));
    }

    public static ClientConfig get() {
        if (instance == null) {
            instance = ConfigApiJava.registerAndLoadConfig(ClientConfig::new, RegisterType.CLIENT);
        }
        return instance;
    }

    // glow strength under shader packs
    @ValidatedFloat.Restrict(min = 0.2f, max = 2f)
    public float shaderGlow = 1.0f;
    // see EffectLights
    public boolean dynamicLight = true;
}
