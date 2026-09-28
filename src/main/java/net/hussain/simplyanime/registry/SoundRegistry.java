package net.hussain.simplyanime.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.hussain.simplyanime.SimplyAnime;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUND = DeferredRegister.create(SimplyAnime.MOD_ID, RegistryKeys.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> HEAVEN_CHAIN_WINDUP = register("heaven_chain_windup");
    public static final RegistrySupplier<SoundEvent> HEAVEN_SPIN_SWOOSH = register("heaven_spin_swoosh");
    public static final RegistrySupplier<SoundEvent> HEAVEN_PAYOFF_CLANG = register("heaven_payoff_clang");
    public static final RegistrySupplier<SoundEvent> HEAVEN_CHAIN_RETRACT = register("heaven_chain_retract");
    public static final RegistrySupplier<SoundEvent> HEAVEN_DASH = register("heaven_dash");
    public static final RegistrySupplier<SoundEvent> HEAVEN_STRIKE_HEAVY = register("heaven_strike_heavy");
    public static final RegistrySupplier<SoundEvent> HEAVEN_STRIKE_QUICK = register("heaven_strike_quick");

    public static final RegistrySupplier<SoundEvent> ENUMA_ELISH_CHARGE = register("enuma_elish_charge");
    public static final RegistrySupplier<SoundEvent> ENUMA_ELISH_SHOUT = register("enuma_elish_shout");
    public static final RegistrySupplier<SoundEvent> ENUMA_ELISH_RELEASE = register("enuma_elish_release");
    public static final RegistrySupplier<SoundEvent> ENUMA_ELISH_BEAM = register("enuma_elish_beam");
    public static final RegistrySupplier<SoundEvent> ENUMA_ELISH_IMPACT = register("enuma_elish_impact");

    private static RegistrySupplier<SoundEvent> register(String name) {
        return SOUND.register(name, () -> SoundEvent.of(new Identifier(SimplyAnime.MOD_ID, name)));
    }
}
