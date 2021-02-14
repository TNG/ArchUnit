package com.tngtech.archunit.lang.syntax.elements.testclasses.innerclassaccess;

public class ClassWithInnerClasses {
    public class ClassAccessingInnerMemberClass {
        @SuppressWarnings("unused")
        void access() {
            new InnerMemberClassBeingAccessed();
        }
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    public class InnerMemberClassBeingAccessed {
    }
}
