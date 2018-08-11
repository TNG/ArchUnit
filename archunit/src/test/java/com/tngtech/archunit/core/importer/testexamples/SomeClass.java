package com.tngtech.archunit.core.importer.testexamples;

public class SomeClass {
    SomeEnum other;

    public SomeClass(String irrelevant, SomeEnum other) {
        this.other = other;
    }

    void methodWithSomeEnumParameter(String irrelevant, SomeEnum someEnum) {
    }

    SomeEnum methodWithSomeEnumReturnType(String irrelevant) {
        return SomeEnum.OTHER_VALUE;
    }

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
