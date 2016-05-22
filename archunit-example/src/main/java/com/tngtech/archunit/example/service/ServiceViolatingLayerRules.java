package com.tngtech.archunit.example.service;

import com.tngtech.archunit.example.usecase.one.UseCaseOneController;
import com.tngtech.archunit.example.usecase.two.UseCaseTwoController;

public class ServiceViolatingLayerRules {
    public static final String illegalAccessToUseCase = "illegalAccessToUseCase";

    void illegalAccessToUseCase() {
        System.out.println(UseCaseOneController.someString);
        UseCaseTwoController otherController = new UseCaseTwoController();
        otherController.doSomething();
    }
}
