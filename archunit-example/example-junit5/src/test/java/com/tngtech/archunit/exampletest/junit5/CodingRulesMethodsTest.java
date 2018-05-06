package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.USE_JAVA_UTIL_LOGGING;

@Tag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
class CodingRulesMethodsTest {
    @ArchTest
    static void no_java_util_logging_as_method(JavaClasses classes) {
        noClasses().should(USE_JAVA_UTIL_LOGGING).check(classes);
    }
}
