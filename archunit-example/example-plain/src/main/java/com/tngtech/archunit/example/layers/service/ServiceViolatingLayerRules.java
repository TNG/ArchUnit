package com.tngtech.archunit.example.layers.service;

import com.tngtech.archunit.example.layers.MyService;
import com.tngtech.archunit.example.layers.controller.ComplexControllerAnnotation;
import com.tngtech.archunit.example.layers.controller.SimpleControllerAnnotation;
import com.tngtech.archunit.example.layers.controller.SomeGuiController;
import com.tngtech.archunit.example.layers.controller.one.SomeEnum;
import com.tngtech.archunit.example.layers.controller.one.UseCaseOneTwoController;
import com.tngtech.archunit.example.layers.controller.two.UseCaseTwoController;
import com.tngtech.archunit.example.layers.security.Secured;

@MyService
@ComplexServiceAnnotation(
        controllerAnnotation = @ComplexControllerAnnotation(simpleControllerAnnotation = @SimpleControllerAnnotation),
        simpleServiceAnnotation = @SimpleServiceAnnotation,
        controllerEnum = SomeEnum.DISPATCH,
        serviceType = ServiceType.STANDARD
)
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

    @Secured
    public void properlySecured() {
    }
}
