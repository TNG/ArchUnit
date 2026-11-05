package com.tngtech.archunit.core.importer;

import java.util.function.ToIntFunction;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassFileImporterTypeSwitchJavaTest {
    @Test
    void imports_class_labels_from_a_compiled_java_pattern_switch() {
        JavaClasses classes = new ClassFileImporter().importClasses(SwitchUser.class, SampleData.class);
        JavaClass origin = classes.get(SwitchUser.class);

        assertThat(origin.getMethod("classify", Object.class).getTypeSwitchChecks())
                .extracting(check -> check.getRawType().getName())
                .containsExactlyInAnyOrder(String.class.getName(), SampleData.class.getName());
        assertThat(classes.get(SampleData.class).getDirectDependenciesToSelf())
                .extracting(Dependency::getOriginClass).contains(origin);
        assertThat(origin.getInstanceofChecks()).isEmpty();
        assertThat(origin.getReferencedClassObjects()).isEmpty();
        assertThat(SwitchUser.classify(new SampleData())).isEqualTo(1);
    }

    @Test
    void imports_class_labels_from_a_compiled_java_pattern_switch_in_a_lambda() {
        JavaClass origin = new ClassFileImporter().importClass(SwitchUser.class);
        JavaMethod method = origin.getMethod("classifier");

        assertThat(method.getTypeSwitchChecks()).singleElement().satisfies(check -> {
            assertThat(check.getOwner()).isSameAs(method);
            assertThat(check.getRawType().getName()).isEqualTo(SampleData.class.getName());
            assertThat(check.isDeclaredInLambda()).isTrue();
        });
        assertThat(SwitchUser.classifier().applyAsInt(new SampleData())).isEqualTo(1);
    }

    static class SampleData {
    }

    static class SwitchUser {
        static int classify(Object value) {
            return switch (value) {
                case String ignored -> 0;
                case SampleData ignored -> 1;
                default -> -1;
            };
        }

        static ToIntFunction<Object> classifier() {
            return value -> switch (value) {
                case SampleData ignored -> 1;
                default -> -1;
            };
        }
    }
}
