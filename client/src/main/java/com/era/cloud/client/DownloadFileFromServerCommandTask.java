package com.era.cloud.client;

import com.era.cloud.common.CommandMessage;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;

public class DownloadFileFromServerCommandTask implements Task {
    private CommandMessage com;
    private ObjectEncoderOutputStream out;

    DownloadFileFromServerCommandTask(CommandMessage com, ObjectEncoderOutputStream out) {
        this.com = com;
        this.out = out;
    }
    @Override
    public void doing() {
        try {
            out.writeObject(com);
            out.flush();
        } catch (IOException ex) {ex.printStackTrace();}
    }
}
