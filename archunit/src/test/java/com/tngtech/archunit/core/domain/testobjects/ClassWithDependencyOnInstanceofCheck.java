package com.tngtech.archunit.core.domain.testobjects;

@SuppressWarnings({"unused", "ConstantConditions"})
public class ClassWithDependencyOnInstanceofCheck {

    private static final boolean check = new Object() instanceof InstanceOfCheckTarget;

    ClassWithDependencyOnInstanceofCheck(Object o) {
        System.out.println(o instanceof InstanceOfCheckTarget);
    }

    boolean method(Object o) {
        return o instanceof InstanceOfCheckTarget;
    }

    public static class InstanceOfCheckTarget {
    }
}
