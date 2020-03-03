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
    private final int MAX_SIZE = 1024*1024;

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
        try {
            if (len <= MAX_SIZE) {
                FileInputStream inp = new FileInputStream(file);
                BufferedInputStream buff = new BufferedInputStream(inp, len);
                req.setData(new byte[len]);
                req.setPartNumber(partNumber);
                buff.read(req.getData());
                out.writeObject(req);
                out.flush();
                buff.close();
            } else {
                FileInputStream inp = new FileInputStream(file);
                BufferedInputStream buff = new BufferedInputStream(inp, MAX_SIZE);
                int countPart = len/MAX_SIZE + 1;
                req.setData(new byte[MAX_SIZE]);
                while (len > 0) {
                    if (len <= MAX_SIZE) {
                        req.setData(new byte[len]);
                    }
                    req.setPartNumber(partNumber);
                    req.setCountNumber(countPart);
                    if ((buff.available()) > 0) {
                        buff.read(req.getData());
                    }
                    len = len - MAX_SIZE;
                    System.out.println(" Осталось прочитать еще " + len);
                    partNumber++;
                    out.writeObject(req);
                    out.flush();
                }
                buff.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }        System.out.println();
        System.out.println("Клиент отправил файл: " + file.getName());
    }
}

