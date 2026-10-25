package net.ghostedhype.simplyanime.registry;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar;
import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier;
import net.ghostedhype.simplyanime.Constants;
import net.ghostedhype.simplyanime.entity.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public class EntityRegistry {

    private static final Registrar<EntityType<?>> ENTITIES =
            ConfigApiJava.platform().createRegistrar(Constants.MOD_ID, BuiltInRegistries.ENTITY_TYPE);

    public static final Supplier<EntityType<HeavenChainVisualEntity>> HEAVEN_CHAIN_VISUAL =
            registerEntityType("heaven_chain_visual", HeavenChainVisualEntity::new, 0.35F, 96);
    public static final Supplier<EntityType<HeavenMarkVisualEntity>> HEAVEN_MARK_VISUAL =
            registerEntityType("heaven_mark_visual", HeavenMarkVisualEntity::new, 0.35F, 96);
    public static final Supplier<EntityType<EaVisualEntity>> EA_VISUAL =
            registerEntityType("ea_visual", EaVisualEntity::new, 0.35F, 16);
    public static final Supplier<EntityType<CruelSunEntity>> CRUEL_SUN =
            registerEntityType("cruel_sun", CruelSunEntity::new, 0.5F, 12);
    public static final Supplier<EntityType<BladeSkillEntity>> BLADE_SKILL =
            registerEntityType("blade_skill", BladeSkillEntity::new, 0.35F, 16);

    public static void init() {
        ENTITIES.init();
    }

    protected static <T extends Entity> RegistrySupplier<EntityType<T>> registerEntityType(String name, EntityType.EntityFactory<T> factory, float size, int range) {
        return ENTITIES.register(name, () ->
                EntityType.Builder.of(factory, MobCategory.MISC)
                        .sized(size, size)
                        .clientTrackingRange(range)
                        .updateInterval(1)
                        .build(Constants.tryId(name).toString()));
    }
}
