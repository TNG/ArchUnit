package com.tngtech.archunit.junit;

import java.net.URL;

public interface UrlFilter {
    boolean accept(URL url);

    class NoOp implements UrlFilter {
        @Override
        public boolean accept(URL url) {
            return true;
        }
    }
}
