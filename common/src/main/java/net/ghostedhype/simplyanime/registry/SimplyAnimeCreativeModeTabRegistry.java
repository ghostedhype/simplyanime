package net.ghostedhype.simplyanime.registry;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar;
import net.ghostedhype.simplyanime.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class SimplyAnimeCreativeModeTabRegistry {
    private static final Registrar<CreativeModeTab> CREATIVE_MODE_TAB =
            ConfigApiJava.platform().createRegistrar(Constants.MOD_ID, BuiltInRegistries.CREATIVE_MODE_TAB);

    public static void register() {
        CREATIVE_MODE_TAB.init();

        CREATIVE_MODE_TAB.register("weapons", () -> CreativeModeTab.builder(null, -1)
                .title(Component.translatable("itemGroup.simplyanime.weapons"))
                .icon(() -> new ItemStack(ItemsRegistry.ICON.get()))
                .displayItems(((itemDisplayParameters, output) -> {
                    output.accept(ItemsRegistry.DARK_REPULSER.get());
                    output.accept(ItemsRegistry.DIVINE_AXE_RHITTA.get());
                    output.accept(ItemsRegistry.ELUCIDATOR.get());
                    output.accept(ItemsRegistry.EA.get());
                    output.accept(ItemsRegistry.INVERTED_SPEAR_OF_HEAVEN.get());
                }))
                .build());
    }
}
