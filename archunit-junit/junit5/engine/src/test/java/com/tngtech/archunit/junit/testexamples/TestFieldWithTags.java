package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses
public class TestFieldWithTags {
    public static final String FIELD_WITH_TAG_NAME = "field_with_tag";

    @ArchTag("field-tag-one")
    @ArchTag("field-tag-two")
    @ArchTest
    static ArchRule field_with_tag = RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES);
}
