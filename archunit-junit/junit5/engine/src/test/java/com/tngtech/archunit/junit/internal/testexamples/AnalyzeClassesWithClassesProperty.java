package com.tngtech.archunit.junit.internal.testexamples;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.library.testclasses.coveringallclasses.first.First;
import com.tngtech.archunit.library.testclasses.coveringallclasses.second.Second;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(
        classes = {First.class, Second.class},
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class}
)
public class AnalyzeClassesWithClassesProperty {
    @ArchTest
    public static final ArchRule irrelevant = classes().should(new ArchCondition<JavaClass>("exist") {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
        }
    });
}
