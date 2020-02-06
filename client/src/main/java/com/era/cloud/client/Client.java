package com.era.cloud.client;

import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {

        String path1 = "fileFrom/5.png";
        String path2 = "fileFrom/3.txt";

        try(Socket socket = new Socket("localhost", 8878);
            DataOutputStream ds = new DataOutputStream(socket.getOutputStream()))
        {
            File file = new File(path1); // объект File

//            ds.write(dataFromTheFile(file));   // или
            ds.write(dataFromFile(file));         // или
//            ds.write(command("Авторизоваться и отправить файл")); // или

        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Формирование массива байт файла по протоколу с использованием ByteArrayOutputStream
    // (сигнальный байт, длина имени файла, имя файла, содержимое файла)
    // без передачи размера файла
    private static byte[] dataFromTheFile(File file) {
        String fileName = file.getName(); // получили имя файла с расширением
        int fileNameLength = fileName.length(); // кол-во символов в имени файла
        int fileSize = (int)file.length(); // размер файла, вообще там long
        int allSize = fileNameLength + fileSize + 2;

        ByteArrayOutputStream bout = new ByteArrayOutputStream(allSize);

        try(FileInputStream inp = new FileInputStream(file)) {

            DataOutputStream dd = new DataOutputStream(bout);
            dd.write(15);  // сигнальный байт
            dd.write(fileNameLength); // длина имени файла
            dd.write(fileName.getBytes()); // байты имени файла

            for (int i = 0; i < fileSize; i++) {
                dd.write(inp.read());
            }
            System.out.println("Указанный файл отправлен на сервер: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bout.toByteArray();
    }
    // Формирование массива байт для отправки файла на сервер
    // на массиве: сигнальный байт, длина имени файла, имя файла, содержимое файла
    private static byte[] dataFromFile(File file) {
        String fileName = file.getName(); // получили имя файла с расширением
        int fileNameLength = fileName.length(); // кол-во символов в имени файла
        int fileSize = (int)file.length(); // размер содержимого файла, (вообще там long)
        int allSize = fileNameLength + fileSize + 2;

        byte[] data = new byte[allSize];
        data[0] = 15; // сигнальный байт
        data[1] = (byte) fileNameLength; // длина имени файла

        byte[] nameB = fileName.getBytes(); // массив байт имени файла
        System.arraycopy(nameB, 0, data, 2, nameB.length); // добавили в data имя файла

        try(FileInputStream inp = new FileInputStream(file)){
            int k = inp.read(data, (2 + fileNameLength) , inp.available()); // записываем содержимое файла в data
            System.out.println(k + " - количество записанных байт");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
    // Формирует массив байтов служебной команды для передачи без указания размера (пока)
// String str - текст служебной команды
    private static byte[] command(String str) {
        byte[] strByte = str.getBytes();
        byte[] command = new byte[strByte.length + 1]; // итоговый массив
        System.out.println(str.length() + " длина строки");
        System.out.println(strByte.length + " длина массива под строку");
        command[0] = 17; // сигнальный байт
        System.arraycopy(strByte, 0, command, 1, strByte.length);
        return command;
    }

}
