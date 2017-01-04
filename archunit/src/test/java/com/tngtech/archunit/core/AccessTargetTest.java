package com.tngtech.archunit.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.simulateCall;
import static org.assertj.core.api.Assertions.assertThat;

public class AccessTargetTest {
    @Test
    public void annotated_on_resolved_target() {
        JavaCall<?> call = simulateCall().from(Origin.class, "call").to(Target.class, "called");

        assertThat(call.getTarget().isAnnotatedWith(QueriedAnnotation.class))
                .as("target is annotated with @" + QueriedAnnotation.class.getSimpleName())
                .isTrue();
        assertThat(call.getTarget().isAnnotatedWith(Deprecated.class))
                .as("target is annotated with @" + Deprecated.class.getSimpleName())
                .isFalse();
    }

    @Test
    public void annotated_on_unresolved_target() {
        JavaCall<?> call = simulateCall().from(Origin.class, "call").toUnresolved(Target.class, "called");

        assertThat(call.getTarget().isAnnotatedWith(QueriedAnnotation.class))
                .as("target is annotated with @" + QueriedAnnotation.class.getSimpleName())
                .isFalse();
    }

    private static class Origin {
        private Target target;

        void call() {
            target.called();
        }
    }

    private static class Target {
        @QueriedAnnotation
        void called() {
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface QueriedAnnotation {
    }
}