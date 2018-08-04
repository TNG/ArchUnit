package com.tngtech.archunit.example.controller.one;

import com.tngtech.archunit.example.controller.three.UseCaseThreeController;

public class UseCaseOneThreeController {
    public static final String doSomethingOne = "doSomethingOne";

    public void doSomethingOne() {
        new UseCaseThreeController().doSomethingThree();
    }
}
