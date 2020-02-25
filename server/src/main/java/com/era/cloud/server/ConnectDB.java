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

    public void writeNewUser() {
            String query2 = "INSERT INTO users (login, password)\n" +
            "VALUES ('login1', 'pass1');";
            try {
                state = connection.createStatement();
                state.executeUpdate(query2);
            } catch (SQLException ex) {ex.printStackTrace();}

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
//            String q = "CREATE TABLE auth (id integer primary key autoincrement, login varchar not null,  password varchar not null, unique (login)";
            Statement state = connection.createStatement();
            state.executeUpdate(query);

//            String query2 = "INSERT INTO users (login, password)\n" +
//                    "VALUES ('login1', 'pass1');";
//            try {
//                state = connection.createStatement();
//                state.executeUpdate(query2);
//            } catch (SQLException ex) {ex.printStackTrace();}

        }catch (SQLException ex){
            System.out.println("Проблемы при создании таблицы auth");
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

    //добавляет в базу нового пользователя? надо проверить
    public String registration(String login, String pass){
        String sss = null;
        String queryALL = "SELECT * FROM users";
        int count = 0;
        try {
            state = connection.createStatement();
            ResultSet res = state.executeQuery(queryALL);
            while (res.next()){
                String log_  = res.getString("login");
                String pass_  = res.getString("password");
                if (!log_.equals(login) && !pass_.equals(pass)){
                }else count++;
            }
            if (count == 0){
                String query = "INSERT INTO auth (login, password) VALUES ('" + login + "', '" + pass + "');";
                state.executeUpdate(query);
                System.out.println("Регистрация прошла успешно");
                ResultSet rs = state.executeQuery("SELECT * FROM users WHERE login = '" + login + "'" + " AND password = '" + pass + "'");
                sss = rs.getString("nick");
            }else {
                System.out.println("Регистрация не выполнена");
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return sss;
    }
}

