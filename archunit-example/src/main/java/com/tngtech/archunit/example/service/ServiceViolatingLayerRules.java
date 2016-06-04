package com.tngtech.archunit.example.service;

import com.tngtech.archunit.example.controller.one.UseCaseOneController;
import com.tngtech.archunit.example.controller.two.UseCaseTwoController;

public class ServiceViolatingLayerRules {
    public static final String illegalAccessToController = "illegalAccessToController";
    public static final String doSomething = "doSomething";

    void illegalAccessToController() {
        System.out.println(UseCaseOneController.someString);
        UseCaseTwoController otherController = new UseCaseTwoController();
        otherController.doSomething();
    }

    public void doSomething() {
    }
}
