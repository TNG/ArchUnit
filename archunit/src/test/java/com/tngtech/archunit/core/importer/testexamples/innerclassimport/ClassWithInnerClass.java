package com.tngtech.archunit.core.importer.testexamples.innerclassimport;

@SuppressWarnings("unused")
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

    void callInsideOfLocalClass() {
        final CalledClass calledClass = null;

        class LocalCaller {
            void call() {
                calledClass.doIt();
            }
        }
    }

    public class Inner implements CanBeCalled {
        private CalledClass calledClass;

        @Override
        public void call() {
            calledClass.doIt();
        }

        void accessOuterClass() {
            System.out.println("Can access outer instance: " + ClassWithInnerClass.this.toString());
        }
    }

    public static class NestedStatic implements CanBeCalled {
        private CalledClass calledClass;

        @Override
        public void call() {
            calledClass.doIt();
        }
    }

    public interface ImplicitlyNestedStatic {
    }

    public interface CanBeCalled {
        void call();
    }
}
