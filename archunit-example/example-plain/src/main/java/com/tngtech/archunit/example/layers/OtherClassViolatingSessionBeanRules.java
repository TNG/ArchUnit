package com.tngtech.archunit.example.layers;

public class OtherClassViolatingSessionBeanRules {
    public static final String init = "init";

    private ClassViolatingSessionBeanRules bean;

    public OtherClassViolatingSessionBeanRules(ClassViolatingSessionBeanRules bean) {
        this.bean = bean;
    }

    public void init() {
        bean.state = "initialized";
    }
}
