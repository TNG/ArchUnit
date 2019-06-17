package com.tngtech.archunit.exampletest.junit4;

import com.tngtech.archunit.example.layers.ClassViolatingCodingRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.library.DependencyRules.NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packagesOf = ClassViolatingCodingRules.class)
public class DependencyRulesTest {

    @ArchTest
    static final ArchRule no_accesses_to_upper_package = NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES;
}
