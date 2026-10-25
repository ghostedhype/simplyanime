package net.ghostedhype.simplyanime.registry;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar;
import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier;
import net.ghostedhype.simplyanime.Constants;
import net.ghostedhype.simplyanime.config.Config;
import net.ghostedhype.simplyanime.config.RhittaConfig;
import net.ghostedhype.simplyanime.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public class ItemsRegistry {

    private static final Registrar<Item> ITEMS = ConfigApiJava.platform().createRegistrar(Constants.MOD_ID, BuiltInRegistries.ITEM);

    public static final RegistrySupplier<Item> DARK_REPULSER = ITEMS.register("dark_repulser", () ->
            new DarkRepulserItem(SimplyAnimeTiers.SIMPLY_ANIME_UNIQUE, weaponProperties(SimplyAnimeTiers.SIMPLY_ANIME_UNIQUE,
                    Config.weapons.dark_repulser.damageModifier, Config.weapons.dark_repulser.attackSpeed)));

    public static final RegistrySupplier<Item> DIVINE_AXE_RHITTA = ITEMS.register("divine_axe_rhitta", () ->
            new DivineAxeRhittaItem(SimplyAnimeTiers.SIMPLY_ANIME_UNIQUE, weaponProperties(SimplyAnimeTiers.SIMPLY_ANIME_UNIQUE,
                    RhittaConfig.get().axe.damageModifier, RhittaConfig.get().axe.attackSpeed)));

    public static final RegistrySupplier<Item> ELUCIDATOR = ITEMS.register("elucidator", () ->
            new ElucidatorItem(SimplyAnimeTiers.SIMPLY_ANIME_UNIQUE, weaponProperties(SimplyAnimeTiers.SIMPLY_ANIME_UNIQUE,
                    Config.weapons.elucidator.damageModifier, Config.weapons.elucidator.attackSpeed)));

    public static final RegistrySupplier<Item> EA = ITEMS.register("ea", () ->
            new EaItem(SimplyAnimeTiers.SIMPLY_ANIME_UNIQUE, weaponProperties(SimplyAnimeTiers.SIMPLY_ANIME_UNIQUE,
                    Config.weapons.ea.damageModifier, Config.weapons.ea.attackSpeed)));

    public static final RegistrySupplier<Item> INVERTED_SPEAR_OF_HEAVEN = ITEMS.register("inverted_spear_of_heaven", () ->
            new InvertedSpearItem(SimplyAnimeTiers.SIMPLY_ANIME_UNIQUE, weaponProperties(SimplyAnimeTiers.SIMPLY_ANIME_UNIQUE,
                    Config.weapons.inverted_spear_of_heaven.damageModifier, Config.weapons.inverted_spear_of_heaven.attackSpeed)));

    // only the creative tab icon, not part of any tab
    public static final RegistrySupplier<Item> ICON = ITEMS.register("icon", () -> new Item(new Item.Properties()));

    public static void init() {
        ITEMS.init();
    }

    private static Item.Properties weaponProperties(Tier tier, float damageModifier, float attackSpeed) {
        return new Item.Properties().fireResistant().attributes(SwordItem.createAttributes(tier, (int) damageModifier, attackSpeed));
    }
}
