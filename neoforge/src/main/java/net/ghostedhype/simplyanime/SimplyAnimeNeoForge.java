package net.ghostedhype.simplyanime;

import com.mojang.serialization.MapCodec;
import net.ghostedhype.simplyanime.ability.RhittaGrowth;
import net.ghostedhype.simplyanime.command.SimplyAnimeCommand;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(Constants.MOD_ID)
public class SimplyAnimeNeoForge {

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Constants.MOD_ID);

    public SimplyAnimeNeoForge(IEventBus modEventBus) {
        SimplyAnimeCommon.initialize();
        CONDITIONS.register("craftable", () -> RecipeToggleCondition.CODEC);
        CONDITIONS.register(modEventBus);

        modEventBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(SimplyAnimeCommon::setup));

        IEventBus gameEventBus = NeoForge.EVENT_BUS;
        gameEventBus.addListener(SimplyAnimeNeoForge::onLevelTick);
        gameEventBus.addListener(SimplyAnimeNeoForge::onLevelUnload);
        gameEventBus.addListener((ServerStoppedEvent event) -> CommonEvents.serverStopped());
        gameEventBus.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> CommonEvents.playerGone(event.getEntity()));
        gameEventBus.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> CommonEvents.playerGone(event.getEntity()));
        gameEventBus.addListener(SimplyAnimeNeoForge::onDamage);
        gameEventBus.addListener(SimplyAnimeNeoForge::onAttack);
        gameEventBus.addListener((PlayerTickEvent.Post event) -> RhittaGrowth.tick(event.getEntity()));
        gameEventBus.addListener(SimplyAnimeNeoForge::onSize);
        gameEventBus.addListener((RegisterCommandsEvent event) -> SimplyAnimeCommand.register(event.getDispatcher()));
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            CommonEvents.levelTick(level);
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            CommonEvents.levelUnload(level);
        }
    }

    private static void onDamage(LivingIncomingDamageEvent event) {
        if (CommonEvents.blocksDamage(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(CommonEvents.scaleDamage(event.getEntity(), event.getSource(), event.getAmount()));
    }

    private static void onAttack(AttackEntityEvent event) {
        if (CommonEvents.blocksAttack(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void onSize(EntityEvent.Size event) {
        float scale = RhittaGrowth.sizeScale(event.getEntity());
        if (scale != 1.0F) {
            event.setNewSize(event.getNewSize().scale(scale));
        }
    }
}
