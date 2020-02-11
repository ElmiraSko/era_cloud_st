package com.era.cloud.common;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class CloudPackage {

    private Path path;

    private String text;
    private int textLength;

//Конструкторы
    public CloudPackage() {}

    public CloudPackage(Path path) {
        this.path = path;
    }

    public CloudPackage(String text, int textLength) {
        this.text = text;
        this.textLength = textLength;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTextLength(int textLength) {
        this.textLength = textLength;
    }

    //метод для передачи файла в поток OutputStream outp
    //(сигнальный байт, длина имени файла, имя файла, размер файла, содержимое файла)
    public void dataFromTheFile(Path path, OutputStream outp) throws IOException {
        String fileName = path.getFileName().toString(); // получили имя файла
        int fileNameLength = fileName.length(); // длина имени файла
        int fileSize = (int) Files.size(path);// размер файла, здесь пробрасывается исключение

        outp.write(15);  // сигнальный байт
        outp.write(fileNameLength); // длина имени файла
        outp.write(fileName.getBytes()); // байты имени файла
        outp.write(fileSize); // размер файла
        Files.copy(path, outp); // скопировали весь файл в поток
        System.out.println("Указанный файл отправлен на сервер: " + fileName);
    }

// из файла в файл
    public void fromFile(Path path){
        Path p = Paths.get("fileTo/TTT.png");
        try {
            String s2 = "fileTo/TTT.png";
            System.out.println(path.toString() + "00000");
            FileInputStream in = new FileInputStream(path.toString());
            FileChannel fileChannel = in.getChannel(); // получили канал
            long fileSize = fileChannel.size(); //
            FileOutputStream out = new FileOutputStream(s2);
            FileChannel outChannel1 = out.getChannel();
            fileChannel.transferTo(0, fileSize, outChannel1);

        } catch (IOException e) {e.printStackTrace();}

    }


//======================================================================================
    //Метод buildNameFile - возвращает имя файла полученного из входного потока inpSt
    // size - длина имени файла
    private String buildNameFile(InputStream inpSt) throws IOException{
        int sizeName = inpSt.read();
        StringBuilder build = new StringBuilder();
        for (int i = 0; i < sizeName; i++) {
            build.append((char)inpSt.read());
        }
        return build.toString();
    }

    // Метод writeFile - записывает в файл из InputStream inpSt
    public void writeFile1(String sPath, InputStream inpSt) throws IOException{
        StringBuilder file = new StringBuilder(sPath); // каталоговый путь
        String fileName = buildNameFile(inpSt); // получили имя файла
        System.out.println("Получен файл " + fileName);
        file.append(fileName);
        int size = inpSt.read(); // считываем размер файла
        Path path = Paths.get(file.toString()); // создади путь
        Files.copy(inpSt, path, REPLACE_EXISTING); // скопировали из входного потока в файл
    }
//====================================================================================
    public void writeFile2(String sPath, InputStream inpSt) throws IOException{
        StringBuilder file = new StringBuilder(sPath); // путь для файла
        String fileName = buildNameFile(inpSt); // получили имя файла
        System.out.println("Получен файл " + fileName);
        file.append(fileName);
        int size = inpSt.read(); // считываем размер файла
        try {
            FileOutputStream ft = new FileOutputStream(file.toString());
            int m;
            while ((m = inpSt.read()) != -1){
                ft.write(m);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
//============================================================================
    // Метод записывает в файл из InputStream inpSt
    public void writeFileChannel(InputStream inpSt) throws IOException{
        String strName = buildNameFile(inpSt); // получили имя файла
        System.out.println("Получен файл " + strName);
        int size = inpSt.read(); // считываем размер файла
        System.out.println(size + " -размер файла");
        Path path = Paths.get(strName); // создади путь
        Files.copy(inpSt, path, REPLACE_EXISTING); // скопировали из входного потока в файл
    }


    // Формирует массив байтов для команды
    // String str - текст команды
    public byte[] command(String str) {
        byte[] strByte = str.getBytes();
        byte[] command = new byte[strByte.length + 1]; // итоговый массив
        command[0] = 17; // сигнальный байт
        System.arraycopy(strByte, 0, command, 1, strByte.length);
        return command;
    }
 public void sendMessage(String message) {

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
