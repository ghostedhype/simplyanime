package net.ghostedhype.simplyanime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.ghostedhype.simplyanime.config.Config;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record RecipeToggleCondition(String weapon) implements ResourceCondition {

    public static final MapCodec<RecipeToggleCondition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.STRING.fieldOf("weapon").forGetter(RecipeToggleCondition::weapon))
                    .apply(instance, RecipeToggleCondition::new));

    public static final ResourceConditionType<RecipeToggleCondition> TYPE =
            ResourceConditionType.create(Constants.tryId("craftable"), CODEC);

    @Override
    public @NotNull ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(@Nullable HolderLookup.Provider registryLookup) {
        return switch (this.weapon) {
            case "ea" -> Config.weapons.ea.craftable;
            default -> true;
        };
    }
}
