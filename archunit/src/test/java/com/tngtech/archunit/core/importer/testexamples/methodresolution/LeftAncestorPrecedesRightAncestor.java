package com.tngtech.archunit.core.importer.testexamples.methodresolution;

@SuppressWarnings("unused")
public class LeftAncestorPrecedesRightAncestor {
    interface Left {
        @ExpectedMethod
        void target();
    }

    interface Right {
        void target();
    }

    interface Child extends Left, Right {
    }

    void scenario() {
        Child child = null;
        child.target();
    }
}
