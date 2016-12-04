package com.recipewizard.recipewizard;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brian on 12/4/2016.
 */

public class RecipeToFile {
    private static final String FILE_NAME = "FavoritesListManagerActivityData.txt";
    private static final String TAG ="RecipeToFile";

    private Recipe recipe;
    private Context context;
    public RecipeToFile (Recipe recipe, Context context){
        this.recipe = recipe;
        this.context = context;
    }
    private void saveImage(Bitmap image, String itemName){
        String filename = itemName + ".png";
        FileOutputStream out = null;
        File root = Environment.getExternalStorageDirectory();
        File file = new File(root, filename);
        Log.d(TAG, "Saving: " + file.toString());
        if(image != null && !file.exists()){
            try {
                out = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.PNG,100, out);
                out.flush();
                out.close();
                Log.d(TAG, "Finished Saving: " + out.toString());
            } catch(FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally{
                try{
                    if (out != null)
                        out.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
    private Bitmap loadImage(String itemName){
        String filename = itemName + ".png";
        String fullPath = Environment.getExternalStorageDirectory()+"/" + filename;
        Log.d(TAG,"Loading: " + fullPath);
        return BitmapFactory.decodeFile(fullPath);
    }

    private boolean[] getAllergyInformationFromLines (String line){
        String allergiesArray[];
        boolean allergyInformation [] = new boolean [Recipe.NUM_ALLERGIES];
        int i = 0;
        line = line.split("\\[")[1];
        line = line.split("]")[0];
        allergiesArray = line.split(",");

        for(String s : allergiesArray){
            allergyInformation[i] = Boolean.parseBoolean(s);
            i++;
        }
        return allergyInformation;
    }

    private List<Step> getStepsFromLine(String line){
        String stepsArray[], singleStepArray[], singleIngredientArray[] ;
        List<Step> steps = new ArrayList<>();
        List<String> equipment = new ArrayList<>();
        Bitmap picture = null;
        String equipmentString [];
        String name;
        String direction;
        boolean checked;
        String category;
        int amount;
        String unit;

        stepsArray = line.split(";");
        for(String s : stepsArray) {
            singleStepArray = s.split(".");
            List<Ingredient> ingredients = new ArrayList<>();
            if (singleStepArray.length > 0) {
                direction = singleStepArray[0];
                for (int i = 1; i < singleStepArray.length - 1; i++) {
                    singleIngredientArray = singleStepArray[i].split(",");
                    name = singleIngredientArray[0];
                    picture = loadImage(name);
                    checked = Boolean.parseBoolean(singleIngredientArray[1]);
                    category = singleIngredientArray[2];
                    amount = Integer.parseInt(singleIngredientArray[3]);
                    unit = singleIngredientArray[4];
                    ingredients.add(new Ingredient(name, picture, checked, category, amount, unit));
                }
                equipmentString = singleStepArray[singleStepArray.length -1].split(",");
                if(equipmentString.length > 0);
                for(String e : equipmentString) {
                    equipment.add(e);
                }

                steps.add(new Step(direction, ingredients, equipment));
            }
        }
        return steps;
    }
    private List<Recipe> getFavoritesListFromFile(){
        BufferedReader reader = null;
        List<Recipe> favorites = new ArrayList<>();
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            reader = new BufferedReader(new InputStreamReader(fis));

            Bitmap picture = null;
            String id;
            String name;
            String stepsString;
            String author;
            List<Step> steps = new ArrayList<>();
            String calorieInformationLine [];
            int calories;
            int protein;
            int carbs;
            int fat;
            int likes;
            int servings;
            int cook_time_in_minutes;
            boolean [] allergyInformation;

            while (null != (id = reader.readLine())) {
                name = reader.readLine();
                picture = loadImage(name);
                author = reader.readLine();
                stepsString = reader.readLine();
                steps = getStepsFromLine(stepsString);
                calorieInformationLine = reader.readLine().split(",");
                calories = Integer.parseInt(calorieInformationLine[0]);
                protein = Integer.parseInt(calorieInformationLine[1]);
                carbs = Integer.parseInt(calorieInformationLine[2]);
                fat = Integer.parseInt(calorieInformationLine[3]);
                likes = Integer.parseInt(calorieInformationLine[4]);
                servings = Integer.parseInt(calorieInformationLine[5]);
                cook_time_in_minutes = Integer.parseInt(calorieInformationLine[6]);
                allergyInformation = getAllergyInformationFromLines(reader.readLine());
                favorites.add(new Recipe(id, name, author, picture, steps, calories,
                        protein, carbs, fat, likes, servings, cook_time_in_minutes, allergyInformation));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return favorites;
    }
    private void saveNewFavoritesListToFile(List<Recipe> favorites){
        PrintWriter writer = null;
        try {
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    fos)));

            for (int idx = 0; idx < favorites.size(); idx++) {
                Recipe curr = (Recipe) favorites.get(idx);
                saveImage(curr.getPicture(), curr.getName());
                for(Step step : curr.getSteps())
                    for(Ingredient ingredient: step.getIngredients())
                        saveImage(ingredient.getPicture(), ingredient.getName());
                writer.println(curr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != writer) {
                writer.close();
            }
        }
    }
    private void removeRecipeWithName (String name, List<Recipe> favorites){
        int i = 0;
        boolean found = false;

        for (Recipe r : favorites){
            if(r.getName().compareTo(name) == 0) {
                found = true;
                break;
            }
            i++;
        }
        if(found) {
            favorites.remove(i);
        }

    }
    public void addRecipeToFavoritesList(){
        List<Recipe> favoritesList = getFavoritesListFromFile();
        favoritesList.add(recipe);
        saveNewFavoritesListToFile(favoritesList);
        Log.d(TAG, "Added" + recipe.getName() + "recipe to favorites list");
    }
    public void removeRecipeFromFavoritesList(){
        List<Recipe> favoritesList = getFavoritesListFromFile();
        removeRecipeWithName(recipe.getName(), favoritesList);
        saveNewFavoritesListToFile(favoritesList);
        Log.d(TAG, "Removed" + recipe.getName() + "recipe from favorites list");
    }

}