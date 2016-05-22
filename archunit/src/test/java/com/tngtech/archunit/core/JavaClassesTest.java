package com.tngtech.archunit.core;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaClassesTest {
    public static final JavaClass SOME_CLASS = new JavaClass.Builder().withType(SomeClass.class).build();
    public static final JavaClass SOME_OTHER_CLASS = new JavaClass.Builder().withType(SomeOtherClass.class).build();
    public static final JavaClasses ALL_CLASSES = new JavaClasses(ImmutableSet.of(SOME_CLASS, SOME_OTHER_CLASS), "classes");

    @Test
    public void restriction_on_classes_should_filter_the_elements() {
        JavaClasses onlySomeClass = ALL_CLASSES.that(haveTheNameOf(SomeClass.class));

        assertThat(onlySomeClass).containsExactly(SOME_CLASS);
    }

    @Test
    public void restriction_on_classes_should_set_description() {
        JavaClasses onlySomeClass = ALL_CLASSES.that(haveTheNameOf(SomeClass.class));

        assertThat(onlySomeClass.getDescription()).isEqualTo(SOME_CLASS.reflect().getSimpleName());
    }

    @Test
    public void restriction_on_classes_with_undescribed_predicate_should_keep_the_old_description() {
        JavaClasses allOriginalElements = ALL_CLASSES.that(EXIST);

        assertThat(allOriginalElements.getDescription()).isEqualTo(ALL_CLASSES.getDescription());
    }

    private DescribedPredicate<JavaClass> haveTheNameOf(final Class<?> clazz) {
        return new DescribedPredicate<JavaClass>(clazz.getSimpleName()) {
            @Override
            public boolean apply(JavaClass input) {
                return input.reflect().getName().equals(clazz.getName());
            }
        };
    }

    private static final DescribedPredicate<JavaClass> EXIST = new DescribedPredicate<JavaClass>() {
        @Override
        public boolean apply(JavaClass input) {
            return true;
        }
    };

    private static class SomeClass {
    }

    private static class SomeOtherClass {
    }
}