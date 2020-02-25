package com.era.cloud.client;

import com.era.cloud.common.CommandMessage;
import com.era.cloud.common.LoginAndPasswordMessage;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;

import static com.era.cloud.common.CommandMessage.CMD_MSG_AUTH_OK;

public class AuthCommandTask implements Task { // задача передать команду для авторизации с объектом содержащим логин и пароль
    private LoginAndPasswordMessage loginAndPass; // сообщение
    private ObjectEncoderOutputStream out;

    AuthCommandTask( ObjectEncoderOutputStream out, LoginAndPasswordMessage loginAndPass) {
        this.out = out;
        this.loginAndPass = loginAndPass;
    }
    @Override
    public void doing() {
        CommandMessage message = new CommandMessage(CMD_MSG_AUTH_OK, loginAndPass);
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException ex) {ex.printStackTrace();}
    }

}
