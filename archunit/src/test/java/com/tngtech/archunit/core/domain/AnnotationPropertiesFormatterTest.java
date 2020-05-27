package com.tngtech.archunit.core.domain;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationPropertiesFormatterTest {
    @Test
    public void formats_arrays_with_square_brackets() {
        AnnotationPropertiesFormatter formatter = AnnotationPropertiesFormatter.configure()
                .formattingArraysWithSquareBrackets()
                .formattingTypesAsClassNames()
                .build();

        assertThat(formatter.formatValue(new String[]{"one", "two"}))
                .isEqualTo("[one, two]");

        assertThat(formatter.formatValue(new int[]{5, 9}))
                .isEqualTo("[5, 9]");

        assertThat(formatter.formatValue(new List[]{ImmutableList.of(1, 2), ImmutableList.of(3, 4)}))
                .isEqualTo("[[1, 2], [3, 4]]");
    }

    @Test
    public void formats_arrays_with_curly_brackets() {
        AnnotationPropertiesFormatter formatter = AnnotationPropertiesFormatter.configure()
                .formattingArraysWithCurlyBrackets()
                .formattingTypesAsClassNames()
                .quotingStrings()
                .build();

        assertThat(formatter.formatValue(new String[]{"one", "two"}))
                .isEqualTo("{\"one\", \"two\"}");

        assertThat(formatter.formatValue(new int[]{5, 9}))
                .isEqualTo("{5, 9}");

        assertThat(formatter.formatValue(new List[]{ImmutableList.of(1, 2), ImmutableList.of(3, 4)}))
                .isEqualTo("{[1, 2], [3, 4]}");
    }

    @Test
    public void formats_types_to_string() {
        AnnotationPropertiesFormatter formatter = AnnotationPropertiesFormatter.configure()
                .formattingArraysWithCurlyBrackets()
                .formattingTypesToString()
                .quotingStrings()
                .build();

        assertThat(formatter.formatValue(Object.class))
                .isEqualTo("class java.lang.Object");
    }

    @Test
    public void formats_types_as_classNames() {
        AnnotationPropertiesFormatter formatter = AnnotationPropertiesFormatter.configure()
                .formattingArraysWithCurlyBrackets()
                .formattingTypesAsClassNames()
                .quotingStrings()
                .build();

        assertThat(formatter.formatValue(Object.class))
                .isEqualTo("java.lang.Object.class");
    }

    @Test
    public void quotes_strings() {
        AnnotationPropertiesFormatter.Builder builder = AnnotationPropertiesFormatter.configure()
                .formattingArraysWithCurlyBrackets()
                .formattingTypesAsClassNames();

        AnnotationPropertiesFormatter formatter = builder.build();
        assertThat(formatter.formatValue("string"))
                .isEqualTo("string");

        formatter = builder.quotingStrings().build();
        assertThat(formatter.formatValue("string"))
                .isEqualTo("\"string\"");
    }
}
