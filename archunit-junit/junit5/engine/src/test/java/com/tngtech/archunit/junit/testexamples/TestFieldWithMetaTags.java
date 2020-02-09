package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTags;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@AnalyzeClasses
public class TestFieldWithMetaTags {
    public static final String FIELD_WITH_META_TAGS_NAME = "field_with_meta_tags";

    @MetaTags
    @ArchTest
    static ArchRule field_with_meta_tags = RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES);

    @Inherited
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD})
    @ArchTags({
            @ArchTag("field-meta-tags-one"),
            @ArchTag("field-meta-tags-two"),
    })
    private @interface MetaTags {
    }
}
