package com.era.cloud.common;

public class LoginAndPasswordMessage extends AbstractMessage{
    private String login;
    private String password;

    public LoginAndPasswordMessage(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
