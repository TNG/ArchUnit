package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.ClassViolatingCodingRules;
import com.tngtech.archunit.example.layers.service.Async;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.library.ProxyRules.no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with;

@Category(Example.class)
public class ProxyRulesTest {

    private final JavaClasses classes = new ClassFileImporter().importPackagesOf(ClassViolatingCodingRules.class);

    @Test
    public void no_bypass_of_proxy_logic() {
        no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(Async.class)
                .check(classes);
    }
}
