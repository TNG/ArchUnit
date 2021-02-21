package com.tngtech.archunit.junit.testexamples;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.junit.testexamples.subone.SimpleRuleField;
import com.tngtech.archunit.lang.ArchRule;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@TestClassWithMetaTag.MetaTag
@AnalyzeClasses
public class TestClassWithMetaTag {
    public static final String FIELD_RULE_NAME = "rule_in_class_with_meta_tag";

    @ArchTest
    public static final ArchRule rule_in_class_with_meta_tag = RuleThatFails.on(UnwantedClass.class);

    @ArchTest
    public static final ArchTests rules_in_class_with_meta_tag = ArchTests.in(SimpleRuleField.class);

    @ArchTest
    static void method_in_class_with_meta_tag(JavaClasses classes) {
    }

    @Inherited
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD})
    @ArchTag("meta-tag-one")
    @ArchTag("meta-tag-two")
    @interface MetaTag {
    }
}
