package mekanism.common.recipe.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import mekanism.api.JsonConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.api.recipes.FluidSlurryToSlurryRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient.SlurryStackIngredient;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.Mekanism;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class FluidSlurryToSlurryRecipeSerializer<RECIPE extends FluidSlurryToSlurryRecipe> implements RecipeSerializer<RECIPE> {

    private final IFactory<RECIPE> factory;

    public FluidSlurryToSlurryRecipeSerializer(IFactory<RECIPE> factory) {
        this.factory = factory;
    }

    @NotNull
    @Override
    public RECIPE fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
        JsonElement fluidInput = GsonHelper.isArrayNode(json, JsonConstants.FLUID_INPUT) ? GsonHelper.getAsJsonArray(json, JsonConstants.FLUID_INPUT) :
                                 GsonHelper.getAsJsonObject(json, JsonConstants.FLUID_INPUT);
        FluidStackIngredient fluidIngredient = IngredientCreatorAccess.fluid().deserialize(fluidInput);
        JsonElement slurryInput = GsonHelper.isArrayNode(json, JsonConstants.SLURRY_INPUT) ? GsonHelper.getAsJsonArray(json, JsonConstants.SLURRY_INPUT) :
                                  GsonHelper.getAsJsonObject(json, JsonConstants.SLURRY_INPUT);
        SlurryStackIngredient slurryIngredient = IngredientCreatorAccess.slurry().deserialize(slurryInput);
        SlurryStack output = SerializerHelper.getSlurryStack(json, JsonConstants.OUTPUT);
        if (output.isEmpty()) {
            throw new JsonSyntaxException("Recipe output must not be empty.");
        }
        return this.factory.create(recipeId, fluidIngredient, slurryIngredient, output);
    }

    @Override
    public RECIPE fromNetwork(@NotNull FriendlyByteBuf buffer) {
        try {
            FluidStackIngredient fluidInput = IngredientCreatorAccess.fluid().read(buffer);
            SlurryStackIngredient slurryInput = IngredientCreatorAccess.slurry().read(buffer);
            SlurryStack output = SlurryStack.readFromPacket(buffer);
            return this.factory.create(fluidInput, slurryInput, output);
        } catch (Exception e) {
            Mekanism.logger.error("Error reading fluid slurry to slurry recipe from packet.", e);
            throw e;
        }
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull RECIPE recipe) {
        try {
            recipe.write(buffer);
        } catch (Exception e) {
            Mekanism.logger.error("Error writing fluid slurry to slurry recipe to packet.", e);
            throw e;
        }
    }

    @FunctionalInterface
    public interface IFactory<RECIPE extends FluidSlurryToSlurryRecipe> {

        RECIPE create(FluidStackIngredient fluidInput, SlurryStackIngredient slurryInput, SlurryStack output);
    }
}