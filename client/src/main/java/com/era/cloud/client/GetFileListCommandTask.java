package com.era.cloud.client;


import com.era.cloud.common.AbstractMessage;
import com.era.cloud.common.CommandMessage;
import com.era.cloud.common.ServerListMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static com.era.cloud.common.CommandMessage.CMD_MSG_FILES_LIST;

public class GetFileListCommandTask implements Task {

    private ObjectEncoderOutputStream out;
    private ObjectDecoderInputStream in;
    private CountDownLatch cdl2;
    private DefaultListModel<String> listOnServerModel = new DefaultListModel<>();
    ArrayList<String> files;

    GetFileListCommandTask(ObjectEncoderOutputStream out, ObjectDecoderInputStream in, CountDownLatch cdl2) {
        this.out = out;
        this.in = in;
        this.cdl2 = cdl2;
    }

    @Override
    public void doing() {
        CommandMessage message = new CommandMessage(CMD_MSG_FILES_LIST);
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException ex) {ex.printStackTrace();}
        try {
            Object obj = in.readObject();
            if (obj instanceof AbstractMessage) {
                files = ((ServerListMessage) obj).getFilesList();
            }
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

    }

    public ArrayList<String> getFiles() {
        cdl2.countDown();
        return files;
    }
}

