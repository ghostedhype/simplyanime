package net.hussain.simplyanime.autotest;

import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

// flat test world, a husk casts each ability at a golem, screenshots go to <pack>_<effect>_<tick>.png, then it quits
public class Autotest {

    private static final Logger LOG = LogUtils.getLogger();
    private static final int GROUND = -60;
    private static final String SHADERS = System.getProperty("simplyanime.autotest.shaders", "");
    private static final String ONLY = System.getProperty("simplyanime.autotest.effect", "");
    private static final String TIME = System.getProperty("simplyanime.autotest.time", "13200");

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static int ticks;
    private static int wait;
    private static boolean worldRequested;
    private static boolean scripted;

    private record Step(int delay, Runnable action) {
    }

    public static void tick(MinecraftClient mc) {
        ticks++;
        if (!worldRequested) {
            if (ticks > 60 && mc.world == null) {
                worldRequested = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                mc.options.getGamma().setValue(0.5);
                mc.options.getViewDistance().setValue(12);
                mc.options.getSoundVolumeOption(SoundCategory.MASTER).setValue(0.0);
                mc.options.onboardAccessibility = false;
                mc.options.write();
                createWorld(mc);
            }
            return;
        }
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (!scripted) {
            scripted = true;
            script(mc);
            wait = 100;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = STEPS.poll();
        if (step == null) {
            return;
        }
        step.action().run();
        Step next = STEPS.peek();
        wait = next == null ? 0 : next.delay();
    }

    private static void createWorld(MinecraftClient mc) {
        LOG.info("[autotest] creating world");
        LevelInfo info = new LevelInfo("autotest", GameMode.CREATIVE, false, Difficulty.NORMAL, true,
                new GameRules(), DataConfiguration.SAFE_MODE);
        mc.createIntegratedServerLoader().createAndStart("autotest", info, new GeneratorOptions(4317L, false, false),
                registries -> registries.get(RegistryKeys.WORLD_PRESET).entryOf(WorldPresets.FLAT).value().createDimensionsRegistryHolder());
    }

    private static void cmd(String command) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            // stay in the air, otherwise the camera drops after every tp
            mc.player.getAbilities().flying = true;
            mc.player.sendAbilitiesUpdate();
            mc.player.networkHandler.sendChatCommand(command);
        }
    }

    private static void add(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static void script(MinecraftClient mc) {
        add(0, () -> {
            cmd("gamerule doDaylightCycle false");
            cmd("gamerule doMobSpawning false");
            cmd("gamerule doWeatherCycle false");
            cmd("gamerule mobGriefing false");
            cmd("time set " + TIME);
            cmd("weather clear");
            mc.options.hudHidden = true;
        });
        List<String> packs = new ArrayList<>();
        packs.add("none");
        File[] files = new File(mc.runDirectory, "shaderpacks").listFiles((d, n) -> n.endsWith(".zip"));
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                if (wantedPack(f.getName())) {
                    packs.add(f.getName());
                }
            }
        }
        for (String pack : packs) {
            String tag = pack.replace(".zip", "").replaceAll("[^A-Za-z0-9]", "");
            add(10, () -> setPack(pack));
            // shader compile time
            add(pack.equals("none") ? 40 : 200, () -> LOG.info("[autotest] pack ready: {}", pack));
            if (wanted("sun")) {
                sun(mc, tag);
            }
            if (wanted("ea")) {
                ea(mc, tag);
            }
            if (wanted("field")) {
                spear(mc, tag, "field", 4, new int[]{4, 14, 28, 46, 57, 62, 68}, "tp @s 7 " + (GROUND + 4) + " 6 facing 0 " + (GROUND + 1) + " -2");
            }
            if (wanted("strike")) {
                spear(mc, tag, "strike", 9, new int[]{4, 8, 11, 14, 18, 23}, "tp @s 9 " + (GROUND + 4) + " 3 facing 0 " + (GROUND + 1) + " -5");
            }
        }
        add(20, () -> {
            setPack("none");
            LOG.info("[autotest] done");
            mc.scheduleStop();
        });
    }

    // "all" or parts of pack names, comma separated
    private static boolean wantedPack(String file) {
        if (SHADERS.equals("all")) {
            return true;
        }
        for (String part : SHADERS.toLowerCase().split(",")) {
            if (!part.isBlank() && file.toLowerCase().contains(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean wanted(String effect) {
        return ONLY.isEmpty() || ONLY.contains(effect);
    }

    private static void scene(String weapon, int distance, String camera) {
        add(5, () -> {
            cmd("kill @e[type=!player]");
            cmd("summon minecraft:iron_golem 0 " + GROUND + " -" + distance + " {NoAI:1b,PersistenceRequired:1b,Health:100f}");
            cmd("summon minecraft:husk 0 " + GROUND + " 0 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f],HandItems:[{id:\"simplyanime:"
                    + weapon + "\",Count:1b},{}]}");
            cmd(camera);
        });
        add(20, () -> {
            cmd("kill @e[type=item]");
            cmd("effect give @e[type=minecraft:iron_golem] minecraft:resistance infinite 4 true");
        });
    }

    private static void shots(MinecraftClient mc, String tag, String effect, int[] at) {
        int last = 0;
        for (int t : at) {
            String name = tag + "_" + effect + "_" + String.format("%03d", t) + ".png";
            add(t - last, () -> ScreenshotRecorder.saveScreenshot(mc.runDirectory, name, mc.getFramebuffer(), msg -> {
            }));
            last = t;
        }
        add(40, () -> {
        });
    }

    private static void sun(MinecraftClient mc, String tag) {
        scene("divine_axe_rhitta", 34, "tp @s 46 " + (GROUND + 12) + " -14 facing 0 " + (GROUND + 9) + " -14");
        add(2, () -> cmd("execute as @e[type=minecraft:husk,limit=1] run satest sun"));
        shots(mc, tag, "sun", new int[]{12, 24, 40, 70, 116, 128, 146, 154, 166, 184, 204});
    }

    private static void ea(MinecraftClient mc, String tag) {
        scene("enuma_elish", 40, "tp @s 30 " + (GROUND + 9) + " 6 facing 0 " + (GROUND + 4) + " -16");
        add(2, () -> cmd("execute as @e[type=minecraft:husk,limit=1] run satest ea"));
        shots(mc, tag, "ea", new int[]{30, 70, 86, 92, 120, 170, 203, 212});
    }

    private static void spear(MinecraftClient mc, String tag, String effect, int distance, int[] at, String camera) {
        scene("inverted_spear_of_heaven", distance, camera);
        add(2, () -> cmd("execute as @e[type=minecraft:husk,limit=1] run satest " + effect));
        shots(mc, tag, effect, at);
    }

    private static void setPack(String pack) {
        for (String type : new String[]{"net.irisshaders.iris.Iris", "net.coderbot.iris.Iris"}) {
            try {
                Class<?> iris = Class.forName(type);
                Object config = iris.getMethod("getIrisConfig").invoke(null);
                boolean on = !pack.equals("none");
                if (on) {
                    config.getClass().getMethod("setShaderPackName", String.class).invoke(config, pack);
                }
                config.getClass().getMethod("setShadersEnabled", boolean.class).invoke(config, on);
                config.getClass().getMethod("save").invoke(config);
                iris.getMethod("reload").invoke(null);
                LOG.info("[autotest] switched shaders to {}", pack);
                return;
            } catch (ClassNotFoundException e) {
                continue;
            } catch (Throwable e) {
                LOG.error("[autotest] could not switch shader pack to {}: {}", pack, e.toString());
                return;
            }
        }
    }
}
