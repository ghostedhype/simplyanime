package net.hussain.simplyanime.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.hussain.simplyanime.SimplyAnime;
import net.hussain.simplyanime.config.Config;
import net.hussain.simplyanime.item.EnumaElishItem;
import net.hussain.simplyanime.item.InvertedSpearItem;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.LegacyWeaponAttributes;
import net.sweenus.simplyswords.item.ModToolMaterial;

public class ItemsRegistry {

    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(SimplyAnime.MOD_ID, RegistryKeys.ITEM);

    static float inverted_spear_damage_modifier = Config.weapons.inverted_spear_of_heaven.damageModifier;
    static float inverted_spear_attackspeed = Config.weapons.inverted_spear_of_heaven.attackSpeed;

    public static final RegistrySupplier<InvertedSpearItem> INVERTED_SPEAR_OF_HEAVEN = ITEM.register("inverted_spear_of_heaven", () ->
            new InvertedSpearItem(
                    ModToolMaterial.UNIQUE,
                    LegacyWeaponAttributes.configure(new Item.Settings().fireproof()
                            , (int) inverted_spear_damage_modifier, inverted_spear_attackspeed)));

    static float enuma_elish_damage_modifier = Config.weapons.enuma_elish.damageModifier;
    static float enuma_elish_attackspeed = Config.weapons.enuma_elish.attackSpeed;

    public static final RegistrySupplier<EnumaElishItem> ENUMA_ELISH = ITEM.register("enuma_elish", () ->
            new EnumaElishItem(
                    ModToolMaterial.UNIQUE,
                    LegacyWeaponAttributes.configure(new Item.Settings().fireproof()
                            , (int) enuma_elish_damage_modifier, enuma_elish_attackspeed)));

    static {
        CreativeTabRegistry.append(SimplySwords.SIMPLYSWORDS, INVERTED_SPEAR_OF_HEAVEN);
        CreativeTabRegistry.append(SimplySwords.SIMPLYSWORDS, ENUMA_ELISH);
    }
}
