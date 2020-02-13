package com.era.cloud.client;

import com.era.cloud.common.CloudPackage;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client {
    public static void main(String[] args) {

        Path path1 = Paths.get("fileFrom/5.png");
        Path path2 = Paths.get("fileFrom/2.txt");


        try(Socket socket = new Socket("localhost", 8078);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream()))
        {
            CloudPackage pack = new CloudPackage(input, output);
//            pack.sendFile(path1); // передача файла
            pack.writeCommand("Hello!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
