package com.tngtech.archunit.core.domain;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.InitialConfiguration;
import com.tngtech.archunit.core.importer.ImportTestUtils;
import com.tngtech.archunit.core.importer.JavaAnnotationTestBuilder;
import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.core.domain.TestUtils.javaAnnotationFrom;
import static com.tngtech.archunit.core.domain.TestUtils.simpleImportedClasses;
import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationProxyTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void annotation_type_is_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.annotationType()).isEqualTo(TestAnnotation.class);
    }

    @Test
    public void primitive_is_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.primitive())
                .as(annotation.annotationType().getSimpleName() + ".primitive()")
                .isEqualTo(77);
    }

    @Test
    public void primitive_default_is_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.primitiveWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".primitiveWithDefault()")
                .isEqualTo(1);
    }

    @Test
    public void primitives_are_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.primitives())
                .as(annotation.annotationType().getSimpleName() + ".primitives()")
                .containsExactly(77, 88);
    }

    @Test
    public void primitives_defaults_are_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.primitivesWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".primitivesWithDefault()")
                .containsExactly(1, 2);
    }

    @Test
    public void string_is_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.string())
                .as(annotation.annotationType().getSimpleName() + ".string()")
                .isEqualTo("foo");
    }

    @Test
    public void string_default_is_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.stringWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".stringWithDefault()")
                .isEqualTo("something");
    }

    @Test
    public void strings_are_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.strings())
                .as(annotation.annotationType().getSimpleName() + ".strings()")
                .isEqualTo(new String[]{"one", "two"});
    }

    @Test
    public void strings_default_are_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.stringsWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".stringsWithDefault()")
                .isEqualTo(new String[]{"something", "more"});
    }

    @Test
    public void type_is_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.type())
                .as(annotation.annotationType().getSimpleName() + ".type()")
                .isEqualTo(String.class);
    }

    @Test
    public void type_default_is_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.typeWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".typeWithDefault()")
                .isEqualTo(Serializable.class);
    }

    @Test
    public void types_are_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.types())
                .as(annotation.annotationType().getSimpleName() + ".types()")
                .isEqualTo(new Class[]{Map.class, List.class});
    }

    @Test
    public void types_default_are_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.typesWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".typesWithDefault()")
                .isEqualTo(new Class[]{Serializable.class, String.class});
    }

    @Test
    public void enumConstant_is_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.enumConstant())
                .as(annotation.annotationType().getSimpleName() + ".enumConstant()")
                .isEqualTo(TestEnum.SECOND);
    }

    @Test
    public void enumConstant_default_is_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.enumConstantWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".enumConstantWithDefault()")
                .isEqualTo(TestEnum.FIRST);
    }

    @Test
    public void enumConstants_are_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.enumConstants())
                .as(annotation.annotationType().getSimpleName() + ".enumConstants()")
                .isEqualTo(new TestEnum[]{TestEnum.SECOND, TestEnum.THIRD});
    }

    @Test
    public void enumConstants_default_are_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.enumConstantsWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".enumConstantsWithDefault()")
                .isEqualTo(new TestEnum[]{TestEnum.FIRST, TestEnum.SECOND});
    }

    @Test
    public void subAnnotation_is_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.subAnnotation().value())
                .as(annotation.annotationType().getSimpleName() + ".subAnnotation().value()")
                .isEqualTo("custom");
    }

    @Test
    public void subAnnotation_default_is_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation.subAnnotationWithDefault().value())
                .as(annotation.annotationType().getSimpleName() + ".subAnnotationWithDefault().value()")
                .isEqualTo("default");
    }

    @Test
    public void subAnnotations_are_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(valuesOf(annotation.subAnnotations()))
                .as(annotation.annotationType().getSimpleName() + ".subAnnotations()*.value()")
                .containsExactly("customOne", "customTwo");
    }

    @Test
    public void subAnnotations_default_are_returned() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(valuesOf(annotation.subAnnotationsWithDefault()))
                .as(annotation.annotationType().getSimpleName() + ".subAnnotationsWithDefault()*.value()")
                .containsExactly("defaultOne", "defaultTwo");
    }

    @Test
    // NOTE: For now we'll just implement reference equality and hashcode of the proxy object
    public void equals_hashcode_and_toString() {
        TestAnnotation annotation = getProxyFor(TestAnnotation.class);

        assertThat(annotation).isEqualTo(annotation);
        assertThat(annotation.hashCode()).isEqualTo(annotation.hashCode());
        assertThat(annotation.toString()).is(matching(TestAnnotation.class, propertiesOf(TestAnnotation.class)));
    }

    @Test
    public void wrong_annotation_type_is_rejected() {
        JavaAnnotation<?> mismatch = javaAnnotationFrom(TestAnnotation.class.getAnnotation(Retention.class), getClass());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Retention.class.getSimpleName());
        thrown.expectMessage(TestAnnotation.class.getSimpleName());
        thrown.expectMessage("incompatible");
        AnnotationProxy.of(TestAnnotation.class, mismatch);
    }

    @Test
    public void array_is_converted_to_the_correct_type() {
        ImportTestUtils.ImportedTestClasses importedClasses = simpleImportedClasses();
        JavaAnnotation<?> annotation = new JavaAnnotationTestBuilder()
                .withType(JavaType.From.name(TestAnnotation.class.getName()))
                .addProperty("types", new Object[0])
                .addProperty("enumConstants", new Object[0])
                .addProperty("subAnnotations", new Object[0])
                .build(importedClasses.get(getClass().getName()), importedClasses);

        TestAnnotation reflected = annotation.as(TestAnnotation.class);
        assertThat(reflected.types()).isEmpty();
        assertThat(reflected.enumConstants()).isEmpty();
        assertThat(reflected.subAnnotations()).isEmpty();
    }

    private ImmutableMap<String, String> propertiesOf(Class<TestAnnotation> type) {
        Function<Object, String> formatter = getAnnotationValueFormatterForCurrentPlatform();
        ImmutableMap<String, String> result = ImmutableMap.<String, String>builder()
                .put("primitive", "77")
                .put("primitiveWithDefault", "1")
                .put("primitives", formatter.apply(new int[]{77, 88}))
                .put("primitivesWithDefault", formatter.apply(new int[]{1, 2}))
                .put("string", formatter.apply("foo"))
                .put("stringWithDefault", formatter.apply("something"))
                .put("strings", formatter.apply(new String[]{"one", "two"}))
                .put("stringsWithDefault", formatter.apply(new String[]{"something", "more"}))
                .put("type", formatter.apply(String.class))
                .put("typeWithDefault", formatter.apply(Serializable.class))
                .put("types", formatter.apply(new Class[]{Map.class, List.class}))
                .put("typesWithDefault", formatter.apply(new Class[]{Serializable.class, String.class}))
                .put("enumConstant", String.valueOf(TestEnum.SECOND))
                .put("enumConstantWithDefault", String.valueOf(TestEnum.FIRST))
                .put("enumConstants", formatter.apply(new TestEnum[]{TestEnum.SECOND, TestEnum.THIRD}))
                .put("enumConstantsWithDefault", formatter.apply(new TestEnum[]{TestEnum.FIRST, TestEnum.SECOND}))
                .put("subAnnotation",
                        formatSubAnnotation(formatter, "custom"))
                .put("subAnnotationWithDefault",
                        formatSubAnnotation(formatter, "default"))
                .put("subAnnotations",
                        formatter.apply(new Object[]{
                                subAnnotationFormatter(formatter, "customOne"),
                                subAnnotationFormatter(formatter, "customTwo")}))
                .put("subAnnotationsWithDefault",
                        formatter.apply(new Object[]{
                                subAnnotationFormatter(formatter, "defaultOne"),
                                subAnnotationFormatter(formatter, "defaultTwo")}))
                .build();
        ensureInSync(Irrelevant.class.getAnnotation(type), result);
        return result;
    }

    private Function<Object, String> getAnnotationValueFormatterForCurrentPlatform() {
        DomainPlugin domainPlugin = DomainPlugin.Loader.loadForCurrentPlatform();
        InitialConfiguration<Function<Object, String>> valueFormatter = new InitialConfiguration<>();
        domainPlugin.plugInAnnotationValueFormatter(valueFormatter);
        return valueFormatter.get();
    }

    private String formatSubAnnotation(Function<Object, String> formatter, String value) {
        return "@com.tngtech.archunit.core.domain.AnnotationProxyTest$SubAnnotation(value=" + formatter.apply(value) + ")";
    }

    // NOTE: We do not want this value to be treated as a string by the formatter, and e.g. quoted -> Object
    private Object subAnnotationFormatter(final Function<Object, String> formatter, final String value) {
        return new Object() {
            @Override
            public String toString() {
                return formatSubAnnotation(formatter, value);
            }
        };
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAnnotation {
        int primitive();

        int primitiveWithDefault() default 1;

        int[] primitives();

        int[] primitivesWithDefault() default {1, 2};

        String string();

        String stringWithDefault() default "something";

        String[] strings();

        String[] stringsWithDefault() default {"something", "more"};

        Class<?> type();

        Class<?> typeWithDefault() default Serializable.class;

        Class[] types();

        Class[] typesWithDefault() default {Serializable.class, String.class};

        TestEnum enumConstant();

        TestEnum enumConstantWithDefault() default TestEnum.FIRST;

        TestEnum[] enumConstants();

        TestEnum[] enumConstantsWithDefault() default {TestEnum.FIRST, TestEnum.SECOND};

        SubAnnotation subAnnotation();

        SubAnnotation subAnnotationWithDefault() default @SubAnnotation("default");

        SubAnnotation[] subAnnotations();

        SubAnnotation[] subAnnotationsWithDefault() default {@SubAnnotation("defaultOne"), @SubAnnotation("defaultTwo")};
    }

    private @interface SubAnnotation {
        String value();
    }

    private enum TestEnum {
        FIRST, SECOND, THIRD
    }

    @TestAnnotation(
            primitive = 77,
            primitives = {77, 88},
            string = "foo",
            strings = {"one", "two"},
            type = String.class,
            types = {Map.class, List.class},
            enumConstant = TestEnum.SECOND,
            enumConstants = {TestEnum.SECOND, TestEnum.THIRD},
            subAnnotation = @SubAnnotation("custom"),
            subAnnotations = {@SubAnnotation("customOne"), @SubAnnotation("customTwo")})
    private static class Irrelevant {
    }

    private void ensureInSync(TestAnnotation annotation, Map<String, String> result) {
        Set<String> necessaryKeysAsSanityCheck = new HashSet<>();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            necessaryKeysAsSanityCheck.add(method.getName());
        }
        assertThat(result.keySet()).as("Specified expected keys").isEqualTo(necessaryKeysAsSanityCheck);
        for (String v : result.values()) {
            assertThat(annotation.toString()).contains(v);
        }
    }

    private List<String> valuesOf(SubAnnotation[] subAnnotations) {
        List<String> result = new ArrayList<>();
        for (SubAnnotation annotation : subAnnotations) {
            result.add(annotation.value());
        }
        return result;
    }

    private Condition<String> matching(final Class<?> annotationType, final Map<String, String> properties) {
        return new Condition<String>("matching " + properties) {
            @Override
            public boolean matches(String value) {
                if (value == null) {
                    return false;
                }
                String expectedPart = "@" + annotationType.getName();
                if (!value.contains(expectedPart)) {
                    return mismatch(expectedPart);
                }
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    expectedPart = String.format("%s=%s", entry.getKey(), entry.getValue());
                    if (!value.contains(expectedPart)) {
                        return mismatch(expectedPart);
                    }
                }
                return true;
            }

            private boolean mismatch(String expectedPart) {
                as(description() + " but couldn't find " + expectedPart);
                return false;
            }
        };
    }

    private <A extends Annotation> A getProxyFor(Class<A> annotationType) {
        JavaAnnotation<?> toProxy = javaAnnotationFrom(Irrelevant.class.getAnnotation(annotationType), Irrelevant.class);
        return AnnotationProxy.of(annotationType, toProxy);
    }
}
