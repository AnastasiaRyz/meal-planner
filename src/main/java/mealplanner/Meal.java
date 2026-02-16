package mealplanner;

import java.util.List;

public class Meal {
    private String category;
    private String name;
    private List<String> ingredients;

    public Meal(String category, String name, List<String> ingredients){
        this.category = category;
        this.name = name;
        this.ingredients = ingredients;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void addIngredient(String ingredient) {
        ingredients.add(ingredient); // добавляем только один элемент к уже существующему списку (не перезатирает предыдущие значения, если они есть)
    }
}