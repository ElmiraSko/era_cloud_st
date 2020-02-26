package com.era.cloud.client;

import javax.swing.*;
import java.io.File;

public class UpdateClientListTask implements Task {
    private DefaultListModel<String> clientListModel;
    private String clientDirectory;

    public UpdateClientListTask(DefaultListModel<String> clientListModel, String clientDirectory) {
        this.clientListModel = clientListModel;
        this.clientDirectory = clientDirectory;
    }

    @Override
    public void doing() {
        clientListModel.clear();
        File dir = new File(clientDirectory);
        String[] files = dir.list();
        if (files != null) {
            for (String s : files)
                clientListModel.addElement(s);
        }
    }
}
