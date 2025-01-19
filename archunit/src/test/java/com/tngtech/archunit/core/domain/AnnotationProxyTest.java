package com.tngtech.archunit.core.domain;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.base.Suppliers;
import com.tngtech.archunit.core.InitialConfiguration;
import com.tngtech.archunit.core.domain.AnnotationFormatter.AnnotationPropertiesFormatter;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.assertj.core.api.Condition;
import org.junit.Test;

import static com.tngtech.archunit.testutil.ReflectionTestUtils.getFieldValue;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AnnotationProxyTest {
    private final Supplier<AnnotationFormatter> annotationFormatterForCurrentPlatform =
            Suppliers.memoize(() -> {
                DomainPlugin domainPlugin = DomainPlugin.Loader.loadForCurrentPlatform();
                InitialConfiguration<AnnotationFormatter> formatter = new InitialConfiguration<>();
                domainPlugin.plugInAnnotationFormatter(formatter);
                return formatter.get();
            });
    private final Supplier<Function<JavaClass, String>> annotationTypeFormatter =
            Suppliers.memoize(() -> getFieldValue(annotationFormatterForCurrentPlatform.get(), "annotationTypeFormatter"));
    private final Supplier<AnnotationPropertiesFormatter> annotationPropertiesFormatter =
            Suppliers.memoize(() -> getFieldValue(annotationFormatterForCurrentPlatform.get(), "propertiesFormatter"));

    @Test
    public void annotation_type_is_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.annotationType()).isEqualTo(TestAnnotation.class);
    }

    @Test
    public void primitive_is_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.primitive())
                .as(annotation.annotationType().getSimpleName() + ".primitive()")
                .isEqualTo(77);
    }

    @Test
    public void primitive_default_is_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.primitiveWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".primitiveWithDefault()")
                .isEqualTo(1);
    }

    @Test
    public void primitives_are_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.primitives())
                .as(annotation.annotationType().getSimpleName() + ".primitives()")
                .containsExactly(77, 88);
    }

    @Test
    public void primitives_defaults_are_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.primitivesWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".primitivesWithDefault()")
                .containsExactly(1, 2);
    }

    @Test
    public void string_is_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.string())
                .as(annotation.annotationType().getSimpleName() + ".string()")
                .isEqualTo("foo");
    }

    @Test
    public void string_default_is_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.stringWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".stringWithDefault()")
                .isEqualTo("something");
    }

    @Test
    public void strings_are_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.strings())
                .as(annotation.annotationType().getSimpleName() + ".strings()")
                .isEqualTo(new String[]{"one", "two"});
    }

    @Test
    public void strings_default_are_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.stringsWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".stringsWithDefault()")
                .isEqualTo(new String[]{"something", "more"});
    }

    @Test
    public void type_is_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.type())
                .as(annotation.annotationType().getSimpleName() + ".type()")
                .isEqualTo(String.class);
    }

    @Test
    public void type_default_is_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.typeWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".typeWithDefault()")
                .isEqualTo(Serializable.class);
    }

    @Test
    public void types_are_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.types())
                .as(annotation.annotationType().getSimpleName() + ".types()")
                .isEqualTo(new Class[]{Map.class, List.class});
    }

    @Test
    public void types_default_are_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.typesWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".typesWithDefault()")
                .isEqualTo(new Class[]{Serializable.class, String.class});
    }

    @Test
    public void enumConstant_is_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.enumConstant())
                .as(annotation.annotationType().getSimpleName() + ".enumConstant()")
                .isEqualTo(TestEnum.SECOND);
    }

    @Test
    public void enumConstant_default_is_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.enumConstantWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".enumConstantWithDefault()")
                .isEqualTo(TestEnum.FIRST);
    }

    @Test
    public void enumConstants_are_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.enumConstants())
                .as(annotation.annotationType().getSimpleName() + ".enumConstants()")
                .isEqualTo(new TestEnum[]{TestEnum.SECOND, TestEnum.THIRD});
    }

    @Test
    public void enumConstants_default_are_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.enumConstantsWithDefault())
                .as(annotation.annotationType().getSimpleName() + ".enumConstantsWithDefault()")
                .isEqualTo(new TestEnum[]{TestEnum.FIRST, TestEnum.SECOND});
    }

    @Test
    public void subAnnotation_is_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.subAnnotation().value())
                .as(annotation.annotationType().getSimpleName() + ".subAnnotation().value()")
                .isEqualTo("custom");
    }

    @Test
    public void subAnnotation_default_is_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation.subAnnotationWithDefault().value())
                .as(annotation.annotationType().getSimpleName() + ".subAnnotationWithDefault().value()")
                .isEqualTo("default");
    }

    @Test
    public void subAnnotations_are_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(valuesOf(annotation.subAnnotations()))
                .as(annotation.annotationType().getSimpleName() + ".subAnnotations()*.value()")
                .containsExactly("customOne", "customTwo");
    }

    @Test
    public void subAnnotations_default_are_returned() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(valuesOf(annotation.subAnnotationsWithDefault()))
                .as(annotation.annotationType().getSimpleName() + ".subAnnotationsWithDefault()*.value()")
                .containsExactly("defaultOne", "defaultTwo");
    }

    @Test
    // NOTE: For now we'll just implement reference equality and hashcode of the proxy object
    public void equals_hashcode_and_toString() {
        TestAnnotation annotation = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);
        TestAnnotation other = importAnnotation(ClassWithTestAnnotation.class, TestAnnotation.class);

        assertThat(annotation).isEqualTo(annotation);
        assertThat(annotation).isNotEqualTo(other);
        assertThat(annotation.hashCode()).isEqualTo(annotation.hashCode());
        assertThat(annotation.toString()).is(matching(TestAnnotation.class, propertiesOf(TestAnnotation.class)));
    }

    @Test
    public void wrong_annotation_type_is_rejected() {
        JavaAnnotation<?> mismatch = new ClassFileImporter().importClasses(TestAnnotation.class, Retention.class)
                .get(TestAnnotation.class).getAnnotationOfType(Retention.class.getName());

        assertThatThrownBy(
                () -> AnnotationProxy.of(TestAnnotation.class, mismatch)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Retention.class.getSimpleName())
                .hasMessageContaining(TestAnnotation.class.getSimpleName())
                .hasMessageContaining("incompatible")
        ;
    }

    @Test
    public void array_is_converted_to_the_correct_type() {
        TestAnnotation reflected = importAnnotation(ClassWithTestAnnotationWithEmptyArrays.class, TestAnnotation.class);

        assertThat(reflected.types()).isEmpty();
        assertThat(reflected.enumConstants()).isEmpty();
        assertThat(reflected.subAnnotations()).isEmpty();
    }

    private ImmutableMap<String, String> propertiesOf(Class<TestAnnotation> type) {
        AnnotationPropertiesFormatter propertiesFormatter = annotationPropertiesFormatter.get();
        ImmutableMap<String, String> result = ImmutableMap.<String, String>builder()
                .put("primitive", "77")
                .put("primitiveWithDefault", "1")
                .put("primitives", propertiesFormatter.formatValue(new int[]{77, 88}))
                .put("primitivesWithDefault", propertiesFormatter.formatValue(new int[]{1, 2}))
                .put("string", propertiesFormatter.formatValue("foo"))
                .put("stringWithDefault", propertiesFormatter.formatValue("something"))
                .put("strings", propertiesFormatter.formatValue(new String[]{"one", "two"}))
                .put("stringsWithDefault", propertiesFormatter.formatValue(new String[]{"something", "more"}))
                .put("type", propertiesFormatter.formatValue(String.class))
                .put("typeWithDefault", propertiesFormatter.formatValue(Serializable.class))
                .put("types", propertiesFormatter.formatValue(new Class[]{Map.class, List.class}))
                .put("typesWithDefault", propertiesFormatter.formatValue(new Class[]{Serializable.class, String.class}))
                .put("enumConstant", String.valueOf(TestEnum.SECOND))
                .put("enumConstantWithDefault", String.valueOf(TestEnum.FIRST))
                .put("enumConstants", propertiesFormatter.formatValue(new TestEnum[]{TestEnum.SECOND, TestEnum.THIRD}))
                .put("enumConstantsWithDefault", propertiesFormatter.formatValue(new TestEnum[]{TestEnum.FIRST, TestEnum.SECOND}))
                .put("subAnnotation", formatSubAnnotation("custom"))
                .put("subAnnotationWithDefault", formatSubAnnotation("default"))
                .put("subAnnotations",
                        propertiesFormatter.formatValue(new Object[]{
                                subAnnotationFormatter("customOne"),
                                subAnnotationFormatter("customTwo")}))
                .put("subAnnotationsWithDefault",
                        propertiesFormatter.formatValue(new Object[]{
                                subAnnotationFormatter("defaultOne"),
                                subAnnotationFormatter("defaultTwo")}))
                .build();
        ensureInSync(ClassWithTestAnnotation.class.getAnnotation(type), result);
        return result;
    }

    private String formatSubAnnotation(String value) {
        Map<String, Object> properties = Collections.singletonMap("value", value);
        return "@" + formatAnnotationType(SubAnnotation.class) + "(" + formatAnnotationProperties(properties) + ")";
    }

    private String formatAnnotationType(Class<?> annotationType) {
        return annotationTypeFormatter.get().apply(new ClassFileImporter().importClass(annotationType));
    }

    private String formatAnnotationProperties(Map<String, Object> properties) {
        return annotationPropertiesFormatter.get().formatProperties(properties);
    }

    // NOTE: We do not want this value to be treated as a string by the formatter, and e.g. quoted -> Object
    private Object subAnnotationFormatter(String value) {
        return new Object() {
            @Override
            public String toString() {
                return formatSubAnnotation(value);
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

        Class<?>[] types();

        Class<?>[] typesWithDefault() default {Serializable.class, String.class};

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
    private static class ClassWithTestAnnotation {
    }

    @TestAnnotation(
            primitive = 0,
            primitives = {},
            string = "",
            strings = {},
            type = TestAnnotation.class,
            types = {},
            enumConstant = TestEnum.SECOND,
            enumConstants = {},
            subAnnotation = @SubAnnotation(""),
            subAnnotations = {})
    private static class ClassWithTestAnnotationWithEmptyArrays {
    }

    private void ensureInSync(TestAnnotation annotation, Map<String, String> result) {
        Set<String> necessaryKeysAsSanityCheck = stream(annotation.annotationType().getDeclaredMethods())
                .map(Method::getName)
                .collect(toSet());
        assertThat(result.keySet()).as("Specified expected keys").isEqualTo(necessaryKeysAsSanityCheck);

        String expectedAnnotationString = annotation.toString();
        for (String v : result.values()) {
            assertThat(v).isSubstringOf(expectedAnnotationString);
        }
    }

    private List<String> valuesOf(SubAnnotation[] subAnnotations) {
        return stream(subAnnotations).map(SubAnnotation::value).collect(toList());
    }

    private Condition<String> matching(Class<?> annotationType, Map<String, String> properties) {
        return new Condition<String>("matching " + properties) {
            @Override
            public boolean matches(String value) {
                if (value == null) {
                    return false;
                }
                String expectedPart = "@" + formatAnnotationType(annotationType);
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

    private <A extends Annotation> A importAnnotation(Class<?> ownerType, Class<A> annotationType) {
        return new ClassFileImporter().importClasses(ownerType, annotationType).get(ownerType).getAnnotationOfType(annotationType);
    }
}
