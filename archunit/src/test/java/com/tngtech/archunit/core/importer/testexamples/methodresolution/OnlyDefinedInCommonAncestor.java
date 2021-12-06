package com.tngtech.archunit.core.importer.testexamples.methodresolution;

@SuppressWarnings("unused")
public class OnlyDefinedInCommonAncestor {
    interface Root {
        @ExpectedMethod
        void target();
    }

    interface Left extends Root {
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
