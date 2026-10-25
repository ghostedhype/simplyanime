package net.ghostedhype.simplyanime.config;

import net.ghostedhype.simplyanime.Constants;

public class WeaponsConfig extends me.fzzyhmstrs.fzzy_config.config.Config {

    public WeaponsConfig() {
        super(Constants.tryId("weapons"));
    }

    public InvertedSpearSettings inverted_spear_of_heaven = new InvertedSpearSettings();
    public EaSettings ea = new EaSettings();
    public ElucidatorSettings elucidator = new ElucidatorSettings();
    public DarkRepulserSettings dark_repulser = new DarkRepulserSettings();
    public StarburstSettings starburst_stream = new StarburstSettings();
}
