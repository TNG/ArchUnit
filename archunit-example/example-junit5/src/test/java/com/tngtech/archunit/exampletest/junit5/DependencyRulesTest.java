package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.example.layers.ClassViolatingCodingRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.DependencyRules.NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES;

@ArchTag("example")
@AnalyzeClasses(packagesOf = ClassViolatingCodingRules.class)
public class DependencyRulesTest {

    @ArchTest
    static final ArchRule no_accesses_to_upper_package = NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES;
}
