package com.tngtech.archunit.core.importer.testexamples.innerclassimport;

@SuppressWarnings("unused")
public class GenericClassWithInnerClass<T> {

    public class Middle {
        private T middleField;

        public class Inner {
        }

        public class GenericInner<U> {
        }
    }

    public class GenericMiddle<U extends T> {
        private U middleField;

        public class GenericInner<V> {

        }
    }
}
