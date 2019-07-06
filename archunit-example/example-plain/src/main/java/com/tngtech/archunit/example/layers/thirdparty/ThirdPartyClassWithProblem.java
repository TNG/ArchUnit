package com.tngtech.archunit.example.layers.thirdparty;

/**
 * This class simulates a problem with a third party class, where we can't influence the original code base.
 */
public class ThirdPartyClassWithProblem {
    public ThirdPartyClassWithProblem() { // We assume that this constructor causes problems and needs a workaround
    }
}
