package com.tngtech.archunit.core.domain;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static java.util.Collections.emptyMap;

class AnnotationFormatterTest {
    @Test
    void should_format_annotation_type_as_configured() {
        JavaClass annotationType = new ClassFileImporter().importClass(getClass());
        AnnotationFormatter formatter = AnnotationFormatter
                .formatAnnotationType(javaClass -> "changed." + javaClass.getSimpleName())
                .formatProperties(config -> config.formattingArraysWithSquareBrackets().formattingTypesToString());

        assertThat(formatter.format(annotationType, emptyMap()))
                .isEqualTo("@changed." + getClass().getSimpleName() + "()");
    }

    @Test
    void should_use_property_formatter_to_format_properties() {
        JavaClass annotationType = new ClassFileImporter().importClass(getClass());
        AnnotationFormatter formatter = AnnotationFormatter
                .formatAnnotationType(JavaClass::getName)
                .formatProperties(config -> config
                        .formattingArraysWithCurlyBrackets()
                        .formattingTypesToString()
                        .quotingStrings());

        assertThat(formatter.format(annotationType, ImmutableMap.of("test", new String[]{"one", "two"})))
                .contains("{\"one\", \"two\"}");
    }
}