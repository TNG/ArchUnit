package com.bar;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.bar")
public class IgnoreByAnnotationWorksTest {
    @ArchTest
    public static final ArchRule ignoring_classes_annotated_with_Allow =
            noClasses().that().areNotAnnotatedWith(Allow.class)
                    .should().accessClassesThat().resideInAPackage("com.bar.evil");
}
