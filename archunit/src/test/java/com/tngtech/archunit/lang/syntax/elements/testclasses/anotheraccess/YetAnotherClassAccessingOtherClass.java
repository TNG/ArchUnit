package com.tngtech.archunit.lang.syntax.elements.testclasses.anotheraccess;

import com.tngtech.archunit.lang.syntax.elements.testclasses.accessed.ClassBeingAccessedByOtherClass;

public class YetAnotherClassAccessingOtherClass {
    void call() {
        new ClassBeingAccessedByOtherClass();
    }
}
