package com.era.cloud.server;


import com.era.cloud.common.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;

import java.io.*;
import java.sql.Connection;


public class MainHandler extends ChannelInboundHandlerAdapter {

    ConnectDB conDB;
    private String loginUser;
    private final int MAX_SIZE = 1024*1024*100;
    private String rootDirectory = "server/ServerDir/";
    private String userDirectory;



   public MainHandler(ConnectDB conDB) {
       super();
       this.conDB = conDB;
   }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {


        try {
            if(msg instanceof CommandMessage) { // если команда
                CommandMessage com = (CommandMessage) msg;
                if (com.is_AUTH_OK()) {
                    Object[] objects = com.getAttachment();
                    if (objects[0] instanceof LoginAndPasswordMessage) {
                        LoginAndPasswordMessage log_pass = (LoginAndPasswordMessage)objects[0];
                        String log = log_pass.getLogin();
                        String pass = log_pass.getPassword();
                        loginUser = conDB.authorize(log, pass);
                        if (loginUser != null) {
                            SimpleMessage mess = new SimpleMessage("OK");
                            ctx.writeAndFlush(mess);

                            userDirectory = rootDirectory + loginUser;
                            System.out.println(userDirectory);
                            File dir = new File(userDirectory);
                            if (dir.isDirectory()) { // если по указанному пути есть каталог
                                userDirectory += "/";
                            } else {
                                boolean created = dir.mkdir(); // создали новый коталог
                                if(created){userDirectory += "/";}
                            }
                        }
                        else {SimpleMessage mess = new SimpleMessage("NO");
                            ctx.writeAndFlush(mess);}
                    }
                }
                if (com.is_FILES_LIST())
                    writeFileList(ctx);
                if (com.is_FILE_DOWNLOAD()) {
                    String str = com.getStr();
                    writeFile(str, ctx);
                }
            } else
            if (msg instanceof UploadFile) { // если файл
                UploadFile file = (UploadFile) msg;
                writingFileToStorage(file);
            }
            else
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
    private void writeFileList(ChannelHandlerContext ctx) {
        File dir = new File(userDirectory);
        ServerListMessage serList = new ServerListMessage(dir.list());
        ctx.writeAndFlush(serList);
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
            System.out.println(writeFile);
            FileOutputStream out = new FileOutputStream(writeFile, append);
            out.write(file.getData()); // записали в файл
            out.close();
        } catch (IOException ex) {ex.printStackTrace();}
        System.out.println("От клиента: получен файл " + fileName);
    }

    // передача файла
    private void writeFile(String fileName, ChannelHandlerContext ctx){
        File file = new File(userDirectory + fileName);
        int len = (int)file.length();
        UploadFile req = new UploadFile(file);
        int partNumber = 1;
        req.setPartNumber(partNumber);
        try {
            FileInputStream inp = new FileInputStream(file);
            BufferedInputStream buff = new BufferedInputStream(inp, MAX_SIZE);
            if (len <= MAX_SIZE) {
                req.setData(new byte[len]);
                buff.read(req.getData());
                ctx.writeAndFlush(req);
            } else { // если длина файла превышает ограничение на передаваемый объем
                while (len > 0) {  // пока все не считали
                    req.setPartNumber(partNumber); // устанавливаем номер посылки
                    req.setData(new byte[MAX_SIZE]); // обновляем массив для данных
                    if ((buff.available()) > 0) {
                        buff.read(req.getData()); // записываем в массив данные
                    }
                    len = len - MAX_SIZE; // уменьшаем длину файла
                    partNumber++;
                    ctx.writeAndFlush(req); // отправляем посылку
                }
            }
            buff.close();
        } catch (IOException e) {
            e.printStackTrace();
        }        System.out.println();
        System.out.println("Сервер отправил файл: " + fileName);
    }

}
