package com.era.cloud.server;


import com.era.cloud.common.Request;
import com.era.cloud.common.UploadFile;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

public class MainHandler extends ChannelInboundHandlerAdapter {
    String temp2;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof Request) {
//                if (((Request) msg).isMessage()) {
//                    System.out.println(((Request) msg).getMessage());
//                }
                if (((Request) msg).isFile()) {
                    UploadFile f = ((Request) msg).getFile();
                    String fileName = f.getName();
                    System.out.println("Получен файл " + fileName);
String temp = "C:\\Users\\Admin\\Documents\\Elmira Studying\\Github_2019\\era_cloud_st\\fileTo\\8.txt";
temp2 = fileName;
//                    File writeFile = new File(fileName);
//                    FileOutputStream out = new FileOutputStream(writeFile);
                    FileOutputStream out = new FileOutputStream(temp);
                    out.write(f.getData()); // записали в файл (массив data - содержимое переданного файла)
                }
                Request mess = new Request();
                mess.setMessage("Следующий файл получен и сохранен " + temp2);
                ctx.write(mess);
                ChannelFuture channelFuture = ctx.writeAndFlush(mess);

                if (((Request) msg).isMessage()) {
                    String n = ((Request) msg).getMessage();
                    System.out.println("================");
                    System.out.println(n);
                }



//                File file = new File("2.txt");
//                int bufSize = 1024 * 1024 * 10;
//                int partsCount = new Long(file.length() / bufSize).intValue();
//                if (file.length() % bufSize != 0) {
//                    partsCount++;
//                }
//                FileMessage fmOut = new FileMessage("file.dat", -1, partsCount, new byte[bufSize]);
//                FileInputStream in = new FileInputStream(file);
//                for (int i = 0; i < partsCount; i++) {
//                    int readedBytes = in.read(fmOut.getData());
//                    fmOut.setPartNumber(i + 1);
//                    if (readedBytes < bufSize) {
//                        fmOut.setData(Arrays.copyOfRange(fmOut.getData(), 0, readedBytes));
////                        data = Arrays.copyOfRange(fmOut.getData(), 0, readedBytes);
//                    }
//                    ChannelFuture channelFuture = ctx.writeAndFlush(fmOut);
//                    System.out.println("Отправлена часть #" + (i + 1));
//                }
//                in.close();
//            }
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
}

