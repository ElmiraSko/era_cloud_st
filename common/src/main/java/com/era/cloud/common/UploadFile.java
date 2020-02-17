package com.era.cloud.common;

import java.io.*;

public class UploadFile implements Serializable {

    private String name;
    private File file;
    private byte[] data;
    private int size;

//    private int partNumber;
//    private int partsCount;
//
    UploadFile(File file) {
        this.file = file;
        name = file.getName();
        size = (int)file.length();
        data = new byte[size];
        try {
            FileInputStream inp = new FileInputStream(file);
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) inp.read();
//                System.out.print((char) data[i]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public byte[] getData() {
        return data;
    }
}
