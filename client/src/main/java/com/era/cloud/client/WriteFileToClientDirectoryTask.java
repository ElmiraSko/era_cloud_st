package com.era.cloud.client;

import com.era.cloud.common.UploadFile;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WriteFileToClientDirectoryTask implements Task {
    private ObjectDecoderInputStream in;
    private File clientDirectory;


    WriteFileToClientDirectoryTask(ObjectDecoderInputStream in, File clientDirectory) {
        this.in = in;
        this.clientDirectory = clientDirectory;
    }

    @Override
    public void doing() {
        try {
            Object object = in.readObject();
            if (object instanceof UploadFile) { // если файл
                UploadFile file = (UploadFile) object;
                writeFileInDir(file);
            }
        } catch (IOException | ClassNotFoundException ex) {ex.printStackTrace();}
    }

    // запись файла на диск клиента
    private void writeFileInDir(UploadFile file) {
        boolean append = true;
        String fileName = file.getName();
        String filePath = clientDirectory + "/" + fileName;
        if (file.getPartNumber() == 1) { // если файл состоит из одной части, то не дописываем
            append = false;
        }
        try {
            File writeFile = new File(filePath);
            if (!writeFile.exists()) {
                writeFile.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(writeFile, append);
            out.write(file.getData()); // записали в файл
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("В методе writeFileInDir возникли проблемы.");}
        System.out.println("От сервера: получен файл " + fileName);
    }

}
