package net.hussain.simplyanime.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;

public final class Config {

    public static void init() {}

    public static final WeaponsConfig weapons = ConfigApiJava.registerAndLoadConfig(WeaponsConfig::new);
}
