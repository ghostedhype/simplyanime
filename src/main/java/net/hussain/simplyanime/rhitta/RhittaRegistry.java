package net.hussain.simplyanime.rhitta;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.hussain.simplyanime.SimplyAnime;
import net.hussain.simplyanime.rhitta.config.RhittaConfig;
import net.hussain.simplyanime.rhitta.entity.CruelSunEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.LegacyWeaponAttributes;
import net.sweenus.simplyswords.item.ModToolMaterial;

public class RhittaRegistry {

    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(SimplyAnime.MOD_ID, RegistryKeys.ITEM);
    public static final DeferredRegister<SoundEvent> SOUND = DeferredRegister.create(SimplyAnime.MOD_ID, RegistryKeys.SOUND_EVENT);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(SimplyAnime.MOD_ID, RegistryKeys.ENTITY_TYPE);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(SimplyAnime.MOD_ID, RegistryKeys.PARTICLE_TYPE);

    public static final RegistrySupplier<DivineAxeRhittaItem> DIVINE_AXE_RHITTA = ITEM.register("divine_axe_rhitta", () ->
            new DivineAxeRhittaItem(
                    ModToolMaterial.UNIQUE,
                    LegacyWeaponAttributes.configure(new Item.Settings().fireproof(),
                            (int) RhittaConfig.get().axe.damageModifier, RhittaConfig.get().axe.attackSpeed)));

    public static final RegistrySupplier<SoundEvent> BUILD_UP = sound("rhitta_build_up");
    public static final RegistrySupplier<SoundEvent> LAUNCH = sound("rhitta_launch");
    public static final RegistrySupplier<SoundEvent> FLIGHT = sound("rhitta_flight");
    public static final RegistrySupplier<SoundEvent> BLAST = sound("rhitta_blast");
    public static final RegistrySupplier<SoundEvent> BLAST_ROAR = sound("rhitta_blast_roar");

    public static final RegistrySupplier<EntityType<CruelSunEntity>> CRUEL_SUN = ENTITIES.register("cruel_sun",
            () -> EntityType.Builder.<CruelSunEntity>create(CruelSunEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .maxTrackingRange(12)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplyAnime.MOD_ID, "cruel_sun").toString()));

    public static final RegistrySupplier<DefaultParticleType> SUN_FLAME = PARTICLES.register("sun_flame",
            () -> new DefaultParticleType(true) {});
    public static final RegistrySupplier<DefaultParticleType> SUN_EMBER = PARTICLES.register("sun_ember",
            () -> new DefaultParticleType(true) {});
    public static final RegistrySupplier<DefaultParticleType> SUN_SPARK = PARTICLES.register("sun_spark",
            () -> new DefaultParticleType(true) {});
    public static final RegistrySupplier<DefaultParticleType> SUN_WISP = PARTICLES.register("sun_wisp",
            () -> new DefaultParticleType(true) {});

    public static void register() {
        ITEM.register();
        SOUND.register();
        ENTITIES.register();
        PARTICLES.register();
        CreativeTabRegistry.append(SimplySwords.SIMPLYSWORDS, DIVINE_AXE_RHITTA);
    }

    private static RegistrySupplier<SoundEvent> sound(String name) {
        return SOUND.register(name, () -> SoundEvent.of(new Identifier(SimplyAnime.MOD_ID, name)));
    }
}
