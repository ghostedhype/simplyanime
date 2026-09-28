package net.hussain.simplyanime.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.hussain.simplyanime.SimplyAnime;
import net.hussain.simplyanime.entity.EnumaElishVisualEntity;
import net.hussain.simplyanime.entity.HeavenChainVisualEntity;
import net.hussain.simplyanime.entity.HeavenMarkVisualEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(SimplyAnime.MOD_ID, RegistryKeys.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<HeavenChainVisualEntity>> HEAVEN_CHAIN_VISUAL = ENTITIES.register(
            "heaven_chain_visual",
            () -> EntityType.Builder.<HeavenChainVisualEntity>create(HeavenChainVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35F, 0.35F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplyAnime.MOD_ID, "heaven_chain_visual").toString())
    );

    public static final RegistrySupplier<EntityType<HeavenMarkVisualEntity>> HEAVEN_MARK_VISUAL = ENTITIES.register(
            "heaven_mark_visual",
            () -> EntityType.Builder.<HeavenMarkVisualEntity>create(HeavenMarkVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35F, 0.35F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplyAnime.MOD_ID, "heaven_mark_visual").toString())
    );

    public static final RegistrySupplier<EntityType<EnumaElishVisualEntity>> ENUMA_ELISH_VISUAL = ENTITIES.register(
            "enuma_elish_visual",
            () -> EntityType.Builder.<EnumaElishVisualEntity>create(EnumaElishVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35F, 0.35F)
                    .maxTrackingRange(16)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplyAnime.MOD_ID, "enuma_elish_visual").toString())
    );
}
