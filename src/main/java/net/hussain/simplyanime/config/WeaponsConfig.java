package net.hussain.simplyanime.config;

import net.hussain.simplyanime.SimplyAnime;
import net.hussain.simplyanime.item.EnumaElishItem;
import net.hussain.simplyanime.item.InvertedSpearItem;
import net.minecraft.util.Identifier;

public class WeaponsConfig extends me.fzzyhmstrs.fzzy_config.config.Config {

    public WeaponsConfig() {
        super(new Identifier(SimplyAnime.MOD_ID, "weapons"));
    }

    public InvertedSpearItem.EffectSettings inverted_spear_of_heaven = new InvertedSpearItem.EffectSettings();
    public EnumaElishItem.EffectSettings enuma_elish = new EnumaElishItem.EffectSettings();
}
