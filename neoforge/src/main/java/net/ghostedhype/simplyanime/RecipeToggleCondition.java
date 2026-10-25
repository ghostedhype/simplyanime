package net.ghostedhype.simplyanime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ghostedhype.simplyanime.config.Config;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.NotNull;

public record RecipeToggleCondition(String weapon) implements ICondition {

    public static final MapCodec<RecipeToggleCondition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.STRING.fieldOf("weapon").forGetter(RecipeToggleCondition::weapon))
                    .apply(instance, RecipeToggleCondition::new));

    @Override
    public boolean test(@NotNull IContext context) {
        return switch (this.weapon) {
            case "ea" -> Config.weapons.ea.craftable;
            default -> true;
        };
    }

    @Override
    public @NotNull MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
