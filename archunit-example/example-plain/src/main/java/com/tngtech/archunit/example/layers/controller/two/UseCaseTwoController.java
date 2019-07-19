package com.tngtech.archunit.example.layers.controller.two;

import com.tngtech.archunit.example.layers.AbstractController;
import com.tngtech.archunit.example.layers.controller.one.UseCaseOneTwoController;

public class UseCaseTwoController extends AbstractController {
    public static final String doSomethingTwo = "doSomethingTwo";

    public void doSomethingTwo() {
        new UseCaseOneTwoController().doSomethingOne();
    }
}
