package com.era.cloud.common;

public class SimpleMessage extends AbstractMessage {
    private String message;
    public SimpleMessage(String mess) {
        message = mess;
    }

    public String getMessage() {
        return message;
    }
}
