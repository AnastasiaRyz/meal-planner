package mealplanner;

public class Plan {
    private Day dayOfWeek;
    private String category;
    private String mealName;
    private long mealId;

    public Plan(Day dayOfWeek, String category, String mealName, long mealId) {
        this.dayOfWeek = dayOfWeek;
        this.category = category;
        this.mealName = mealName;
        this.mealId = mealId;
    }

    public Day getDayOfWeek() {
        return dayOfWeek;
    }

    public String getCategory() {
        return category;
    }

    public String getMealName() {
        return mealName;
    }

    public long getMealId() {
        return mealId;
    }
}
