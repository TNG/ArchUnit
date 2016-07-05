package com.tngtech.archunit.example.controller.two;

import com.tngtech.archunit.example.controller.one.UseCaseOneController;

public class UseCaseTwoController {
    public static final String doSomethingTwo = "doSomethingTwo";

    public void doSomethingTwo() {
        new UseCaseOneController().doSomethingOne();
    }
}
