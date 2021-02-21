package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.example.layers.ClassViolatingCodingRules;
import com.tngtech.archunit.example.layers.service.Async;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.ProxyRules.no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with;

@ArchTag("example")
@AnalyzeClasses(packagesOf = ClassViolatingCodingRules.class)
public class ProxyRulesTest {

    @ArchTest
    static ArchRule no_bypass_of_proxy_logic =
            no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(Async.class);

}
