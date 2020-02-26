package com.era.cloud.common;

public class LoginAndPasswordMessage extends AbstractMessage {
    private final int AUTH = 259436787; // для авторизации
    private final int REG = 733277879; // для регистрации

    private String login;
    private String password;
    private int typeMess; // тип

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

    public int getTypeMess() {
        return typeMess;
    }
// методы для задания типа
    public void setTypeAUTH() {
        this.typeMess = AUTH;
    }
    public void setTypeREG() {
        this.typeMess = REG;
    }

    public boolean typeIsAUTH() {
        return typeMess == AUTH;
    }

    public boolean typeIsREG() {
        return typeMess == REG;
    }

}
