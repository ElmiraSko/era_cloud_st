package com.era.cloud.client;

import com.era.cloud.common.CommandMessage;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;

public class DeleteFileOnServerTask implements Task { // отправка команды на удаление, выполняется в отдельном потоке

    private ObjectEncoderOutputStream out; // выходной поток
    private CommandMessage deleteCommand; // объект-команда

    public DeleteFileOnServerTask(ObjectEncoderOutputStream out, CommandMessage deleteCommand) {
        this.out = out;
        this.deleteCommand = deleteCommand;
    }

    @Override
    public void doing() {
        try {
            out.writeObject(deleteCommand);
        } catch (IOException e) {e.printStackTrace();}
    }
}
