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

    // метод для создания таблицы auth в бд DataBase.
    public void createTable(){
        try{
            String query = String.format("CREATE TABLE auth (id integer primary key autoincrement, login varchar not null,  password varchar not null, nick varchar not null)");
            Statement state = connection.createStatement();
            state.executeUpdate(query);
//            String query2 = "INSERT INTO auth (login, password, nick)\n" +
//                    "VALUES ('login1', 'pass1', 'nick1');";
//            state = connection.createStatement();
//            state.executeUpdate(query2);
        }catch (SQLException ex){}
    }
    // метод для закрытия connection
    void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println("Не закрыли" + e.getMessage());
        }
    }
    //добавляет в базу нового пользователя
    public String registration(String login, String pass, String nick){
        String sss = null;
        String queryALL = "SELECT * FROM auth";
        int count = 0;
        try {
            state = connection.createStatement();
            ResultSet res = state.executeQuery(queryALL);
            while (res.next()){
                String log_  = res.getString("login");
                String pass_  = res.getString("password");
                String nick_  = res.getString("nick");
                if (!log_.equals(login) && !pass_.equals(pass) && !nick_.equals(nick)){
                }else count++;
            }
            if (count == 0){
                String query = "INSERT INTO auth (login, password, nick) VALUES ('" + login + "', '" + pass + "', '" + nick + "');";
                state.executeUpdate(query);
                System.out.println("Регистрация прошла успешно");
                ResultSet rs = state.executeQuery("SELECT * FROM auth WHERE login = '" + login + "'" + " AND password = '" + pass + "'");
                sss = rs.getString("nick");
            }else {
                System.out.println("Регистрация не выполнена");
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return sss;
    }
    //==== метод для смены ника
    String getNewNick(String login, String pass, String nick){
        String nickName = null;
        String query = "UPDATE auth SET nick = '" + nick + "' WHERE login = '" + login + "' AND password = '" + pass + "'";
        try {
            state = connection.createStatement();
            state.executeUpdate(query);
            ResultSet getNewNick = state.executeQuery("SELECT nick FROM auth WHERE login = '" + login + "'" + " AND password = '" + pass + "'");
            while (getNewNick.next()){
                nickName = getNewNick.getString("nick");}
        }catch (SQLException ex){
            ex.printStackTrace();}
        return nickName;
    }
}

