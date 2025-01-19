package com.tngtech.archunit.junit.internal;

import java.lang.annotation.Retention;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.Predicates.alwaysTrue;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.field;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.method;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReflectionUtilsTest {

    @Test
    public void getAllFields() {
        Collection<Field> fields = ReflectionUtils.getAllFields(Child.class, named("field"));

        assertThat(fields).containsOnly(
                field(Child.class, "field"),
                field(UpperMiddle.class, "field"),
                field(LowerMiddle.class, "field"),
                field(Parent.class, "field")
        );
    }

    @Test
    public void getAllMethods() {
        Collection<Method> methods = ReflectionUtils.getAllMethods(Child.class, named("overrideMe"));

        assertThat(methods).containsOnly(
                method(Child.class, "overrideMe"),
                method(UpperMiddle.class, "overrideMe"),
                method(LowerMiddle.class, "overrideMe"),
                method(Parent.class, "overrideMe")
        );
    }

    @Test
    public void getAllMethods_of_interface() {
        assertThat(ReflectionUtils.getAllMethods(Subinterface.class, alwaysTrue()))
                .containsOnly(
                        method(SomeInterface.class, "foo"),
                        method(OtherInterface.class, "bar"));
    }

    @Test
    public void getAllFields_of_interface() {
        assertThat(ReflectionUtils.getAllFields(Subinterface.class, alwaysTrue()))
                .containsOnly(
                        field(SomeInterface.class, "SOME_CONSTANT"),
                        field(OtherInterface.class, "OTHER_CONSTANT"));
    }

    @Test
    public void findAnnotation_should_find_direct_annotation() {
        SomeAnnotation actual = ReflectionUtils.findAnnotation(DirectlyAnnotated.class, SomeAnnotation.class);

        assertThat(actual).isInstanceOf(SomeAnnotation.class);
        assertThat(actual.value()).as(SomeAnnotation.class.getSimpleName() + ".value()").isEqualTo("default");
    }

    @Test
    public void findAnnotation_should_find_meta_annotation() {
        SomeAnnotation actual = ReflectionUtils.findAnnotation(MetaAnnotated.class, SomeAnnotation.class);

        assertThat(actual).isInstanceOf(SomeAnnotation.class);
        assertThat(actual.value()).as(SomeAnnotation.class.getSimpleName() + ".value()").isEqualTo("Meta-Annotation");
    }

    @Test
    void findAnnotation_should_reject_annotation_missing_from_hierarchy() {
        assertThatThrownBy(() -> ReflectionUtils.findAnnotation(Object.class, SomeAnnotation.class))
                .isInstanceOf(ArchTestInitializationException.class)
                .hasMessage("Class %s is not (meta-)annotated with @%s", Object.class.getName(), SomeAnnotation.class.getSimpleName());
    }

    @Test
    void tryFindAnnotation_should_return_empty_when_annotation_missing_from_hierarchy() {
        assertThat(ReflectionUtils.tryFindAnnotation(Object.class, SomeAnnotation.class)).as("Optional.of(SomeAnnotation)").isEmpty();
    }

    @Test
    void findAnnotation_should_return_depth_first_result_of_multiple_annotations_in_hierarchy() {
        SomeAnnotation actual = ReflectionUtils.findAnnotation(AnnotationMultipleTimesInHierarchy.class, SomeAnnotation.class);

        assertThat(actual.value()).isEqualTo("default");
    }

    private Predicate<Member> named(String name) {
        return input -> input.getName().equals(name);
    }

    @SuppressWarnings("unused")
    private static class Parent {
        private int field;
        private String other;

        public Parent() {
        }

        void overrideMe() {
        }

        String someMethod(int param) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static class LowerMiddle extends Parent implements SomeInterface {
        private int field;

        public LowerMiddle() {
        }

        @Override
        public void overrideMe() {
        }

        @Override
        public void foo() {
        }
    }

    @SuppressWarnings("unused")
    private static class UpperMiddle extends LowerMiddle implements OtherInterface {
        private int field;
        private String other;

        public UpperMiddle() {
        }

        @Override
        public void overrideMe() {
        }

        @Override
        String someMethod(int param) {
            return null;
        }

        @Override
        public void bar() {
        }
    }

    @SuppressWarnings("unused")
    private static class Child extends UpperMiddle implements ChildInterface {
        private int field;
        private String another;

        public Child() {
        }

        @Override
        public void overrideMe() {
        }

        @Override
        public void child() {
        }
    }

    private interface ChildInterface {
        void child();
    }

    @SuppressWarnings("unused")
    private interface SomeInterface {
        String SOME_CONSTANT = "SOME";

        void foo();
    }

    @SuppressWarnings("unused")
    private interface OtherInterface {
        String OTHER_CONSTANT = "OTHER";

        void bar();
    }

    private interface Subinterface extends SomeInterface, OtherInterface {
    }

    @Retention(RUNTIME)
    private @interface SomeAnnotation {
        String value() default "default";
    }

    @SomeAnnotation
    private static class DirectlyAnnotated {
    }

    @Retention(RUNTIME)
    @SomeAnnotation("Meta-Annotation")
    private @interface MetaAnnotatedWithSomeAnnotation {
    }

    @MetaAnnotatedWithSomeAnnotation
    private static class MetaAnnotated {
    }

    @SomeAnnotation
    @MetaAnnotatedWithSomeAnnotation
    private static class AnnotationMultipleTimesInHierarchy {
    }
}
