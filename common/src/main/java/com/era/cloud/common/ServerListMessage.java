package com.era.cloud.common;

import java.io.Serializable;

public class ServerListMessage extends Request {
    private String[] filesList;

    public ServerListMessage(String[] list) {
        filesList = list;
    }

    public String[] getFilesList() {
        return filesList;
    }
}
