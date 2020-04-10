package com.tngtech.archunit.core.importer.testexamples.methodimport;

public class ClassWithMultipleMethods {

    static String usage = "ClassFileImporterTest's @Test imports_methods_with_correct_sourceCodeLocation";

    int methodDefinedInLine7() { return 7; }

    void methodWithBodyStartingInLine10() {
        System.out.println(10);
        System.out.println(11);
        System.out.println(12);
    }

    void emptyMethodDefinedInLine15() { }

    void emptyMethodEndingInLine19() {

    }

    public static class InnerClass {

        void methodWithBodyStartingInLine24() {
            new Runnable() {
                @Override
                public void run() {
                    System.out.println(27);
                }
            };
        }
    }
}
