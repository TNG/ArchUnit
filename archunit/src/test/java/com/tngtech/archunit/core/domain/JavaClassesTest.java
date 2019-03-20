package com.tngtech.archunit.core.domain;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedPredicate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaClassesTest {
    public static final JavaClasses ALL_CLASSES = importClassesWithContext(SomeClass.class, SomeOtherClass.class);
    private static final JavaClass SOME_CLASS = ALL_CLASSES.get(SomeClass.class);
    private static final JavaClass SOME_OTHER_CLASS = ALL_CLASSES.get(SomeOtherClass.class);

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void restriction_on_classes_should_filter_the_elements() {
        JavaClasses onlySomeClass = ALL_CLASSES.that(haveTheNameOf(SomeClass.class));

        assertThat(onlySomeClass).containsExactly(SOME_CLASS);
    }

    @Test
    public void restriction_on_classes_should_keep_the_original_package_tree() {
        JavaClasses restrictedClasses = ALL_CLASSES.that(haveTheNameOf(SomeClass.class));

        JavaPackage javaPackage = restrictedClasses.getPackage(SomeClass.class.getPackage().getName());

        assertThatClasses(javaPackage.getClasses()).contain(SomeOtherClass.class);
    }

    @Test
    public void creation_of_JavaClasses_from_existing_classes_should_keep_the_original_package_tree() {
        JavaClasses classes = JavaClasses.of(singletonList(ALL_CLASSES.get(SomeClass.class)));

        assertThat(classes.getDefaultPackage().getAllClasses()).containsOnlyElementsOf(ALL_CLASSES.getDefaultPackage().getAllClasses());
    }

    @Test
    public void creation_of_JavaClasses_from_empty_classes_should_create_empty_default_package() {
        JavaClasses classes = JavaClasses.of(Collections.<JavaClass>emptySet());

        assertThat(classes.getDefaultPackage().getAllClasses()).isEmpty();
    }

    @Test
    public void restriction_on_classes_should_set_description() {
        JavaClasses onlySomeClass = ALL_CLASSES.that(haveTheNameOf(SomeClass.class));

        assertThat(onlySomeClass.getDescription()).
                isEqualTo("classes that have the name " + SOME_CLASS.getSimpleName());
    }

    @Test
    public void restriction_on_classes_with_undescribed_predicate_should_keep_the_old_description() {
        JavaClasses allOriginalElements = ALL_CLASSES.that(EXIST);

        assertThat(allOriginalElements.getDescription()).isEqualTo("classes that exist");

        allOriginalElements = ALL_CLASSES.that(EXIST).as("customized");

        assertThat(allOriginalElements.getDescription()).isEqualTo("customized");
    }

    @Test
    public void contain_type() {
        assertThat(ALL_CLASSES.contain(getClass())).isFalse();
        assertThat(ALL_CLASSES.contain(SomeOtherClass.class)).isTrue();
    }

    @Test
    public void get_returns_correct_JavaClass() {
        assertThat(ALL_CLASSES.get(SomeOtherClass.class)).isEqualTo(SOME_OTHER_CLASS);
        assertThat(ALL_CLASSES.get(SomeOtherClass.class.getName())).isEqualTo(SOME_OTHER_CLASS);
    }

    @Test
    public void javaClasses_of_iterable() {
        ImmutableSet<JavaClass> iterable = ImmutableSet.of(importClassWithContext(JavaClassesTest.class), importClassWithContext(JavaClass.class));
        JavaClasses classes = JavaClasses.of(iterable);

        assertThat(ImmutableSet.copyOf(classes)).isEqualTo(iterable);
    }

    @Test
    public void javaClasses_return_size() {
        Set<JavaClass> given = ImmutableSet.of(
                importClassWithContext(Object.class),
                importClassWithContext(String.class),
                importClassWithContext(List.class));

        JavaClasses classes = JavaClasses.of(given);

        assertThat(classes.size()).as("classes.size()").isEqualTo(given.size());
    }

    @Test
    public void trying_to_get_a_missing_class_causes_IllegalArgumentException() {
        JavaClasses classes = JavaClasses.of(ImmutableSet.of(importClassWithContext(Object.class)));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("JavaClasses do not contain JavaClass of type java.lang.String");

        classes.get(String.class);
    }

    private DescribedPredicate<JavaClass> haveTheNameOf(final Class<?> clazz) {
        return new DescribedPredicate<JavaClass>("have the name " + clazz.getSimpleName()) {
            @Override
            public boolean apply(JavaClass input) {
                return input.getName().equals(clazz.getName());
            }
        };
    }

    private static final DescribedPredicate<JavaClass> EXIST = new DescribedPredicate<JavaClass>("exist") {
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