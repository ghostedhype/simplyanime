package net.ghostedhype.simplyanime.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.ghostedhype.simplyanime.Constants;

// config/simplyanime/client.toml, each player's own
public class ClientConfig extends Config {

    private static ClientConfig instance;

    public ClientConfig() {
        super(Constants.tryId("client"));
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
    // light blocks near the big effects, client side only
    public boolean dynamicLight = true;
    public boolean skillCamera = true;
    public boolean skillShake = true;
    public boolean skillFlash = true;
    // the mod's own swings on a plain attack
    public boolean attackAnimations = true;
    // holding the attack button keeps attacking
    public boolean holdToAttack = true;
}
