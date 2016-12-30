package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.ArchRule.classes;
import static com.tngtech.archunit.library.GeneralCodingRules.NOT_SET_JAVA_UTIL_LOGGING_FIELDS;

@ArchIgnore
@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class CodingRulesWithRunnerMethodsTest {
    @ArchTest
    public static void no_java_util_logging_as_method(JavaClasses classes) {
        all(classes()).should(NOT_SET_JAVA_UTIL_LOGGING_FIELDS).check(classes);
    }
}
