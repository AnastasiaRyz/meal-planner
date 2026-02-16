package mealplanner;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/*
Класс	За что отвечает
Meal	модель данных
Menu	логика приложения
MealDao	работа с БД
DatabaseManager	подключение + init
Main	консоль, ввод
*/

public class Main {
    public static void main(String[] args) {
        boolean isExit = false;
        Scanner scanner = new Scanner(System.in);
        String category;
        String mealName;

        DatabaseManager db = new DatabaseManager();
        db.connect(); //создаем подключение к бд
        db.init(); //создаем таблицы бд, если они еще не были созданы
        MealDao mealDao = new MealDao(db.getConnection());
        PlanDao planDao = new PlanDao(db.getConnection());

        while (!isExit){
            System.out.println("What would you like to do (add, show, plan, list plan, save, exit)?");
            String option = scanner.nextLine();
            switch (option){
                case "add" -> {
                    System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
                    //проверка, что категория соответствует одной из трех
                    category = readCategory(scanner);

                    System.out.println("Input the meal's name:");
                    //проверка на формат наименования (только буквы и пробел между словами)
                    mealName = readMeal(scanner);

                    System.out.println("Input the ingredients:");
                    //проверка на формат наименования ингредиентов (только буквы, "," и пробел между словами)
                    List<String> ingredients = readIngredients(scanner);// создаем лист и наполняем его ингредиентами введными пользователем

                    long mealId = mealDao.insertMeal(category, mealName);
                    for(String ingredient : ingredients){
                        mealDao.insertIngredients(ingredient, mealId);
                    }
                    System.out.println("The meal has been added!");
                }
                case "show" -> {
                    System.out.println("Which category do you want to print (breakfast, lunch, dinner)?");
                    category = readCategory(scanner);
                    printMeals(mealDao.showMeals(category));
                }

        /*
        Структура try/catch для транзакций
        connection.setAutoCommit(false);
         try {
          // операции
          connection.commit();

        } catch (Exception e) {
          connection.rollback();
        } finally {
          connection.setAutoCommit(true);
        }
         */
                case "plan" -> {
                    try {
                        //Каждый SQL-запрос - отдельная транзакция, он сразу коммитится автоматически. setAutoCommit(false) - выключает авто-коммит
                        db.getConnection().setAutoCommit(false);
                        planDao.deletePlan(); // удаляем план из бд

                        List<Plan> planListForPrint = new ArrayList<>();

                        for (Day day : Day.values()){
                            System.out.println(day.getDisplayName());

                            Map<String, Long> breakfastMap = mealDao.showMealsWithoutIngredients("breakfast");
                            printMealsWithoutIngredients(breakfastMap);
                            System.out.println("Choose the breakfast for " + day.getDisplayName() + " from the list above:");
                            mealName = checkMealName(scanner, breakfastMap);
                            Plan planBreakfast = new Plan(day, "breakfast", mealName, breakfastMap.get(mealName));
                            planDao.insertPlan(planBreakfast); // выполняет sql запрос на добавление плана в бд, он пока висит в памяти до commit()
                            planListForPrint.add(planBreakfast); // сохраняем объект plan в лист для печати

                            Map<String, Long> lunchMap = mealDao.showMealsWithoutIngredients("lunch");
                            printMealsWithoutIngredients(lunchMap);
                            System.out.println("Choose the lunch for " + day.getDisplayName() + " from the list above:");
                            mealName = checkMealName(scanner, lunchMap);
                            Plan planLunch = new Plan(day, "lunch", mealName, lunchMap.get(mealName));
                            planDao.insertPlan(planLunch);
                            planListForPrint.add(planLunch);

                            Map<String, Long> dinnerMap = mealDao.showMealsWithoutIngredients("dinner");
                            printMealsWithoutIngredients(dinnerMap);
                            System.out.println("Choose the dinner for " + day.getDisplayName() + " from the list above:");
                            mealName = checkMealName(scanner, dinnerMap);
                            Plan planDinner = new Plan(day, "dinner", mealName, dinnerMap.get(mealName));
                            planDao.insertPlan(planDinner);
                            planListForPrint.add(planDinner);

                            System.out.println("Yeah! We planned the meals for " + day.getDisplayName() + ".\n");
                        }
                        db.getConnection().commit(); // если все выполнено успешно, то делаем коммит, фиксируем состояние
                        printPlan(planListForPrint);//печатает план

                    }catch (Exception e){
                        try {
                            db.getConnection().rollback(); //если произошла ошибка, БД возвращается в состояние до начала транзакций
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }


                    }finally {
                        try {
                            db.getConnection().setAutoCommit(true); //возвращаем автокоммиты
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }

                case "list plan" -> {
                    printPlan(planDao.getPlan()); // получаем лист с планом из БД и передаем его в метод, который группирует план по дням недели и категориям
                }
                case "save" -> {
                    Map<String, Integer> shoppinListMap = planDao.getShoppinList();
                    createShoppingList(shoppinListMap, scanner);
                }

                case "exit" -> {
                    System.out.println("Bye!");
                    isExit = true;
                }
                default -> {}
            }
        }
        db.closeConnetion();
    }

    private static String readCategory(Scanner scanner) {
        while (true) {
            String input = scanner.nextLine();
            if (input.equals("breakfast") ||
                    input.equals("lunch") ||
                    input.equals("dinner")) {
                return input;
            }
            System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
        }
    }

    private static String readMeal(Scanner scanner){
        String mealName;
        while (true){
            mealName = scanner.nextLine();
            if(mealName.matches("[a-zA-Z ]+")){
                return mealName;
            }
            System.out.println("Wrong format. Use letters only!");
        }
    }

    private static List<String> readIngredients(Scanner scanner) {
        List<String> ingredients = new ArrayList<>();
        String ingredientStr;
        while (true){
            ingredientStr = scanner.nextLine();
            if(ingredientStr.matches("[a-zA-Z ]+(, ?[a-zA-Z ]+)*")){
                String[] ingredientsArray = ingredientStr.split(",");
                ingredients = new ArrayList<>();
                for(String ingredient : ingredientsArray){
                    ingredients.add(ingredient.trim());
                }
                return ingredients;
            }
            System.out.println("Wrong format. Use letters only!");
        }
    }

    private static void printMeals(List<Meal> meals){

        if (meals.isEmpty()) {
            System.out.println("No meals found.");
            return;
        }

        System.out.println("Category: " + meals.getFirst().getCategory());

        for (Meal meal : meals) {
            System.out.println("\nName: " + meal.getName()
                    + "\nIngredients:");
            for(String ingredient : meal.getIngredients()){
                System.out.println(ingredient);
            }
        }
    }

    private static void printMealsWithoutIngredients(Map<String, Long> breakfastMap){
        if (breakfastMap.isEmpty()) {
            System.out.println("No meals found.");
            return;
        }
        for (String meal : breakfastMap.keySet()) {
            System.out.println(meal);
        }
    }

    //проверка, что введенное пользователем блюдо есть в списке блюд БД
    private static String checkMealName(Scanner scanner, Map<String, Long> breakfastMap){
        String mealName;

        while (true){
            mealName = scanner.nextLine();
            if(breakfastMap.containsKey(mealName)){
                return mealName;
            }
            System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
        }
    }

    private static void printPlan(List<Plan> planFromDb){
        if (planFromDb.isEmpty()){
            System.out.println("Database does not contain any meal plans");
            return;
        }

        //группируем по первому ключу - день недели; внутри этой группировки по категории (Breakfast → yogurt). LinkedHashMap - для сохранения порядка по дням недели
        Map<Day, Map<String, String>> weekPlan = new LinkedHashMap<>();

        for (Plan plan : planFromDb) {
            //putIfAbsent - добавляет пару «ключ-значение» в коллекцию, если указанный ключ отсутствует или null. То есть
            // день недели НЕ перезаписывается
            weekPlan.putIfAbsent(plan.getDayOfWeek(), new LinkedHashMap<>());

            // получаем день недели из weekPlan для этого дня недели записываем категорию и наименование блюда
            weekPlan.get(plan.getDayOfWeek()).put(plan.getCategory(), plan.getMealName());
        }

        for (Day day : Day.values()){
            System.out.println(day.getDisplayName());
            Map <String, String> planForDay = weekPlan.get(day); // получаем план (категория + блюдо) на каждый день недели
            if(planForDay != null){
                System.out.println("Breakfast: " + planForDay.get("breakfast"));
                System.out.println("Lunch: " + planForDay.get("lunch"));
                System.out.println("Dinner: " + planForDay.get("dinner") + "\n");
            }
        }
    }

    private static void createShoppingList(Map<String, Integer> shoppinListMap, Scanner scanner){
        if (shoppinListMap.isEmpty()){
            System.out.println("Unable to save. Plan your meals first.");
            return;
        }

        /*eggs
        tomato x3
        beef
        broccoli
        salmon
        chicken x2*/

        System.out.println("Input a filename:");
        String fileName = scanner.nextLine();

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) { //Создает(или перезаписывает если уже есть)
            // файл. Так как FileWriter записывает посимвольно, то оборачиваем его в буферезированный поток - записали
            // нужное кол-во информации в буфер -> сохранили в файл
            for (Map.Entry<String, Integer> entry : shoppinListMap.entrySet()){
                writer.write(entry.getKey());
                int count = entry.getValue();
                if(count > 1){
                    writer.write(" x" + count);
                }
                writer.newLine();
            }
            System.out.println("Saved!");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
