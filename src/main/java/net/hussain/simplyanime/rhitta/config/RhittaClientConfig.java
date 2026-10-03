package net.hussain.simplyanime.rhitta.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.hussain.simplyanime.SimplyAnime;
import net.minecraft.util.Identifier;

// config/simplyanime/rhitta_client.toml, each player's own
public class RhittaClientConfig extends Config {

    private static RhittaClientConfig instance;

    public RhittaClientConfig() {
        super(new Identifier(SimplyAnime.MOD_ID, "rhitta_client"));
    }

    public static RhittaClientConfig get() {
        if (instance == null) {
            instance = ConfigApiJava.registerAndLoadConfig(RhittaClientConfig::new, RegisterType.CLIENT);
        }
        return instance;
    }

    public boolean idlePose = true;
    public boolean hudEnabled = true;
    public ValidatedEnum<HudCorner> hudCorner = new ValidatedEnum<>(HudCorner.BOTTOM_RIGHT);
    @ValidatedInt.Restrict(min = 0, max = 2000)
    public int hudOffsetX = 8;
    @ValidatedInt.Restrict(min = 0, max = 2000)
    public int hudOffsetY = 8;
    public boolean cameraZoom = true;
    @ValidatedFloat.Restrict(min = 0f, max = 3f)
    public float cameraZoomStrength = 1.0f;
    public boolean cameraShake = true;
    public boolean screenEffects = true;
    // scales every particle Rhitta spawns, 0 turns them off
    @ValidatedFloat.Restrict(min = 0f, max = 2f)
    public float particleDensity = 1.0f;

    public enum HudCorner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
}
