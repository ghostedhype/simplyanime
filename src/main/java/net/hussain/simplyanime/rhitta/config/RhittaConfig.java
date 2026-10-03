package net.hussain.simplyanime.rhitta.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.hussain.simplyanime.SimplyAnime;
import net.hussain.simplyanime.rhitta.SunPower;
import net.minecraft.util.Identifier;

// config/simplyanime/rhitta.toml, synced from the server
public class RhittaConfig extends Config {

    private static RhittaConfig instance;

    public RhittaConfig() {
        super(new Identifier(SimplyAnime.MOD_ID, "rhitta"));
    }

    public static void init() {
        get();
    }

    public static RhittaConfig get() {
        if (instance == null) {
            instance = ConfigApiJava.registerAndLoadConfig(RhittaConfig::new);
        }
        return instance;
    }

    public AxeSettings axe = new AxeSettings();
    public SunSettings sun = new SunSettings();
    public CruelSunSettings cruelSun = new CruelSunSettings();
    public GrowthSettings growth = new GrowthSettings();

    public static class AxeSettings extends ConfigSection {
        public float damageModifier = 7.0f;
        public float attackSpeed = -3.1f;
        public boolean lootable = true;
        // burn seconds = base + scaling per point of sun power over 1
        @ValidatedFloat.Restrict(min = 0f, max = 60f)
        public float burnSeconds = 3.0f;
        @ValidatedFloat.Restrict(min = 0f, max = 60f)
        public float burnSunScaling = 3.0f;
    }

    public static class SunSettings extends ConfigSection {
        @ValidatedFloat.Restrict(min = 0f, max = 20f)
        public float minPower = 1.0f;
        @ValidatedFloat.Restrict(min = 0f, max = 20f)
        public float peakPower = 2.0f;
        public ValidatedEnum<SunPower.Curve> curve = new ValidatedEnum<>(SunPower.Curve.SMOOTH);
        public boolean weatherMatters = true;
        @ValidatedFloat.Restrict(min = 0f, max = 1f)
        public float rainFactor = 0.6f;
        @ValidatedFloat.Restrict(min = 0f, max = 1f)
        public float thunderFactor = 0.35f;
        public boolean indoorsMatter = true;
        // 1 means no sky access takes away the whole sun bonus
        @ValidatedFloat.Restrict(min = 0f, max = 1f)
        public float indoorPenalty = 0.8f;
        // dimensions without a sky or day cycle, like the Nether and the End
        public ValidatedEnum<SunPower.SkylessMode> skylessMode = new ValidatedEnum<>(SunPower.SkylessMode.NOON);
        @ValidatedFloat.Restrict(min = 0f, max = 1f)
        public float skylessLevel = 0.5f;
        // "dimension=level" entries split by commas, level 0..1, skips every other check in that dimension
        public String dimensionOverrides = "";
    }

    public static class CruelSunSettings extends ConfigSection {
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 1200;
        // cooldown multipliers at midnight and at noon, blended by the sun in between
        @ValidatedFloat.Restrict(min = 0.1f, max = 5f)
        public float nightCooldownMultiplier = 1.25f;
        @ValidatedFloat.Restrict(min = 0.1f, max = 5f)
        public float noonCooldownMultiplier = 0.6f;
        @ValidatedInt.Restrict(min = 0)
        public int minCooldown = 400;

        // default is timed to the build up audio
        @ValidatedInt.Restrict(min = 60, max = 600)
        public int chargeDuration = 120;
        @ValidatedInt.Restrict(min = -1, max = 9)
        public int chargeSlowness = 2;
        // can't be hurt while the sun builds, void and /kill still go through
        public boolean chargeInvulnerable = true;

        @ValidatedDouble.Restrict(min = 0.2, max = 8.0)
        public double projectileSpeed = 1.6;
        @ValidatedDouble.Restrict(min = 4.0, max = 256.0)
        public double range = 96.0;

        // sizes at night, the sun scaling adds up to that much again at noon
        @ValidatedFloat.Restrict(min = 0.5f, max = 24f)
        public float orbRadius = 6.0f;
        @ValidatedFloat.Restrict(min = 0f, max = 5f)
        public float orbSunScaling = 1.0f;
        @ValidatedFloat.Restrict(min = 1f, max = 64f)
        public float blastRadius = 16.0f;
        @ValidatedFloat.Restrict(min = 0f, max = 5f)
        public float blastSunScaling = 1.0f;

        // the heat that burns anything near the sun while it forms and flies
        @ValidatedFloat.Restrict(min = 0f)
        public float heatDamageScaling = 0.3f;
        @ValidatedFloat.Restrict(min = 0f)
        public float heatSpellScaling = 0.9f;
        @ValidatedInt.Restrict(min = 1)
        public int heatInterval = 5;
        // the explosion, full at the centre down to edgeDamage of it at the rim
        @ValidatedFloat.Restrict(min = 0f)
        public float blastDamageScaling = 7.0f;
        @ValidatedFloat.Restrict(min = 0f)
        public float blastSpellScaling = 20.0f;
        @ValidatedFloat.Restrict(min = 0f, max = 1f)
        public float edgeDamage = 0.4f;
        @ValidatedDouble.Restrict(min = 0.0, max = 6.0)
        public double knockback = 2.2;
        @ValidatedFloat.Restrict(min = 0f, max = 60f)
        public float blastBurnSeconds = 6.0f;

        // drawn only, nothing placed or broken
        public boolean scorchSurroundings = true;
        // these change the world, all off by default. the block ones follow mobGriefing
        public boolean surroundingsCatchFire = false;
        public boolean explosionBreaksBlocks = false;
        public boolean explosionStartsFires = false;
        // mobs in the sun's heat catch fire for this long, 0 leaves burning to the explosion and basic hits
        @ValidatedFloat.Restrict(min = 0f, max = 60f)
        public float heatBurnSeconds = 0.0f;

        // volume above 1 only widens how far away it can be heard
        @ValidatedFloat.Restrict(min = 0f, max = 16f)
        public float buildUpVolume = 5.0f;
        // the ability's name above the hotbar as the sun is thrown
        public boolean nameText = true;
        // ticks before the throw that the name shows
        @ValidatedInt.Restrict(min = 0, max = 200)
        public int nameTextLead = 37;
        @ValidatedFloat.Restrict(min = 0f, max = 16f)
        public float launchVolume = 3.0f;
        @ValidatedFloat.Restrict(min = 0f, max = 16f)
        public float flightVolume = 1.5f;
        @ValidatedFloat.Restrict(min = 0f, max = 16f)
        public float blastVolume = 10.0f;
    }

    public static class GrowthSettings extends ConfigSection {
        public boolean enabled = true;
        @ValidatedFloat.Restrict(min = 1f, max = 3f)
        public float maxScale = 1.25f;
        // off by default, only the model grows. On, the hitbox, eye height and reach grow too
        public boolean scaleHitbox = false;
    }
}
