package com.tngtech.archunit.core.importer.testexamples.trycatch;

@SuppressWarnings("unused")
public class ClassWithSimpleTryCatchBlocks {
    Object method() {
        try {
            new Object();
        } catch (IllegalStateException e) {
            System.out.println("Error");
        }

        try {
            return new Object();
        } catch (IllegalStateException e) {
            System.out.println("Error1");
        } catch (IllegalArgumentException e) {
            System.out.println("Error2");
        } catch (CatchClauseTargetException e) {
            System.out.println("Error3");
        } finally {
            System.out.println("finally");
        }
        throw new IllegalStateException();
    }
}
