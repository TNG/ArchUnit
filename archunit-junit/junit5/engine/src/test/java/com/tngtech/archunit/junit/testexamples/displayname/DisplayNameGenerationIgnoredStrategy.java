package com.tngtech.archunit.junit.testexamples.displayname;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.testexamples.RuleThatFails;
import com.tngtech.archunit.junit.testexamples.UnwantedClass;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

@AnalyzeClasses(packages = "some.dummy.package")
@DisplayNameGeneration(DisplayNameGenerator.IndicativeSentences.class)
public class DisplayNameGenerationIgnoredStrategy {
    @ArchTest
    public static final ArchRule simple_rule = RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES);

    public static final String EXPECTED_ORIGINAL_TEST_NAME = "simple_rule";
}
