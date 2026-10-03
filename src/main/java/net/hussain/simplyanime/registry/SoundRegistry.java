package net.hussain.simplyanime.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.hussain.simplyanime.SimplyAnime;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUND = DeferredRegister.create(SimplyAnime.MOD_ID, RegistryKeys.SOUND_EVENT);
    private static final Map<Identifier, RegistrySupplier<SoundEvent>> LAYERS = new HashMap<>();

    public static final RegistrySupplier<SoundEvent> HEAVEN_CHAIN_WINDUP = register("heaven_chain_windup");
    public static final RegistrySupplier<SoundEvent> HEAVEN_SPIN_SWOOSH = layered("heaven_spin_swoosh");
    public static final RegistrySupplier<SoundEvent> HEAVEN_PAYOFF_CLANG = layered("heaven_payoff_clang");
    public static final RegistrySupplier<SoundEvent> HEAVEN_CHAIN_RETRACT = register("heaven_chain_retract");
    public static final RegistrySupplier<SoundEvent> HEAVEN_DASH = layered("heaven_dash");
    public static final RegistrySupplier<SoundEvent> HEAVEN_STRIKE_HEAVY = layered("heaven_strike_heavy");
    public static final RegistrySupplier<SoundEvent> HEAVEN_STRIKE_QUICK = layered("heaven_strike_quick");

    public static final RegistrySupplier<SoundEvent> ENUMA_ELISH_CHARGE = register("enuma_elish_charge");
    public static final RegistrySupplier<SoundEvent> ENUMA_ELISH_RELEASE = register("enuma_elish_release");
    public static final RegistrySupplier<SoundEvent> ENUMA_ELISH_BEAM = register("enuma_elish_beam");
    public static final RegistrySupplier<SoundEvent> ENUMA_ELISH_IMPACT = register("enuma_elish_impact");

    // vanilla samples with a layer of our own on top
    private static RegistrySupplier<SoundEvent> layered(String name) {
        LAYERS.put(new Identifier(SimplyAnime.MOD_ID, name), register(name + "_layer"));
        return register(name);
    }

    @Nullable
    public static SoundEvent layer(SoundEvent sound) {
        RegistrySupplier<SoundEvent> layer = LAYERS.get(sound.getId());
        return layer == null ? null : layer.get();
    }

    private static RegistrySupplier<SoundEvent> register(String name) {
        return SOUND.register(name, () -> SoundEvent.of(new Identifier(SimplyAnime.MOD_ID, name)));
    }
}
