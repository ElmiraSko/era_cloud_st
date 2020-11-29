package com.era.cloud.server;

import com.era.cloud.common.*;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private ConnectDB conDB;
    private String loginUser;
    private final int MAX_SIZE = 1024*1024;
    private String rootDirectory = "server/ServerDir/";
    private String userDirectory;

   public MainHandler(ConnectDB conDB) {
       super();
       this.conDB = conDB;
   }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            if(msg instanceof CommandMessage) { // если полученный объект - команда
                CommandMessage command = (CommandMessage) msg;
                if (command.is_AUTH_OK()) { // если это команда аутентификации
                    Object[] objects = command.getAttachment();
                    if (objects[0] instanceof LoginAndPasswordMessage) {
                        LoginAndPasswordMessage log_pass = (LoginAndPasswordMessage)objects[0];
                        String log = log_pass.getLogin();
                        String pass = log_pass.getPassword();

                        if (log_pass.typeIsAUTH()){ // если авторизация
                            loginUser = conDB.authorize(log, pass); // есть ли в базе такой пользователь
                            if (loginUser != null) {
                                userDirectory = rootDirectory + loginUser + "/";
                                SimpleMessage mess = new SimpleMessage("OK");
                                ctx.writeAndFlush(mess);
                                System.out.println(userDirectory);
                            }
                            else {SimpleMessage mess = new SimpleMessage("NO");
                                ctx.writeAndFlush(mess);}
                        } else
                            if (log_pass.typeIsREG()) { // если регистрация, при регистрации сразу создаем папку
                                if (conDB.authorize(log, pass) == null) {   // если такого логина и пароля еще нет в базе, то
                                conDB.writeNewUser(log, pass); //добавляем нового пользователя
                                    SimpleMessage mess = new SimpleMessage("OK"); //отправляем OK
                                    ctx.writeAndFlush(mess);
                                    userDirectory = rootDirectory + log; // так как логины не повторяются, то содаем папку
                                    System.out.println(userDirectory);
                                    File dir = new File(userDirectory);
                                    boolean created = dir.mkdir(); // создали новый коталог
                                    if(created){userDirectory += "/";}
                                } else {
                                    SimpleMessage mess = new SimpleMessage("NO"); //отправляем NO
                                    ctx.writeAndFlush(mess);
                                }
                            }
                    }
                }
                if (command.is_FILES_LIST()) // если команда на формирование списка файлов
                    fileListToClient(ctx);
                if (command.is_FILE_DOWNLOAD()) { // команда на скачивание файла
                    System.out.println("Поступила команда на скачивание");
                    String fileName = command.getFileName();
                    sendFileToClient(fileName, ctx);
                }
                if (command.is_SERVER_DELETE_FILE()) { // если команда на удаление файла
                    String fileName = command.getFileName();
                    deleteFile(fileName);
                }
            } else
                if (msg instanceof UploadFile) { // если получен файл от клиента
                    UploadFile file = (UploadFile) msg;
                    System.out.println(" Пришел файл от клиента");
                    writingFileToStorage(file);

            } else
                if (msg instanceof SimpleMessage) { // если сообщение
                    String mes = ((SimpleMessage) msg).getMessage();
                    System.out.println("От клиента: " + mes);
                    // ответ сервера
                    SimpleMessage mess = new SimpleMessage("Сервер получил сообщение: " + mes);
                    ctx.writeAndFlush(mess);
            }

        }  finally {
            ReferenceCountUtil.release(msg);
        }


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }


    // список файлов на сервере
    private void fileListToClient(ChannelHandlerContext ctx) {
        File dir = new File(userDirectory);
        String[] filesArray = dir.list();
        ArrayList<String> files = new ArrayList<>();
        if (filesArray != null){
            files.addAll(Arrays.asList(filesArray));
        }
        ServerListMessage serverList = new ServerListMessage(files);
        ctx.writeAndFlush(serverList);
    }



    // запись файла на диск
    private void writingFileToStorage(UploadFile file) {
        boolean append = true;
        String fileName = file.getName();
        String filePath = userDirectory + fileName;
        if (file.getPartNumber() == 1) { // если файл состоит из одной части, то не дописываем
            append = false;
        }
        try {
            File writeFile = new File(filePath);
            FileOutputStream out = new FileOutputStream(writeFile, append);
            out.write(file.getData()); // записали в файл
            out.close();
        } catch (IOException ex) {ex.printStackTrace();}
        System.out.println("От клиента: получен файл " + fileName);
    }

    // передача файла клиенту
    private void sendFileToClient(String fileName, ChannelHandlerContext ctx){
        File file = new File(userDirectory + fileName);
        int len = (int)file.length();
        UploadFile req = new UploadFile(file);
        int partNumber = 1;
        try {
            if (len <= MAX_SIZE) {
                FileInputStream inp = new FileInputStream(file);
                BufferedInputStream buff = new BufferedInputStream(inp, len);
                req.setData(new byte[len]);
                req.setPartNumber(partNumber);
                req.setCountNumber(partNumber);
                buff.read(req.getData());
                ctx.writeAndFlush(req);
                buff.close();
            } else { // если длина файла превышает ограничение на передаваемый объем
                FileInputStream inp = new FileInputStream(file);
                BufferedInputStream buff = new BufferedInputStream(inp, MAX_SIZE);
                int countPart = len/MAX_SIZE + 1;
                req.setData(new byte[MAX_SIZE]);
                while (len > 0) {  // пока все не считали
                    if (len <= MAX_SIZE) {
                        req.setData(new byte[len]);
                    }
                    req.setPartNumber(partNumber); // устанавливаем номер посылки
                    req.setCountNumber(countPart);
                    if ((buff.available()) > 0) {
                        buff.read(req.getData()); // записываем в массив данные
                    }
                    len = len - MAX_SIZE; // уменьшаем длину файла
                    partNumber++;
                    ctx.writeAndFlush(req); // отправляем посылку
                }
                buff.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }        System.out.println();
        System.out.println("Сервер отправил файл: " + fileName);
    }

    private void deleteFile(String fileName){
        File file = new File(userDirectory + fileName);
        if (file.exists()) {
           boolean del = file.delete();
           if (del) System.out.println("Файл удален.");
           else System.out.println("Ошибка при удалении файла.");
        }
    }
}
