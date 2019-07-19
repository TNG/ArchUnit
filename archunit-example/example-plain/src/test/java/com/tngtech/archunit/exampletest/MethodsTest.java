package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.anticorruption.WrappedResult;
import com.tngtech.archunit.example.layers.security.Secured;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noCodeUnits;

@Category(Example.class)
public class MethodsTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example.layers");

    @Test
    public void all_public_methods_in_the_controller_layer_should_return_API_response_wrappers() {
        methods()
                .that().areDeclaredInClassesThat().resideInAPackage("..anticorruption..")
                .and().arePublic()
                .should().haveRawReturnType(WrappedResult.class)
                .because("we do not want to couple the client code directly to the return types of the encapsulated module")
                .check(classes);
    }

    @Test
    public void code_units_in_DAO_layer_should_not_be_Secured() {
        noCodeUnits()
                .that().areDeclaredInClassesThat().resideInAPackage("..persistence..")
                .should().beAnnotatedWith(Secured.class)
                .check(classes);
    }
}