package com.tngtech.archunit.visual;

class JsonMethodCall {
    private String to;
    private String startCodeUnit;
    private String targetElement;

    JsonMethodCall(String to, String startCodeUnit, String targetElement) {
        this.to = to;
        this.startCodeUnit = startCodeUnit;
        this.targetElement = targetElement;
    }
}
