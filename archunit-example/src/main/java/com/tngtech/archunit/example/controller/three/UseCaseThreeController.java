package com.tngtech.archunit.example.controller.three;

import com.tngtech.archunit.example.controller.one.UseCaseOneController;

public class UseCaseThreeController {
    public static final String doSomethingThree = "doSomethingThree";

    public void doSomethingThree() {
        new UseCaseOneController().doSomethingOne();
    }
}
