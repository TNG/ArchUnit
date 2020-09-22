package com.tngtech.archunit.core.importer.testexamples.methodresolution;

@SuppressWarnings("unused")
public class OnlyLeftAncestorOverridesRootMethod {
    interface Root {
        void target();
    }

    interface Left extends Root {
        @Override
        @ExpectedMethod
        void target();
    }

    interface Right extends Root {
    }

    interface Child extends Left, Right {
    }

    void scenario() {
        Child child = null;
        child.target();
    }
}
