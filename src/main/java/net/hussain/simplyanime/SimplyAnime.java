package net.hussain.simplyanime;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.forge.EventBuses;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.hussain.simplyanime.client.SimplyAnimeClient;
import net.hussain.simplyanime.command.SimplyAnimeCommand;
import net.hussain.simplyanime.config.Config;
import net.hussain.simplyanime.config.RecipeToggleCondition;
import net.hussain.simplyanime.registry.EntityRegistry;
import net.hussain.simplyanime.registry.ItemsRegistry;
import net.hussain.simplyanime.registry.ParticlesRegistry;
import net.hussain.simplyanime.registry.SoundRegistry;
import net.hussain.simplyanime.world.EnumaElishAbilityManager;
import net.hussain.simplyanime.world.InvertedSpearAbilityManager;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;

@Mod(SimplyAnime.MOD_ID)
public class SimplyAnime {

    public static final String MOD_ID = "simplyanime";

    public SimplyAnime() {
        EventBuses.registerModEventBus(MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        Config.init();
        ItemsRegistry.ITEM.register();
        SoundRegistry.SOUND.register();
        EntityRegistry.ENTITIES.register();
        ParticlesRegistry.PARTICLES.register();
        InvertedSpearAbilityManager.init();
        EnumaElishAbilityManager.init();
        RecipeToggleCondition.register();
        SimplyAnimeCommand.init();

        LifecycleEvent.SETUP.register(() -> {
            SimplySwordsAPI.registerUniqueLoot(ItemsRegistry.INVERTED_SPEAR_OF_HEAVEN.get(), 1);
            SimplySwordsAPI.registerWeaponType(ItemsRegistry.INVERTED_SPEAR_OF_HEAVEN.get(),
                    new Identifier("simplyswords", "sai"));
            SimplySwordsAPI.registerTransformation(Blocks.CHAIN,
                    new Identifier(MOD_ID, "inverted_spear_of_heaven"));

            // also craftable, see data/simplyanime/recipes/enuma_elish.json
            if (Config.weapons.enuma_elish.lootable) {
                SimplySwordsAPI.registerUniqueLoot(ItemsRegistry.ENUMA_ELISH.get(), 1);
            }
            SimplySwordsAPI.registerWeaponType(ItemsRegistry.ENUMA_ELISH.get(),
                    new Identifier("simplyswords", "claymore"));
        });

        EnvExecutor.runInEnv(Env.CLIENT, () -> SimplyAnimeClient::init);
    }
}
