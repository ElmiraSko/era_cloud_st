package com.era.cloud.client;


import com.era.cloud.common.CommandMessage;
import com.era.cloud.common.UploadFile;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static com.era.cloud.common.CommandMessage.CMD_MSG_FILES_LIST;

public class SendFileTask implements Task{
    private final int MAX_SIZE = 1024*1024*100;

    private String filePath;
    private ObjectEncoderOutputStream out;

    public SendFileTask(String filePath, ObjectEncoderOutputStream out){
        this.filePath = filePath;
        this.out = out;
    }

    @Override
    public void doing() {
        File file = new File(filePath);
        int len = (int)file.length();
        UploadFile req = new UploadFile(file); // пересмотреть для оптимизации
        int partNumber = 1;
        req.setPartNumber(partNumber);
        try {
            FileInputStream inp = new FileInputStream(file);
            BufferedInputStream buff = new BufferedInputStream(inp, MAX_SIZE);
            if (len <= MAX_SIZE) {
                req.setData(new byte[len]);
                buff.read(req.getData());
                out.writeObject(req);
                out.flush();
            } else {
                while (len > 0) {
                    req.setPartNumber(partNumber);
                    req.setData(new byte[MAX_SIZE]);
                    if ((buff.available()) > 0) {
                        buff.read(req.getData());
                    }
                    len = len - MAX_SIZE;
                    partNumber++;
                    out.writeObject(req);
                    out.flush();
                }
            }
            buff.close();
        } catch (IOException e) {
            e.printStackTrace();
        }        System.out.println();
        System.out.println("Клиент отправил файл: " + file.getName());
//        getFileListFromServer();
    }


    // запрос на получение списка файлов находящихся на сервере
    private void getFileListFromServer() {
        CommandMessage message = new CommandMessage(CMD_MSG_FILES_LIST);
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException ex) {ex.printStackTrace();}
    }
}

