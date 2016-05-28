package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_SETTING_OF_JAVA_UTIL_LOGGING_FIELDS;

@ArchIgnore
@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class CodingRulesWithRunnerTest {

    @ArchTest
    public static final ArchRule<JavaClass> NO_ACCESS_TO_STANDARD_STREAMS = CLASSES_SHOULD_NOT_ACCESS_STANDARD_STREAMS;

    @ArchTest
    public static final ArchRule<JavaClass> NO_GENERIC_EXCEPTIONS = CLASSES_SHOULD_NOT_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    public static final ArchRule<JavaClass> NO_JAVA_UTIL_LOGGING = CLASSES_SHOULD_NOT_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    public static void no_java_util_logging_as_method(JavaClasses classes) {
        all(classes).should("not use java.util.logging").assertedBy(NO_SETTING_OF_JAVA_UTIL_LOGGING_FIELDS);
    }
}
