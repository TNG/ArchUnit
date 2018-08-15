package com.tngtech.archunit.example.service;

import com.tngtech.archunit.example.MyService;
import com.tngtech.archunit.example.controller.SomeGuiController;
import com.tngtech.archunit.example.controller.one.UseCaseOneTwoController;
import com.tngtech.archunit.example.controller.two.UseCaseTwoController;

@MyService
public class ServiceViolatingLayerRules {
    public static final String illegalAccessToController = "illegalAccessToController";
    public static final String doSomething = "doSomething";
    public static final String dependentMethod = "dependentMethod";

    void illegalAccessToController() {
        System.out.println(UseCaseOneTwoController.someString);
        UseCaseTwoController otherController = new UseCaseTwoController();
        otherController.doSomethingTwo();
    }

    public void doSomething() {
    }

    public SomeGuiController dependentMethod(UseCaseTwoController otherController) {
        return null;
    }
}
