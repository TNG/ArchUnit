package com.tngtech.archunit.core.domain;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationValueFormatterTest {
    @Test
    public void formats_arrays_with_square_brackets() {
        AnnotationValueFormatter formatter = AnnotationValueFormatter.configure()
                .formattingArraysWithSquareBrackets()
                .formattingTypesAsClassNames()
                .build();

        assertThat(formatter.apply(new String[]{"one", "two"}))
                .isEqualTo("[one, two]");

        assertThat(formatter.apply(new int[]{5, 9}))
                .isEqualTo("[5, 9]");

        assertThat(formatter.apply(new List[]{ImmutableList.of(1, 2), ImmutableList.of(3, 4)}))
                .isEqualTo("[[1, 2], [3, 4]]");
    }

    @Test
    public void formats_arrays_with_curly_brackets() {
        AnnotationValueFormatter formatter = AnnotationValueFormatter.configure()
                .formattingArraysWithCurlyBrackets()
                .formattingTypesAsClassNames()
                .quotingStrings()
                .build();

        assertThat(formatter.apply(new String[]{"one", "two"}))
                .isEqualTo("{\"one\", \"two\"}");

        assertThat(formatter.apply(new int[]{5, 9}))
                .isEqualTo("{5, 9}");

        assertThat(formatter.apply(new List[]{ImmutableList.of(1, 2), ImmutableList.of(3, 4)}))
                .isEqualTo("{[1, 2], [3, 4]}");
    }

    @Test
    public void formats_types_to_string() {
        AnnotationValueFormatter formatter = AnnotationValueFormatter.configure()
                .formattingArraysWithCurlyBrackets()
                .formattingTypesToString()
                .quotingStrings()
                .build();

        assertThat(formatter.apply(Object.class))
                .isEqualTo("class java.lang.Object");
    }

    @Test
    public void formats_types_as_classNames() {
        AnnotationValueFormatter formatter = AnnotationValueFormatter.configure()
                .formattingArraysWithCurlyBrackets()
                .formattingTypesAsClassNames()
                .quotingStrings()
                .build();

        assertThat(formatter.apply(Object.class))
                .isEqualTo("java.lang.Object.class");
    }

    @Test
    public void quotes_strings() {
        AnnotationValueFormatter.Builder builder = AnnotationValueFormatter.configure()
                .formattingArraysWithCurlyBrackets()
                .formattingTypesAsClassNames();

        AnnotationValueFormatter formatter = builder.build();
        assertThat(formatter.apply("string"))
                .isEqualTo("string");

        formatter = builder.quotingStrings().build();
        assertThat(formatter.apply("string"))
                .isEqualTo("\"string\"");
    }
}