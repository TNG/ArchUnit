package com.tngtech.archunit.core.domain;

import org.junit.Test;

import static com.google.common.collect.Sets.union;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaCodeUnitTest {

    @Test
    public void offers_all_calls_from_Self() {
        JavaMethod method = importClassWithContext(ClassAccessingOtherClass.class).getMethod("access", ClassBeingAccessed.class);

        assertThat(method.getCallsFromSelf())
                .hasSize(4)
                .containsOnlyElementsOf(union(method.getConstructorCallsFromSelf(), method.getMethodCallsFromSelf()));
    }

    @Test
    public void offers_all_accesses_from_Self() {
        JavaMethod method = importClassWithContext(ClassAccessingOtherClass.class).getMethod("access", ClassBeingAccessed.class);

        assertThat(method.getAccessesFromSelf())
                .hasSize(6)
                .containsOnlyElementsOf(union(union(
                        method.getConstructorCallsFromSelf(),
                        method.getMethodCallsFromSelf()),
                        method.getFieldAccesses()));
    }

    @SuppressWarnings("unused")
    private static class ClassAccessingOtherClass {
        void access(ClassBeingAccessed classBeingAccessed) {
            new ClassBeingAccessed();
            new ClassBeingAccessed("");
            classBeingAccessed.field1 = "";
            classBeingAccessed.field2 = null;
            classBeingAccessed.method1();
            classBeingAccessed.method2();
        }
    }

    private static class ClassBeingAccessed {
        String field1;
        Object field2;

        ClassBeingAccessed() {
        }

        ClassBeingAccessed(String field1) {
            this.field1 = field1;
        }

        void method1() {
        }

        void method2() {
        }
    }
}