package com.tngtech.archunit.core.domain.testobjects;

@SuppressWarnings({"unused"})
public class AThrowingBException {
    public AThrowingBException() throws BException1 {
        throw new BException1();
    }

    static void throwingBException2() throws BException2 {
        throw new BException2();
    }

    public void throwingBException3() throws BException3 {
        throw new BException3();
    }
}
