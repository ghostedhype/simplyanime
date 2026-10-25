package net.ghostedhype.simplyanime.registry;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar;
import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier;
import net.ghostedhype.simplyanime.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SoundRegistry {

    private static final Registrar<SoundEvent> SOUNDS =
            ConfigApiJava.platform().createRegistrar(Constants.MOD_ID, BuiltInRegistries.SOUND_EVENT);
    private static final Map<ResourceLocation, RegistrySupplier<SoundEvent>> LAYERS = new HashMap<>();

    public static final RegistrySupplier<SoundEvent> HEAVEN_CHAIN_WINDUP = register("heaven_chain_windup");
    public static final RegistrySupplier<SoundEvent> HEAVEN_SPIN_SWOOSH = layered("heaven_spin_swoosh");
    public static final RegistrySupplier<SoundEvent> HEAVEN_PAYOFF_CLANG = layered("heaven_payoff_clang");
    public static final RegistrySupplier<SoundEvent> HEAVEN_CHAIN_RETRACT = register("heaven_chain_retract");
    public static final RegistrySupplier<SoundEvent> HEAVEN_DASH = layered("heaven_dash");
    public static final RegistrySupplier<SoundEvent> HEAVEN_STRIKE_HEAVY = layered("heaven_strike_heavy");
    public static final RegistrySupplier<SoundEvent> HEAVEN_STRIKE_QUICK = layered("heaven_strike_quick");

    public static final RegistrySupplier<SoundEvent> EA_CHARGE = register("ea_charge");
    public static final RegistrySupplier<SoundEvent> EA_RELEASE = register("ea_release");
    public static final RegistrySupplier<SoundEvent> EA_BEAM = register("ea_beam");
    public static final RegistrySupplier<SoundEvent> EA_IMPACT = register("ea_impact");

    public static final RegistrySupplier<SoundEvent> RHITTA_BUILD_UP = register("rhitta_build_up");
    public static final RegistrySupplier<SoundEvent> RHITTA_LAUNCH = register("rhitta_launch");
    public static final RegistrySupplier<SoundEvent> RHITTA_FLIGHT = register("rhitta_flight");
    public static final RegistrySupplier<SoundEvent> RHITTA_BLAST = register("rhitta_blast");
    public static final RegistrySupplier<SoundEvent> RHITTA_BLAST_ROAR = register("rhitta_blast_roar");

    public static final RegistrySupplier<SoundEvent> VORPAL_CHARGE = register("vorpal_charge");
    public static final RegistrySupplier<SoundEvent> VORPAL_STRIKE = register("vorpal_strike");
    public static final RegistrySupplier<SoundEvent> SQUARE_SLASH = register("square_slash");
    public static final RegistrySupplier<SoundEvent> SQUARE_RELEASE = register("square_release");
    public static final RegistrySupplier<SoundEvent> STARBURST_STANCE = register("starburst_stance");
    public static final RegistrySupplier<SoundEvent> STARBURST_SLASH_DARK = register("starburst_slash_dark");
    public static final RegistrySupplier<SoundEvent> STARBURST_SLASH_LIGHT = register("starburst_slash_light");
    public static final RegistrySupplier<SoundEvent> STARBURST_COIL = register("starburst_coil");
    public static final RegistrySupplier<SoundEvent> STARBURST_FINALE = register("starburst_finale");
    public static final RegistrySupplier<SoundEvent> BLADE_HIT = register("blade_hit");

    public static void init() {
        SOUNDS.init();
    }

    // vanilla samples with a layer of our own on top
    private static RegistrySupplier<SoundEvent> layered(String name) {
        LAYERS.put(Constants.tryId(name), register(name + "_layer"));
        return register(name);
    }

    @Nullable
    public static SoundEvent layer(SoundEvent sound) {
        RegistrySupplier<SoundEvent> layer = LAYERS.get(sound.getLocation());
        return layer == null ? null : layer.get();
    }

    private static RegistrySupplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Constants.tryId(name)));
    }
}
