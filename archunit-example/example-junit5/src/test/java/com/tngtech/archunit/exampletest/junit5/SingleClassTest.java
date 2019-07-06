package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.example.layers.SomeOtherBusinessInterface;
import com.tngtech.archunit.example.layers.core.CoreSatellite;
import com.tngtech.archunit.example.layers.core.HighSecurity;
import com.tngtech.archunit.example.layers.core.VeryCentralCore;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClass;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.theClass;

@ArchTag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
public class SingleClassTest {

    @ArchTest
    static final ArchRule core_should_only_be_accessed_by_satellites =
            theClass(VeryCentralCore.class)
                    .should().onlyBeAccessed().byClassesThat().implement(CoreSatellite.class);

    @ArchTest
    static final ArchRule core_should_only_access_classes_in_core_itself =
            noClass(VeryCentralCore.class)
                    .should().accessClassesThat().resideOutsideOfPackages("..core..", "java..");

    @ArchTest
    static final ArchRule the_only_class_with_high_security_is_central_core =
            classes()
                    .that().areAnnotatedWith(HighSecurity.class)
                    .should().be(VeryCentralCore.class);

    @ArchTest
    static final ArchRule central_core_should_not_implement_some_business_interface =
            classes()
                    .that().implement(SomeOtherBusinessInterface.class)
                    .should().notBe(VeryCentralCore.class);

}
