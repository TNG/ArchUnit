package com.tngtech.archunit.core.importer.testexamples;

public class SomeClass {
    SomeEnum other;

    public class Inner {
        void foo() {
            other.bar();
        }

        public class InnerInner {
            void bar() {
                other.bar();
            }
        }
    }
}
