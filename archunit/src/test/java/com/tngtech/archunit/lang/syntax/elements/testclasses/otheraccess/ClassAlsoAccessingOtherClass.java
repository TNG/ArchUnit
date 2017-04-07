package com.tngtech.archunit.lang.syntax.elements.testclasses.otheraccess;

import com.tngtech.archunit.lang.syntax.elements.testclasses.accessed.ClassBeingAccessedByOtherClass;

public class ClassAlsoAccessingOtherClass {
    void call() {
        new ClassBeingAccessedByOtherClass();
    }
}
