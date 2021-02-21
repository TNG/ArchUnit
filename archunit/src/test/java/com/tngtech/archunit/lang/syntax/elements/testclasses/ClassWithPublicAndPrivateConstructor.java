package com.tngtech.archunit.lang.syntax.elements.testclasses;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ClassWithPublicAndPrivateConstructor {
    public ClassWithPublicAndPrivateConstructor(String s) {
    }

    private ClassWithPublicAndPrivateConstructor(Integer i) {
        this(i.toString());
    }
}
