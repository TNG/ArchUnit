package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.*;
import com.tngtech.archunit.junit.testexamples.subone.SimpleRuleField;
import com.tngtech.archunit.lang.ArchRule;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@TestClassWithMetaTags.MetaTags
@AnalyzeClasses
public class TestClassWithMetaTags {
    public static final String FIELD_RULE_NAME = "rule_in_class_with_meta_tags";

    @ArchTest
    public static final ArchRule rule_in_class_with_meta_tags = RuleThatFails.on(UnwantedClass.class);

    @ArchTest
    public static final ArchRules rules_in_class_with_meta_tags = ArchRules.in(SimpleRuleField.class);

    @ArchTest
    static void method_in_class_with_meta_tags(JavaClasses classes) {
    }

    @Inherited
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD})
    @ArchTags({
            @ArchTag("meta-tags-one"),
            @ArchTag("meta-tags-two"),
    })
    @interface MetaTags {
    }
}
