package com.era.cloud.client;

import com.era.cloud.common.Request;
import com.era.cloud.common.UploadFile;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.File;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class ClientSerial {
    public static void main(String[] args) throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);

        Socket socket = new Socket("localhost", 8189);
        ObjectEncoderOutputStream out = new ObjectEncoderOutputStream(socket.getOutputStream());
        ObjectDecoderInputStream in = new ObjectDecoderInputStream(socket.getInputStream(), 100 * 1024 * 1024);
        Request mess = new Request();
//        File file = new File("fileFrom/star.png");
//        File file = new File("fileFrom/5.png");
        File file = new File("fileFrom/2.txt");
        mess.setFile(file);
        out.writeObject(mess);
        out.flush();
        System.out.println("Отправили файл на сервер, ждем ответ");
        Object obj = in.readObject();
        if (obj instanceof Request) {
            if (((Request) obj).isMessage()) {
                System.out.println(((Request) obj).getMessage());
            }
        }
        mess = new Request();
        mess.setMessage("ррррррррррррррррррррр");
        out.writeObject(mess);
        out.flush();
//        new Thread(() -> {
//                while (true) {}
//        }).start();
        cdl.await();
        in.close();
        out.close();
        socket.close();
        System.out.println("end");
    }
}
