package com.tngtech.archunit.example.controller.one;

import com.tngtech.archunit.example.controller.three.UseCaseThreeController;

@SuppressWarnings("unused")
public class UseCaseOneThreeController {
    public static final String doSomethingOne = "doSomethingOne";

    public void doSomethingOne() {
        SomeEnum dispatchMode = getDispatchMode();
        switch (dispatchMode) {
            case DISPATCH:
                new UseCaseThreeController().doSomethingThree();
                break;
            case DO_NOT_DISPATCH:
            default:
        }
    }

    private SomeEnum getDispatchMode() {
        return SomeEnum.DISPATCH;
    }
}
