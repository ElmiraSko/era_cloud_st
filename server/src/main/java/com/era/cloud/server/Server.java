package com.era.cloud.server;

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

            int key = inpST.read(); // прочитали 1-й байт (сигнальный)
            if (key == 15) {
                int sizeName = inpST.read();
                String strName = buildNameFile(sizeName, inpST); // получили имя файла
                System.out.println("Получен файл " + strName);

                File newFile = new File("fileTo\\" + strName); // создание объекта File
                writeFile(newFile, inpST); // запись файла
            } else if (key == 17){
                String command = readCommand(inpST);
                System.out.println(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    //Метод buildNameFile - возвращает имя файла полученного из входного потока inpSt
    // size - длина имени файла
    private static String buildNameFile(int size, InputStream inpSt) throws IOException{
        StringBuilder build = new StringBuilder();
        for (int i = 0; i < size; i++) {
            build.append((char)inpSt.read());
        }
        return build.toString();
    }

    // Метод writeFile - записывает в file из InputStream inpSt
    // без получения размера файла в сообщении
    private static void writeFile(File file, InputStream inpSt){
        try {
            FileOutputStream ft = new FileOutputStream(file);
            int m;
            while ((m = inpSt.read()) != -1){
                ft.write(m);
                System.out.print((char) m);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
// чтение служебных сообщений
    private static String readCommand(InputStream inpSt) throws IOException{
        BufferedReader buff = new BufferedReader(new InputStreamReader(inpSt));
        StringBuilder br = new StringBuilder();
        String m;
        while ((m = buff.readLine()) != null) {
            br.append(m);
        }
        return br.toString();
    }

}

