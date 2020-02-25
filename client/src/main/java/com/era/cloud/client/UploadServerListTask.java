package com.era.cloud.client;

import com.era.cloud.common.AbstractMessage;
import com.era.cloud.common.ServerListMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;

import javax.swing.*;
import java.io.IOException;

public class UploadServerListTask implements Task { // пока не используем

    private DefaultListModel<String> listOnServerModel = new DefaultListModel<>();
    private ObjectDecoderInputStream in;


    UploadServerListTask(ObjectDecoderInputStream in) {
        this.in = in;
    }

    public DefaultListModel<String> getListOnServerModel() {
        return listOnServerModel;
    }

    @Override
    public void doing() {
//        DefaultListModel<String> listModel = new DefaultListModel<>();
//        listOnServerModel.clear();
        System.out.println("jjjjjjjjjjjjjjjjjjjjjjjjjj");
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
