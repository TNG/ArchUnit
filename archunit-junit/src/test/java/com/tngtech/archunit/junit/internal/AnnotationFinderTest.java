package com.tngtech.archunit.junit.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationFinderTest {

    private static final Consumer<MetaTestAnno> DEFAULT_META = a -> assertThat(a.value()).as("Default annotation").isEqualTo("");
    private static final Consumer<MetaTestAnno> META_ONE = a -> assertThat(a.value()).as("Meta one").isEqualTo("One");
    private static final Consumer<MetaTestAnno> META_TWO = a -> assertThat(a.value()).as("Meta Two").isEqualTo("Two");

    private final AnnotationFinder<MetaTestAnno> sut = createFinder();

    @Test
    public void should_only_retrieve_direct_annotation() {
        // when
        List<MetaTestAnno> actual = sut.findAnnotationsOn(DirectAnnotated.class);

        // then
        assertThat(actual)
                .singleElement()
                .isInstanceOf(MetaTestAnno.class)
                .satisfies(DEFAULT_META);
    }

    @Test
    public void should_only_retrieve_extended_one_annotation() {
        // when
        final List<MetaTestAnno> actual = sut.findAnnotationsOn(ExtendedAnnotatedOne.class);

        // then
        assertThat(actual)
                .singleElement()
                .isInstanceOf(MetaTestAnno.class)
                .satisfies(META_ONE);
    }

    @Test
    public void should_retrieve_direct_and_extended_one_annotations() {
        // when
        final List<MetaTestAnno> actual = sut.findAnnotationsOn(DirectAndExtendedAnnotatedOne.class);

        // then
        assertThat(actual)
                .hasSize(2)
                .satisfiesExactlyInAnyOrder(
                        DEFAULT_META,
                        META_ONE
                );
    }

    @Test
    public void should_retrieve_all_annotations() {
        // when
        final List<MetaTestAnno> actual = sut.findAnnotationsOn(AllIn.class);

        // then
        assertThat(actual)
                .hasSize(3)
                .satisfiesExactlyInAnyOrder(
                        DEFAULT_META,
                        META_ONE,
                        META_TWO
                );
    }

    @Test
    public void should_retrieve_single_annotation_according_to_equals() {
        // when
        final List<MetaTestAnno> actual = sut.findAnnotationsOn(OverrideAnnotatedTwo.class);

        // then
        assertThat(actual)
                .hasSize(1)
                .satisfiesExactlyInAnyOrder(META_TWO);
    }

    private AnnotationFinder<MetaTestAnno> createFinder() {
        return new AnnotationFinder<>(MetaTestAnno.class);
    }

    @MetaTestAnno
    private static class DirectAnnotated {
    }

    @ExtendedTestAnnoOne
    private static class ExtendedAnnotatedOne {
    }

    @MetaTestAnno
    @ExtendedTestAnnoOne
    private static class DirectAndExtendedAnnotatedOne {
    }

    @MetaTestAnno
    @ExtendedTestAnnoOne
    @ExtendedTestAnnoTwo
    private static class AllIn {
    }

    @MetaTestAnno("Two")
    @ExtendedTestAnnoTwo
    private static class OverrideAnnotatedTwo {
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    private @interface MetaTestAnno {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @MetaTestAnno("One")
    private @interface ExtendedTestAnnoOne {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @MetaTestAnno("Two")
    private @interface ExtendedTestAnnoTwo {
    }
}
