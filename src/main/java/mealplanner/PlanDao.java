package mealplanner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanDao {
    private Connection connection;

    public PlanDao(Connection connection) {
        this.connection = connection;
    }

    public void insertPlan(Plan plan) {
        String sql = "INSERT INTO plan(meal_category, day_of_week, meal_id) VALUES(?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, plan.getCategory());
            statement.setString(2, plan.getDayOfWeek().name()); // сохраняем в бд значение в формате MONDAY, а не displayName
            statement.setLong(3, plan.getMealId());
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Plan> getPlan() {
        List<Plan> planList = new ArrayList<>();

        String sql = """ 
                SELECT p.day_of_week, p.meal_category, p.meal_id, m.meal
                FROM plan p
                LEFT JOIN meals m ON m.meal_id = p.meal_id
                ORDER BY day_of_week, meal_category
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String dayOfWeek = resultSet.getString("day_of_week"); // получаем день недели из БД в виде строки
                Day day = Day.valueOf(dayOfWeek); // преобразуем строку в переменную enum
                String mealCategory = resultSet.getString("meal_category");
                long mealId = resultSet.getLong("meal_id");
                String mealName = resultSet.getString("meal");

                Plan plan = new Plan(day, mealCategory, mealName, mealId);
                planList.add(plan);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return planList;
    }

    public void deletePlan() {
        String sql = "TRUNCATE TABLE plan";//TRUNCATE очищает таблицу полностью, что делает его быстрее чем DELETE(построчная очистка)

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> getShoppinList(){
        Map<String, Integer> shoppinListMap = new HashMap<>();
        String sql = """ 
                SELECT i.ingredient, COUNT(*) as count_ingredients
                FROM plan p
                LEFT JOIN ingredients i ON i.meal_id = p.meal_id
                GROUP BY i.ingredient
                """;

        try(PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()){
                shoppinListMap.put(resultSet.getString("ingredient"), resultSet.getInt("count_ingredients"));
            }

        }catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return shoppinListMap;
    }
}