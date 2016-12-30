package com.tngtech.archunit.lang;

import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.core.DescribedIterable;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClassesViaReflection;
import static org.assertj.core.api.Assertions.assertThat;

public class ClassesTransformerTest {
    @Test
    public void transform_javaclasses() {
        ClassesTransformer<String> transformer = toNameTransformer();

        JavaClasses classes = javaClassesViaReflection(ClassesTransformer.class, ClassesTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed).containsOnly(ClassesTransformer.class.getName(), ClassesTransformerTest.class.getName());
    }

    @Test
    public void filter_by_predicate() {
        ClassesTransformer<String> transformer = toNameTransformer().that(endInTest());

        JavaClasses classes = javaClassesViaReflection(ClassesTransformer.class, ClassesTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed).containsOnly(ClassesTransformerTest.class.getName());
    }

    @Test
    public void description_is_applied() {
        ClassesTransformer<String> transformer = toNameTransformer().as("special description");

        JavaClasses classes = javaClassesViaReflection(ClassesTransformer.class, ClassesTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed.getDescription()).isEqualTo("special description");
    }

    @Test
    public void description_is_extended_by_predicate() {
        ClassesTransformer<String> transformer = toNameTransformer().as("names").that(endInTest().as("end in Test"));

        JavaClasses classes = javaClassesViaReflection(ClassesTransformer.class, ClassesTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed.getDescription()).isEqualTo("names that end in Test");
    }

    @Test
    public void description_can_be_overwritten() {
        ClassesTransformer<String> transformer = toNameTransformer().as("names")
                .that(endInTest().as("end in Test"))
                .as("override");

        JavaClasses classes = javaClassesViaReflection(ClassesTransformer.class, ClassesTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed.getDescription()).isEqualTo("override");
    }

    private ClassesTransformer<String> toNameTransformer() {
        return new ClassesTransformer<String>("changeMe") {
            @Override
            public Iterable<String> doTransform(JavaClasses collection) {
                Set<String> names = new HashSet<>();
                for (JavaClass javaClass : collection) {
                    names.add(javaClass.getName());
                }
                return names;
            }
        };
    }

    private DescribedPredicate<String> endInTest() {
        return new DescribedPredicate<String>("changeMe") {
            @Override
            public boolean apply(String input) {
                return input.endsWith("Test");
            }
        };
    }
}