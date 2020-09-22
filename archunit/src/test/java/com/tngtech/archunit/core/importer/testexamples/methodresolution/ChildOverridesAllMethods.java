package com.tngtech.archunit.core.importer.testexamples.methodresolution;

@SuppressWarnings("unused")
public class ChildOverridesAllMethods {
    interface Root {
        void target();
    }

    interface Left extends Root {
        @Override
        void target();
    }

    interface Right extends Root {
        @Override
        void target();
    }

    interface Child extends Left, Right {
        @Override
        @ExpectedMethod
        void target();
    }

    void scenario() {
        Child child = null;
        child.target();
    }
}
