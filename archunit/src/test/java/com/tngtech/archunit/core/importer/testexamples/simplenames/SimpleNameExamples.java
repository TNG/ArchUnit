package com.tngtech.archunit.core.importer.testexamples.simplenames;

import java.io.Serializable;

@SuppressWarnings("unused")
public class SimpleNameExamples {
    void simpleNames() {
        class Crazy$LocalClass {
        }

        Serializable anonymousClass = new Serializable() {
        };
    }

    public static class Crazy$InnerClass$$LikeAByteCodeGenerator_might_create {
        void simpleNames() {
            class Crazy$$NestedLocalClass {
            }

            Serializable nestedAnonymousClass = new Serializable() {
            };
        }

        public class NestedInnerClass$Also$$_crazy {
        }
    }
}
