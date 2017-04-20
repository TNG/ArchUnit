package com.tngtech.archunit.visual;

import com.google.gson.annotations.Expose;

class JsonMethodCall {
    @Expose
    private String to;
    @Expose
    private String startCodeUnit;
    @Expose
    private String targetElement;

    JsonMethodCall(String to, String startCodeUnit, String targetElement, String... params) {
        this.to = to;
        this.startCodeUnit = startCodeUnit;
        this.targetElement = targetElement; // + getParams(params);
    }

    private String getParams(String[] params) {
        String res = "(";
        boolean first = true;
        for (String s : params) {
            if (!first) {
                res += ", ";
            }
            res += s;
            first = false;
        }
        return res + ")";
    }
}
