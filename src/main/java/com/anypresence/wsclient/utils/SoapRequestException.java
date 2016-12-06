package com.anypresence.wsclient.utils;

public class SoapRequestException extends Exception {
    public SoapRequestException(String message) {
        super(message);
    }

    public SoapRequestException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
