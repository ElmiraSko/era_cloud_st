package com.era.cloud.client;

import javax.swing.*;
import java.io.File;
import java.io.FileFilter;

public class UpdateClientListTask implements Task {

    private DefaultListModel<File> clientListModel; // модель клиентского списка
    private File clientFile;  // путь к файлу

    UpdateClientListTask(DefaultListModel<File> clientListModel, File clientFile) {
        this.clientListModel = clientListModel;
        this.clientFile = clientFile;
    }

    @Override
    public void doing() {
        clientListModel.clear();
        File[] filesAndDirectory = clientFile.listFiles(new MyFilter());
        if (filesAndDirectory != null) {
            for (File s : filesAndDirectory){
                clientListModel.addElement(s);
//            System.out.println(s);
            }
        }
        else System.out.println("Возникла проблема при обновлении файлов!");
    }

    private class MyFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return !pathname.isHidden();
        }
    }
}
