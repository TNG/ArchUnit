package com.tngtech.archunit.core.importer.testexamples.innerclassimport;

public class ClassWithInnerClass {
    void callInsideOfAnonymous() {
        final CalledClass calledClass = null;

        new CanBeCalled() {
            @Override
            public void call() {
                calledClass.doIt();
            }
        };
    }

    public class Inner implements CanBeCalled {
        private CalledClass calledClass;

        @Override
        public void call() {
            calledClass.doIt();
        }
    }

    public interface CanBeCalled {
        void call();
    }
}
