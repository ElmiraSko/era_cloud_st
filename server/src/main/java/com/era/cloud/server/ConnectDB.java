package com.era.cloud.server;

import java.sql.*;

public class ConnectDB {
    private Connection connection = null;
    private Statement state;
    private final String DB_URL = "jdbc:sqlite:server/DB/DataBase.db";
    private final String DB_Driver = "org.sqlite.JDBC";
    //-----------------------------------------------------------------------------------------------------------------

    ConnectDB() {
        try {
            Class.forName(DB_Driver);
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Все отлично! Мы подключились к БД");
        } catch (ClassNotFoundException ex) {
            System.out.println("Не удалось соединиться с базой: " + ex.getMessage());
        } catch (Exception e) {
            System.out.println("Что-то не так, надо разобраться! " + e.getMessage());
        }

    }
// авторизация, проверяем, есть ли юзер в база
    String authorize(String login, String pass) {
        String login_user = null;
        String query = "SELECT login, password FROM users WHERE login = '" + login + "'" + " AND password = '" + pass + "'";
        try {
            state = connection.createStatement();
            ResultSet res = state.executeQuery(query);
            if (res.next()) {
                login_user = res.getString("login");
                System.out.println("Пользователь есть в базе");
            }
            else {
                System.out.println("Пользователя нет в базе");
            }
        } catch (SQLException ex) {ex.printStackTrace();}
        return login_user;
    }

    // добавление нового пользователя
    void writeNewUser(String login, String pass) {
        String query = "INSERT INTO users (login, password) VALUES ('" + login + "', '" + pass + "');";
        try {
            state = connection.createStatement();
            state.executeUpdate(query);
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.out.println("Проблемы с регистрацией");
        }
    }




    // метод для закрытия connection
    void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println("Не закрыли" + e.getMessage());
        }
    }

    // метод для создания таблицы auth в бд DataBase.
    public void createTable(){
        try{
            String query = String.format("CREATE TABLE users (id integer primary key autoincrement, login varchar not null,  password varchar not null, unique (login))");
//            String q = "CREATE TABLE users (id integer primary key autoincrement, login varchar not null,  password varchar not null, unique (login)";
            Statement state = connection.createStatement();
            state.executeUpdate(query);
        }catch (SQLException ex){
            System.out.println("Проблемы при создании таблицы users");
        }
    }

    public void all() {
        String queryALL = "SELECT * FROM users";
        try {
            state = connection.createStatement();
            ResultSet res = state.executeQuery(queryALL);
            System.out.println(res.next() + " есть ли пользователи в базе");
            while (res.next()) {
                String log_ = res.getString("login");
                String pass_ = res.getString("password");
                System.out.println(log_ + ", " + pass_);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}

