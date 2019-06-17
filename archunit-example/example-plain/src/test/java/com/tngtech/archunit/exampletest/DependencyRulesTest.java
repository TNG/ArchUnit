package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.ClassViolatingCodingRules;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.library.DependencyRules.NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES;

@Category(Example.class)
public class DependencyRulesTest {

    private final JavaClasses classes = new ClassFileImporter().importPackagesOf(ClassViolatingCodingRules.class);

    @Test
    public void no_accesses_to_upper_package() {
        NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES.check(classes);
    }
}
