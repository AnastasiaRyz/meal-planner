package mealplanner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MealDao {
    private Connection connection;

    public MealDao(Connection connection) {
        this.connection = connection;
    }

    public long insertMeal(String category, String meal) {
        String sql = "INSERT INTO meals(category, meal) VALUES(?, ?) RETURNING meal_id";

        /* 1. PreparedStatement - подготовленный SQL-запрос;
        2. executeUpdate (возвращает кол-во измененных строк) или executeQuery (возвращает ResultSet) - отправка запроса.
        Используем executeQuery при запросах: "INSERT...RETURNING" или "SELECT" (то, что возвращает данные из БД);
        3. ResultSet - ответ (таблица-курсор) от БД (если он есть)*/
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            statement.setString(2, meal);

            try (ResultSet rs = statement.executeQuery()) {
                rs.next(); // при создании ResultSet курсор перед первой строкой, поэтому двигаем его на первую строку
                return rs.getInt("meal_id");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertIngredients(String ingredient, long mealId) {
        String sql = "INSERT INTO ingredients(ingredient, meal_id) VALUES(?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ingredient);
            statement.setLong(2, mealId);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Meal> showMeals(String category) {
        List<Meal> mealList = new ArrayList<>();
        Map<Integer, Meal> mealMap = new HashMap<>(); //используем для исключения дубликатов блюд по id

        String sql = """ 
                SELECT m.meal_id, m.category, m.meal, i.ingredient
                FROM meals m
                LEFT JOIN ingredients i ON m.meal_id = i.meal_id
                WHERE m.category = ?
                ORDER BY m.meal_id
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, category); // передаем значение переменной метода category в WHERE запроса

            //используем отдельный try для resultSet, так как важна последовательность "создать statement" ->
            // "вызвать setString" -> "потом только executeQuery"
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int mealId = resultSet.getInt("meal_id"); //достаем id блюда из resultSet

                    //Проверяем есть ли объект блюдо в списке
                    Meal meal = mealMap.get(mealId);
                    if (meal == null) {
                        meal = new Meal(
                                resultSet.getString("category"),
                                resultSet.getString("meal"),
                                new ArrayList<>()// добавляем пустой список ингредиентов
                        );
                        mealMap.put(mealId, meal);
                        mealList.add(meal);
                    }

                    //добавляем ингредиенты, если они есть
                    String ingredient = resultSet.getString("ingredient");
                    if (ingredient != null) {
                        meal.addIngredient(ingredient);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return mealList;
    }

    public Map<String, Long> showMealsWithoutIngredients(String category) {
        Map<String, Long> mealMap = new LinkedHashMap<>();

        String sql = """ 
                SELECT meal, meal_id
                FROM meals
                WHERE category = ?
                ORDER BY meal
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, category); // передаем значение переменной метода category в WHERE запроса

            //используем отдельный try для resultSet, так как важна последовательность "создать statement" ->
            // "вызвать setString" -> "потом только executeQuery"
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    mealMap.put(resultSet.getString("meal"), resultSet.getLong("meal_id"));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return mealMap;
    }
}