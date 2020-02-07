package com.era.cloud.server;

import com.era.cloud.common.CloudPackage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {

        try(ServerSocket serverSocket = new ServerSocket(8878)){
            System.out.println("Сервер запущен. Ожидает подключение клиента.");

            Socket socket = serverSocket.accept();
            System.out.println("Клиент подключился.");
            // входной байтовый поток от клиента
            DataInputStream inpST = new DataInputStream(socket.getInputStream());

            CloudPackage pack = new CloudPackage();

            int key = inpST.read(); // прочитали 1-й байт (сигнальный)

            if (key == 15) {

                int sizeName = inpST.read();
                String strName = pack.buildNameFile(sizeName, inpST); // получили имя файла
                System.out.println("Получен файл " + strName);

                File newFile = new File("fileTo\\" + strName); // создание объекта File
                pack.writeFile(newFile, inpST); // запись файла

            } else if (key == 17){

                String command = pack.readCommand(inpST);
                System.out.println(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
