package com.era.cloud.client;


import com.era.cloud.common.CommandMessage;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;

import static com.era.cloud.common.CommandMessage.CMD_MSG_FILES_LIST;

public class GetFileListCommandTask implements Task {

    private ObjectEncoderOutputStream out;

    GetFileListCommandTask(ObjectEncoderOutputStream out) {
        this.out = out;
    }

    @Override
    public void doing() {
        CommandMessage message = new CommandMessage(CMD_MSG_FILES_LIST);
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException ex) {ex.printStackTrace();}
    }
}

