package com.era.cloud.client;

import com.era.cloud.common.CloudPackage;

import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {

        String path1 = "fileFrom/5.png";
        String path2 = "fileFrom/file1.txt";

        try(Socket socket = new Socket("localhost", 8878);
            DataOutputStream ds = new DataOutputStream(socket.getOutputStream()))
        {
            File file = new File(path2); // объект File
            String fileName = file.getName(); // получили имя файла
            int fileNameLength = fileName.length();
            int fileSize = (int) file.length(); // размер файла

            CloudPackage pack = new CloudPackage(fileName, fileNameLength, fileSize, 15);
            ds.write(pack.dataFromTheFile(file)); // отправка на сервер

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
