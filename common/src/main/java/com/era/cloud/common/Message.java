package com.era.cloud.common;

public class Message extends Request {
    private String message;
    public Message(String mess) {
        message = mess;
    }

    public String getMessage() {
        return message;
    }

}
