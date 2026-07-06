package com.tngtech.archunit.core.importer.testexamples.trycatch;

import java.io.Serializable;

@SuppressWarnings("unused")
public class ClassWithComplexTryCatchBlocks {
    ClassWithComplexTryCatchBlocks() {
        ClassHoldingMethods instanceOne = new ClassHoldingMethods();
        Object someOtherObject = new Object();
        Serializable someSerializable = new Serializable() {
        };
        instanceOne.setSomeInt(2);
        try {
            instanceOne.setSomeInt(1);
            someOtherObject.toString();
            try {
                instanceOne.doSomething();
            } catch (IllegalArgumentException | UnsupportedOperationException | CatchClauseTargetException e) {
            }
        } catch (Exception e) {
        } finally {
        }
        instanceOne.someInt = 5;

        try {
            instanceOne.setSomeString("hello");
        } catch (Throwable e) {
        }
    }
}
