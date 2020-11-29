package com.era.cloud.common;


public class CommandMessage extends AbstractMessage {
    public static final int CMD_MSG_AUTH_OK = 4352567;
    public static final int CMD_MSG_FILE_DOWNLOAD = 1292567;
    public static final int CMD_MSG_FILES_LIST = 5642532;
    public static final int CMD_MSG_SERVER_DELETE_FILE = 4113577;

    private int type;
    private Object[] attachment;
    private String fileName;

// клнструкторы
    public CommandMessage(int type) {
        this.type = type;
    }

    public CommandMessage(int type, Object... attachment) {
        this.type = type;
        this.attachment = attachment;
    }

    public CommandMessage(int type, String fileName) {
        this.type = type;
        this.fileName = fileName;
    }

    public int getType() {
        return type;
    }

    public String getFileName() {
        return fileName;
    }

    public Object[] getAttachment() {
        return attachment;
    }

    public boolean is_AUTH_OK(){
        return type == CMD_MSG_AUTH_OK;
    }
    public boolean is_FILE_DOWNLOAD(){
        return type == CMD_MSG_FILE_DOWNLOAD;
    }
    public boolean is_FILES_LIST(){
        return type == CMD_MSG_FILES_LIST;
    }
    public boolean is_SERVER_DELETE_FILE(){
        return type == CMD_MSG_SERVER_DELETE_FILE;
    }


}
