package com.tngtech.archunit.core.importer.testexamples.methodresolution;

@SuppressWarnings("unused")
public class OnlyRightAncestorOverridesRootMethod {
    interface Root {
        void target();
    }

    interface Left extends Root {
    }

    interface Right extends Root {
        @Override
        @ExpectedMethod
        void target();
    }

    interface Child extends Left, Right {
    }

    void scenario() {
        Child child = null;
        child.target();
    }
}
