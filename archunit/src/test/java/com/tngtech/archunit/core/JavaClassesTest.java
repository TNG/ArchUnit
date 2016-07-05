package com.tngtech.archunit.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClass;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaClassesTest {
    public static final JavaClass SOME_CLASS = new JavaClass.Builder().withType(SomeClass.class).build();
    public static final JavaClass SOME_OTHER_CLASS = new JavaClass.Builder().withType(SomeOtherClass.class).build();
    private static final ImmutableMap<Class<?>, JavaClass> BY_RAW_CLASS = ImmutableMap.of(
            SomeClass.class, SOME_CLASS,
            SomeOtherClass.class, SOME_OTHER_CLASS);
    public static final JavaClasses ALL_CLASSES = new JavaClasses(BY_RAW_CLASS, "classes");

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

    @Test
    public void contain_type() {
        assertThat(ALL_CLASSES.contain(getClass())).isFalse();
        assertThat(ALL_CLASSES.contain(SomeOtherClass.class)).isTrue();
    }

    @Test
    public void get_type_returns_correct_JavaClass() {
        assertThat(ALL_CLASSES.get(SomeOtherClass.class)).isEqualTo(SOME_OTHER_CLASS);
    }

    @Test
    public void javaClasses_of_iterable() {
        ImmutableSet<JavaClass> iterable = ImmutableSet.of(javaClass(JavaClassesTest.class), javaClass(JavaClass.class));
        JavaClasses classes = JavaClasses.of(iterable);

        assertThat(ImmutableSet.copyOf(classes)).isEqualTo(iterable);
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