package com.tngtech.archunit.lang;

import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static org.assertj.core.api.Assertions.assertThat;

public class AbstractClassesTransformerTest {
    @Test
    public void transform_javaclasses() {
        AbstractClassesTransformer<String> transformer = toNameTransformer();

        JavaClasses classes = importClassesWithContext(AbstractClassesTransformer.class, AbstractClassesTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed).containsOnly(AbstractClassesTransformer.class.getName(), AbstractClassesTransformerTest.class.getName());
    }

    @Test
    public void filter_by_predicate() {
        ClassesTransformer<String> transformer = toNameTransformer().that(endInTest());

        JavaClasses classes = importClassesWithContext(AbstractClassesTransformer.class, AbstractClassesTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed).containsOnly(AbstractClassesTransformerTest.class.getName());
    }

    @Test
    public void description_is_applied() {
        ClassesTransformer<String> transformer = toNameTransformer().as("special description");

        JavaClasses classes = importClassesWithContext(AbstractClassesTransformer.class, AbstractClassesTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed.getDescription()).isEqualTo("special description");
    }

    @Test
    public void description_is_extended_by_predicate() {
        ClassesTransformer<String> transformer = toNameTransformer().as("names").that(endInTest().as("end in Test"));

        JavaClasses classes = importClassesWithContext(AbstractClassesTransformer.class, AbstractClassesTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed.getDescription()).isEqualTo("names that end in Test");
    }

    @Test
    public void description_can_be_overwritten() {
        ClassesTransformer<String> transformer = toNameTransformer().as("names")
                .that(endInTest().as("end in Test"))
                .as("override");

        JavaClasses classes = importClassesWithContext(AbstractClassesTransformer.class, AbstractClassesTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed.getDescription()).isEqualTo("override");
    }

    private AbstractClassesTransformer<String> toNameTransformer() {
        return new AbstractClassesTransformer<String>("changeMe") {
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