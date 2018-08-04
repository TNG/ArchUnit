package com.tngtech.archunit.example.controller.three;

import com.tngtech.archunit.example.controller.one.UseCaseOneTwoController;

public class UseCaseThreeController {
    public static final String doSomethingThree = "doSomethingThree";

    public void doSomethingThree() {
        new UseCaseOneTwoController().doSomethingOne();
    }
}
