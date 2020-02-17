package com.era.cloud.server;


import com.era.cloud.common.Request;
import com.era.cloud.common.UploadFile;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainHandler extends ChannelInboundHandlerAdapter {
private String rootDir = "server/ServerDir/";
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            if (msg instanceof Request) {
                Request request = (Request) msg;
                if (request.isCommand()) {
                    writeFileList(ctx);
                } else
                if (request.isMessage()) {
                    String message = request.getMessage();
                    System.out.println("От клиента: " + message);
                    // ответ сервера
                    Request mess = new Request();
                    mess.setMessage("Сервер получил сообщение: " + message);
                    ctx.writeAndFlush(mess);
                }
                else if (request.isFile()) {
                    writeFileInDir(request, ctx);
                }
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

    private void writeFileList(ChannelHandlerContext ctx) {
        StringBuilder builder = new StringBuilder();
        File dir = new File(rootDir);
        String[] files = dir.list();
        if (files != null) {
            for (String file : files)
                builder.append(file).append("\n");
        }
        Request mess = new Request();
        mess.setMessage(builder.toString());
        ctx.writeAndFlush(mess);
    }

    private void writeFileInDir(Request req, ChannelHandlerContext ctx) {
        UploadFile f = req.getFile();
        String fileName = f.getName();
        System.out.println("От клиента: получен файл " + fileName);
        String filePath = rootDir + fileName;
        try {
            File writeFile = new File(filePath);
            FileOutputStream out = new FileOutputStream(writeFile);
            out.write(f.getData()); // записали в файл
            // ответ сервера
            Request mess = new Request();
            mess.setMessage("Файл получен: " + fileName);
            ctx.writeAndFlush(mess);
        } catch (IOException ex) {ex.printStackTrace();}
    }
}

