package com.tngtech.archunit.visual;

import com.google.gson.annotations.Expose;

class JsonFieldAccess {
    @Expose
    private String to;
    @Expose
    private String startCodeUnit;
    @Expose
    private String targetElement;

    JsonFieldAccess(String to, String startCodeUnit, String targetElement) {
        this.to = to;
        this.startCodeUnit = startCodeUnit;
        this.targetElement = targetElement;
    }
}
