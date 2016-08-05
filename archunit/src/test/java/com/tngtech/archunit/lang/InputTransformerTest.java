package com.tngtech.archunit.lang;

import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClasses;
import static org.assertj.core.api.Assertions.assertThat;

public class InputTransformerTest {
    @Test
    public void transform_javaclasses() {
        InputTransformer<String> transformer = toNameTransformer();

        JavaClasses classes = javaClasses(InputTransformer.class, InputTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed).containsOnly(InputTransformer.class.getName(), InputTransformerTest.class.getName());
    }

    @Test
    public void filter_by_predicate() {
        InputTransformer<String> transformer = toNameTransformer().that(endInTest());

        JavaClasses classes = javaClasses(InputTransformer.class, InputTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed).containsOnly(InputTransformerTest.class.getName());
    }

    @Test
    public void description_is_applied() {
        InputTransformer<String> transformer = toNameTransformer().as("special description");

        JavaClasses classes = javaClasses(InputTransformer.class, InputTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed.getDescription()).isEqualTo("special description");
    }

    @Test
    public void description_is_extended_by_predicate() {
        InputTransformer<String> transformer = toNameTransformer().as("names").that(endInTest().as("end in Test"));

        JavaClasses classes = javaClasses(InputTransformer.class, InputTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed.getDescription()).isEqualTo("names that end in Test");
    }

    @Test
    public void description_can_be_overwritten() {
        InputTransformer<String> transformer = toNameTransformer().as("names")
                .that(endInTest().as("end in Test"))
                .as("override");

        JavaClasses classes = javaClasses(InputTransformer.class, InputTransformerTest.class);
        DescribedIterable<String> transformed = transformer.transform(classes);

        assertThat(transformed.getDescription()).isEqualTo("override");
    }

    private InputTransformer<String> toNameTransformer() {
        return new InputTransformer<String>("changeMe") {
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