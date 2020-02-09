package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@AnalyzeClasses
public class TestMethodWithMetaTag {
    public static final String METHOD_WITH_META_TAG_NAME = "method_with_meta_tag";

    @MetaTag
    @ArchTest
    static void method_with_meta_tag(JavaClasses classes) {
    }

    @Inherited
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD})
    @ArchTag("method-meta-tag-one")
    @ArchTag("method-meta-tag-two")
    private @interface MetaTag {
    }
}
