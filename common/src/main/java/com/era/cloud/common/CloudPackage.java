package com.era.cloud.common;

import java.io.*;

public class CloudPackage {

    private String fileName;
    private int fileNameLength;
    private int fileSize;
    private int key;

    private String text;
    private int textLength;

//Конструкторы
    public CloudPackage() {}

    public CloudPackage(String text, int textLength) {
        this.text = text;
        this.textLength = textLength;
    }

    public CloudPackage(String fileName, int nameLength, int fileSize, int key) {
        this.fileName = fileName;
        fileNameLength = nameLength;
        this.fileSize = fileSize;
        this.key = key;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTextLength(int textLength) {
        this.textLength = textLength;
    }

    // Формирует массив байтов для передачи файла
    //(сигнальный байт, длина имени файла, имя файла, содержимое файла)
    public byte[] dataFromTheFile(File file) {
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

    // Формирует массив байтов для команды без указания размера (пока)
// String str - текст команды
    public byte[] command(String str) {
        byte[] strByte = str.getBytes();
        byte[] command = new byte[strByte.length + 1]; // итоговый массив
        System.out.println(str.length() + " длина строки");
        System.out.println(strByte.length + " длина массива под строку");
        command[0] = 17; // сигнальный байт
        System.arraycopy(strByte, 0, command, 1, strByte.length);
        return command;
    }

    //Метод buildNameFile - возвращает имя файла полученного из входного потока inpSt
    // size - длина имени файла
    public String buildNameFile(int size, InputStream inpSt) throws IOException{
        StringBuilder build = new StringBuilder();
        for (int i = 0; i < size; i++) {
            build.append((char)inpSt.read());
        }
        return build.toString();
    }

    // Метод writeFile - записывает в file из InputStream inpSt
    // без получения размера файла в сообщении
    public void writeFile(File file, InputStream inpSt){
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
    public String readCommand(InputStream inpSt) throws IOException{
        BufferedReader buff = new BufferedReader(new InputStreamReader(inpSt));
        StringBuilder br = new StringBuilder();
        String m;
        while ((m = buff.readLine()) != null) {
            br.append(m);
        }
        return br.toString();
    }
}
