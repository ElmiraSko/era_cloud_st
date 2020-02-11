package com.era.cloud.server;

import com.era.cloud.common.CloudPackage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
    String path = "fileTo/";
        try(ServerSocket serverSocket = new ServerSocket(8878)){
            System.out.println("Сервер запущен. Ожидает подключение клиента.");

            Socket socket = serverSocket.accept();
            System.out.println("Клиент подключился.");
            //inpST - входной байтовый поток от клиента
            DataInputStream inpST = new DataInputStream(socket.getInputStream());
            DataOutputStream outST = new DataOutputStream(socket.getOutputStream());
            CloudPackage pack = new CloudPackage();
            int key;
            if ((key = inpST.read()) != -1) {
                if (key == 15) {
                    pack.writeFile1(path, inpST); // запись файла
                }
                if (key == 17){
                    String command = pack.readCommand(inpST); // строковое представление команды
                    System.out.println(command);
                }
            }
            inpST.close();
            outST.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
