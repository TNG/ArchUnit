package com.tngtech.archunit.junit.testexamples;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@ComplexMetaTags.LibraryTag
@AnalyzeClasses
public class ComplexMetaTags {
    public static final String FIELD_RULE_NAME = "field_rule";
    public static final String METHOD_RULE_NAME = "method_rule";

    @RulesTag
    @ArchTest
    static final ArchTests classWithMetaTag = ArchTests.in(TestClassWithMetaTag.class);

    @RulesTag
    @ArchTest
    static final ArchTests classWithMetaTags = ArchTests.in(TestClassWithMetaTags.class);

    @FieldTag
    @ArchTest
    static final ArchRule field_rule = RuleThatFails.on(UnwantedClass.class);

    @MethodTag
    @ArchTest
    static void method_rule(JavaClasses classes) {
    }

    @Inherited
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD})
    @ArchTag("library-meta-tag")
    @interface LibraryTag {
    }

    @Inherited
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD})
    @ArchTag("rules-meta-tag")
    @interface RulesTag {
    }

    @Inherited
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD})
    @ArchTag("field-meta-tag")
    @interface FieldTag {
    }

    @Inherited
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD})
    @ArchTag("method-meta-tag")
    @interface MethodTag {
    }
}
