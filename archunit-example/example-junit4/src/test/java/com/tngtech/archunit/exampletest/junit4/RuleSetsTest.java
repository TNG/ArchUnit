package com.tngtech.archunit.exampletest.junit4;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.junit.ArchUnitRunner;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
public class RuleSetsTest {
    @ArchTest
    private final ArchTests CODING_RULES = ArchTests.in(CodingRulesTest.class);

    @ArchTest
    private final ArchTests NAMING_CONVENTION_RULES = ArchTests.in(NamingConventionTest.class);
}
