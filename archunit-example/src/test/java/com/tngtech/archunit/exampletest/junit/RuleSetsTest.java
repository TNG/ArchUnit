package com.tngtech.archunit.exampletest.junit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import org.junit.runner.RunWith;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class RuleSetsTest {
    @ArchTest
    public static final ArchRules CODING_RULES = ArchRules.in(CodingRulesWithRunnerTest.class);

    @ArchTest
    public static final ArchRules CYCLIC_DEPENDENCY_RULES = ArchRules.in(CyclicDependencyRulesTest.class);
}
