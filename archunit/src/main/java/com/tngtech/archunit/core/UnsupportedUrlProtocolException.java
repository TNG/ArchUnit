package com.tngtech.archunit.core;

import java.net.URL;

public class UnsupportedUrlProtocolException extends RuntimeException {
    public UnsupportedUrlProtocolException(URL url) {
        super("The protocol of the following URL is not (yet) supported: " + url);
    }
}
