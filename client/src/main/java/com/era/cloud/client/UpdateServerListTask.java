package com.era.cloud.client;

import com.era.cloud.common.AbstractMessage;
import com.era.cloud.common.ServerListMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;

import javax.swing.*;
import java.io.IOException;

public class UpdateServerListTask implements Task { // пока не используем

    private DefaultListModel<String> listOnServerModel;
    private ObjectDecoderInputStream in;


    UpdateServerListTask(ObjectDecoderInputStream in, DefaultListModel<String> listOnServerModel) {
        this.in = in;
        this.listOnServerModel = listOnServerModel;
    }

    @Override
    public void doing() {
        listOnServerModel.clear();
        try {
            Object obj = in.readObject();
            if (obj instanceof AbstractMessage) {
                String[] list = ((ServerListMessage) obj).getFilesList();
                for (String s: list) {
                    listOnServerModel.addElement(s);
                    System.out.println(s);
                }

            }
        } catch (IOException | ClassNotFoundException ex){ex.printStackTrace();}
    }
}
