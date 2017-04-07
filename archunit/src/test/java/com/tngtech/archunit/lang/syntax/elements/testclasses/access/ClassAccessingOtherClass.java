package com.tngtech.archunit.lang.syntax.elements.testclasses.access;

import com.tngtech.archunit.lang.syntax.elements.testclasses.accessed.ClassBeingAccessedByOtherClass;

public class ClassAccessingOtherClass {
    void call() {
        new ClassBeingAccessedByOtherClass();
    }
}
