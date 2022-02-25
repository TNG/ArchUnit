package com.tngtech.archunit.core.importer.testexamples.trycatch;

@SuppressWarnings("unused")
public class ClassWithTryCatchBlockWithoutThrowables {
    void method() {
        try {
            new Object();
        } finally {
            System.out.println("finally");
        }
    }
}
