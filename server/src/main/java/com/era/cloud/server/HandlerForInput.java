package com.era.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;


public class HandlerForInput extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        try {
            int k = buf.readByte();
            if (k == 17) {
                while (buf.isReadable()) {
                    System.out.print((char) buf.readByte());
                    System.out.flush();
                }
            } else if (k == 15) {
                int sizeName = buf.readByte();
                StringBuilder build = new StringBuilder();
                for (int i = 0; i < sizeName; i++) {
                    build.append((char)buf.readByte());
                }
                String strName = build.toString();
                System.out.println("Получен файл " + strName);
                String clientDir = "fileTo";
//                String clientFile = clientDir + strName;
                Path pp = Paths.get(clientDir, strName);
//                System.out.println(clientFile);
//                File file = new File(clientFile); // создание объекта File
                File file = new File(pp.toUri());
                System.out.println(file.toString() + " hhh");

                try {
                    FileOutputStream ft = new FileOutputStream(file);
                    int m;
                    while ((m = buf.readByte()) != -1){
                        ft.write(m);
                    }
                } catch (IOException e){
                    e.printStackTrace();
                }
            }

        } finally {
            ReferenceCountUtil.release(msg);
        }

            ctx.write("YYYYYYYYYYYYYY");
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
