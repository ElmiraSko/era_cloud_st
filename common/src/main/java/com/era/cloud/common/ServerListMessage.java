package com.era.cloud.common;


import java.util.ArrayList;

public class ServerListMessage extends AbstractMessage {
    private ArrayList<String> filesList;

    public ServerListMessage(ArrayList<String> filesList) {
        this.filesList = filesList;
    }

    public ArrayList<String> getFilesList() {
        return filesList;
    }
}
