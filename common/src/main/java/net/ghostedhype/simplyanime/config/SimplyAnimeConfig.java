package net.ghostedhype.simplyanime.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;

public class SimplyAnimeConfig {
    public static void init() {}

    public static final SimplyAnimeWeaponsConfig weaponsConfig =
            ConfigApiJava.registerAndLoadConfig(SimplyAnimeWeaponsConfig::new);
}
