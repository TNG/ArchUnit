package com.tngtech.archunit.example.layers;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Stateless;

@Stateless
@Local(SomeBusinessInterface.class)
public class ClassViolatingSessionBeanRules implements SomeBusinessInterface {
    public static final String CONSTANT = "CONSTANT";
    private final String CONSTANT_FROM_CONSTRUCTOR;
    private String setOnConstruction;
    String state;

    public ClassViolatingSessionBeanRules() {
        CONSTANT_FROM_CONSTRUCTOR = "CONSTANT_FROM_CONSTRUCTOR";
    }

    @PostConstruct
    private void init() {
        setOnConstruction = "setOnConstruction";
    }

    public void setState(String state) { // Violating rule not to change state after construction
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public String getConstantFromConstructor() {
        return CONSTANT_FROM_CONSTRUCTOR;
    }

    public String getStringFromPostConstruct() {
        return setOnConstruction;
    }
}
