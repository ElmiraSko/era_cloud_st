package com.era.cloud.common;

import java.io.File;
import java.io.Serializable;

public class UploadFile extends Request {

    private String name;
    private byte[] data;
    private int size; // удалить

    private int partNumber; // номер части

    public UploadFile(File file) {
        name = file.getName();
        size = (int)file.length();
    }

    public int getPartNumber() {
        return partNumber;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }
}
