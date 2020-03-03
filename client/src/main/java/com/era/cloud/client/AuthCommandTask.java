package com.era.cloud.client;

import com.era.cloud.common.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static com.era.cloud.common.CommandMessage.CMD_MSG_AUTH_OK;

public class AuthCommandTask implements Task { // задача передать команду для авторизации с объектом содержащим логин и пароль

    private LoginAndPasswordMessage loginAndPass; // сообщение
    private ObjectEncoderOutputStream out;
    private ObjectDecoderInputStream in;
    private CountDownLatch cc;

    private String messFromServer;

    AuthCommandTask(ObjectEncoderOutputStream out, ObjectDecoderInputStream in, LoginAndPasswordMessage loginAndPass, CountDownLatch cc) {
        this.out = out;
        this.in = in;
        this.loginAndPass = loginAndPass;
        this.cc = cc;
    }
    @Override
    public void doing() {
        CommandMessage message = new CommandMessage(CMD_MSG_AUTH_OK, loginAndPass);
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException ex) {ex.printStackTrace();}

        readMessFromServer();

    }

    // получение ответа от сервера
    private void readMessFromServer() {
        try {
            Object message = in.readObject();
            if (message instanceof SimpleMessage) {
                messFromServer = ((SimpleMessage) message).getMessage();
            }
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            System.out.println("Ошибка при получении сообщения!");
        }
    }
    public String serverAnswer() { // возвращаем ответ
        cc.countDown();
        return messFromServer;
    }
}
