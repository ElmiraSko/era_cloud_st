package com.era.cloud.common;


public class ServerListMessage extends AbstractMessage {
    private String[] filesList;

    public ServerListMessage(String[] list) {
        filesList = list;
    }

    public String[] getFilesList() {
        return filesList;
    }
}
