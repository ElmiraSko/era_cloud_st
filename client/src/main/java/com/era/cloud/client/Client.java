package com.era.cloud.client;

import com.era.cloud.common.CloudPackage;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client {

    public static void main(String[] args) {
        Path  path3 = Paths.get("fileFrom/5.png");
        try{
            Socket socket = new Socket("localhost", 8878);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            CloudPackage pack = new CloudPackage();

            pack.dataFromTheFile(path3, out); // отправка на сервер
//            pack.dataFromFile(path3, socket); //
//            pack.fromFile(path3);       //

        in.close();
        out.close();
        socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





}
