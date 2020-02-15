package com.era.cloud.common;

import java.io.File;
import java.io.Serializable;

public class Request implements Serializable {
    private final int TYPE_MESSAGE = 0;
    private final int TYPE_COMMAND = 1;
    private final int TYPE_FILE = 2;
    private int type;
    private String message;
    private String command;
    private UploadFile file;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        type = TYPE_MESSAGE;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
        type = TYPE_COMMAND;
    }

    public UploadFile getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = new UploadFile(file);
        type = TYPE_FILE;
    }

    public boolean isMessage(){
        return type == TYPE_MESSAGE;
    }
    public boolean isCommand(){
        return type == TYPE_COMMAND;
    }
    public boolean isFile(){
        return type == TYPE_FILE;
    }

}
