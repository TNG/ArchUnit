package com.tngtech.archunit.junit.testexamples.displayname;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.testexamples.RuleThatFails;
import com.tngtech.archunit.junit.testexamples.UnwantedClass;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

@AnalyzeClasses(packages = "some.dummy.package")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class DisplayNameGenerationForMethod {
    @ArchTest
    static void simple_rule(JavaClasses classes) {
        RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES).check(classes);
    }

    public static final String EXPECTED_METHOD_TEST_NAME = "simple rule";
}
