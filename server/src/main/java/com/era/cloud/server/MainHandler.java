package com.era.cloud.server;


import com.era.cloud.common.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.*;


public class MainHandler extends ChannelInboundHandlerAdapter {
    private String login;
    private final int MAX_SIZE = 1024;
    private String rootDir = "server/ServerDir/";
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if(msg instanceof CommandMessage) { // если команда
                CommandMessage com = (CommandMessage) msg;
                if (com.is_AUTH_OK()) {
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
                writeFileInDir(file);
            }
            else
            if (msg instanceof Message) { // если сообщение
                String mes = ((Message) msg).getMessage();
                System.out.println("От клиента: " + mes);
                // ответ сервера
                Message mess = new Message("Сервер получил сообщение: " + mes);
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

    private void writeFileList(ChannelHandlerContext ctx) { // список файлов на сервере
        File dir = new File(rootDir);
        Request serList = new ServerListMessage(dir.list());
        ctx.writeAndFlush(serList);
    }

    // запись файла на диск
    private void writeFileInDir(UploadFile file) {
        boolean append = true;
        String fileName = file.getName();
        String filePath = rootDir + fileName;
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

    // передача файла
    private void writeFile(String fileName, ChannelHandlerContext ctx){
        File file = new File(rootDir + fileName);
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
