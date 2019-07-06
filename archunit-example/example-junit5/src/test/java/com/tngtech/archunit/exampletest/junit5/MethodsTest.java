package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.example.layers.anticorruption.WrappedResult;
import com.tngtech.archunit.example.layers.security.Secured;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noCodeUnits;

@ArchTag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
public class MethodsTest {

    @ArchTest
    static ArchRule all_public_methods_in_the_controller_layer_should_return_API_response_wrappers =
            methods()
                    .that().areDeclaredInClassesThat().resideInAPackage("..anticorruption..")
                    .and().arePublic()
                    .should().haveRawReturnType(WrappedResult.class)
                    .because("we do not want to couple the client code directly to the return types of the encapsulated module");

    @ArchTest
    static ArchRule code_units_in_DAO_layer_should_not_be_Secured =
            noCodeUnits()
                    .that().areDeclaredInClassesThat().resideInAPackage("..persistence..")
                    .should().beAnnotatedWith(Secured.class);
}