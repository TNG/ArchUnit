package com.tngtech.archunit.junit.internal.testexamples;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTags;
import com.tngtech.archunit.junit.ArchTest;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@AnalyzeClasses
public class TestMethodWithMetaTags {
    public static final String METHOD_WITH_META_TAGS_NAME = "method_with_meta_tags";

    @MetaTags
    @ArchTest
    static void method_with_meta_tags(JavaClasses classes) {
    }

    @Inherited
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD})
    @ArchTags({
            @ArchTag("method-meta-tags-one"),
            @ArchTag("method-meta-tags-two"),
    })
    private @interface MetaTags {
    }
}
