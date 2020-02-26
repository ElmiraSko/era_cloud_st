package com.era.cloud.client;

import com.era.cloud.common.SimpleMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ReadMessFromServerTask implements Task{
    private ObjectDecoderInputStream in;
    private String messFromServer;
    private CountDownLatch cdl;

    ReadMessFromServerTask(ObjectDecoderInputStream in, String messFromServer, CountDownLatch cdl) {
        this.in = in;
        this.messFromServer = messFromServer;
        this.cdl = cdl;
    }

    @Override
    public void doing() {
        try {
            Object obj = in.readObject();
            if (obj instanceof SimpleMessage) {
                messFromServer = ((SimpleMessage)obj).getMessage();
                System.out.println(messFromServer);
                cdl.countDown();
            }
        } catch (IOException | ClassNotFoundException  ex){ex.printStackTrace();}
    }
}
