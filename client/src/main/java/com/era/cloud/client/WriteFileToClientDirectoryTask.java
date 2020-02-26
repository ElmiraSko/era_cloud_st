package com.era.cloud.client;

import com.era.cloud.common.UploadFile;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class WriteFileToClientDirectoryTask implements Task {
    ObjectDecoderInputStream in;
    String clientDirectory;
//    CountDownLatch cdl;

    WriteFileToClientDirectoryTask(ObjectDecoderInputStream in, String clientDirectory) {
        this.in = in;
        this.clientDirectory = clientDirectory;
//        this.cdl = cdl;
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
//        cdl.countDown();
    }

    // запись файла на диск
    private void writeFileInDir(UploadFile file) {
        boolean append = true;
        String fileName = file.getName();
        String filePath = clientDirectory + fileName;
        if (file.getPartNumber() == 1) { // если файл состоит из одной части, то не дописываем
            append = false;
        }
        try {
            File writeFile = new File(filePath);
            FileOutputStream out = new FileOutputStream(writeFile, append);
            out.write(file.getData()); // записали в файл
            out.close();
        } catch (IOException ex) {ex.printStackTrace();}
        System.out.println("От сервера: получен файл " + fileName);
    }

}
