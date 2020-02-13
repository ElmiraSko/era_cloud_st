
package com.era.cloud.common;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CloudPackage {
    private DataInputStream in;
    private DataOutputStream out;
    private DataType data;
    private String command;

    private enum DataType {EMPTY, FILE, COMMAND}

    public CloudPackage(DataInputStream in, DataOutputStream out){
        this.in = in;
        this.out = out;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    private DataType getDataType() { // определение типа сообщения
        try {
            int firstMessageByte = in.read();
            if(firstMessageByte == 15){
                data = DataType.FILE;
            } else
            if (firstMessageByte == 17) {
                data = DataType.COMMAND;
            } else data = DataType.EMPTY;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public void readMessage() { // чтение из входящего потока
        switch (getDataType()) {
            case FILE:
            {
                try {
                    writeToFile(in);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            case COMMAND:
            {
                try {
                    readCommand(in);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            case EMPTY:
                break;
        }
    }

    //================ передача файла ==================================
    public void sendFile(Path path) throws IOException {
        String fileName = path.getFileName().toString(); // получили имя файла из пути
        byte fileNameLength = (byte) fileName.length(); // длина имени файла (сколько это байт?)
        int fileSize = (int) Files.size(path);// размер файла, здесь пробрасывается исключение
        out.write(15);  // сигнальный байт
        out.write(fileNameLength); // передали длину имени файла
        out.write(fileName.getBytes()); // передали байты имени файла
        try(FileInputStream inp = new FileInputStream(path.toString())) { // открыли входящий поток из файла
            for (int i = 0; i < fileSize; i++) {
                out.write(inp.read()); // считываем из файла и отправляем в поток
            }
            System.out.println("Передача файла: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================================= передача команды ===================
    public void writeCommand(String str) {
        byte[] strByte = str.getBytes();
        try {
            out.write(17);
            out.write(strByte);
        } catch (IOException e) {e.printStackTrace();}
        System.out.println("Команда: " + str);
    }
    //=========================================================================================
    //Метод buildNameFile - возвращает имя файла полученного из входного потока inpSt
    private String buildNameFile(int size, InputStream inpSt) throws IOException{
        StringBuilder build = new StringBuilder();
        for (int i = 0; i < size; i++) {
            build.append((char)inpSt.read());
        }
        return build.toString();
    }

    private void writeToFile(InputStream inpSt) throws IOException {
        int sizeName = inpSt.read();
        String strName = buildNameFile(sizeName, inpSt); // получили имя файла
        System.out.println("Получен файл " + strName);
        String clientDir = "fileTo\\";
        String clientFile = clientDir + strName;
        File file = new File(clientFile); // создание объекта File
        try {
            FileOutputStream ft = new FileOutputStream(file);
            int m;
            while ((m = inpSt.read()) != -1){
                ft.write(m);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    // ===================== чтение служебных сообщений
    private void readCommand(InputStream inpSt) throws IOException{
        BufferedReader buff = new BufferedReader(new InputStreamReader(inpSt));
        StringBuilder br = new StringBuilder();
        String m;
        while ((m = buff.readLine()) != null) {
            br.append(m);
        }
//        return br.toString();
        System.out.println("Команда: " + br.toString());
    }
}
