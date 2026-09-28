package net.hussain.simplyanime.config;

import com.google.gson.JsonObject;
import net.hussain.simplyanime.SimplyAnime;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

// lets a recipe be switched off from the config: "type": "simplyanime:craftable", "weapon": "enuma_elish"
public record RecipeToggleCondition(String weapon) implements ICondition {

    private static final Identifier ID = new Identifier(SimplyAnime.MOD_ID, "craftable");

    public static void register() {
        CraftingHelper.register(Serializer.INSTANCE);
    }

    @Override
    public Identifier getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return switch (this.weapon) {
            case "enuma_elish" -> Config.weapons.enuma_elish.craftable;
            default -> true;
        };
    }

    public static final class Serializer implements IConditionSerializer<RecipeToggleCondition> {

        static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, RecipeToggleCondition value) {
            json.addProperty("weapon", value.weapon());
        }

        @Override
        public RecipeToggleCondition read(JsonObject json) {
            return new RecipeToggleCondition(JsonHelper.getString(json, "weapon"));
        }

        @Override
        public Identifier getID() {
            return ID;
        }
    }
}
