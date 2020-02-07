// 1. Для сетевого взаимодействия использовала java.io
// 2. Пока не ясно понимаю, как передавать большие файлы. Наверное, надо их разбивать на порции,
//     так как большой файл не поместится в массив byte[].
// 3. При нахождении длины файла (file.length()) приходится делать приведение типов long > int,
//    что не очень хорошо.
// 4. Передача по протоколу лучше, чем сериализация
// =======================================================================================================
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
            int fileSize = (int)file.length(); // размер файла

            CloudPackage pack = new CloudPackage(fileName, fileNameLength, fileSize, 15);
            ds.write(pack.dataFromTheFile(file)); // отправка на сервер

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
//=======================================================================================================================

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
//============================================================================================================

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
            //inpST - входной байтовый поток от клиента
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

                String command = pack.readCommand(inpST); // строковое представление команды
                System.out.println(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
