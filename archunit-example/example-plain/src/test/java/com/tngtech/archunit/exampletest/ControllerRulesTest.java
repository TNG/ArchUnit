package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatchers;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.security.Secured;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_PACKAGE_NAME;
import static com.tngtech.archunit.core.domain.JavaMember.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Category(Example.class)
public class ControllerRulesTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example.layers");

    @Test
    public void controllers_should_only_call_secured_methods() {
        classes()
                .that().resideInAPackage("..controller..")
                .should().onlyCallMethodsThat(areDeclaredInController().or(are(annotatedWith(Secured.class))))
                .check(classes);
    }

    @Test
    public void controllers_should_only_call_secured_constructors() {
        classes()
                .that().resideInAPackage("..controller..")
                .should().onlyCallConstructorsThat(areDeclaredInController().or(are(annotatedWith(Secured.class))))
                .check(classes);
    }

    @Test
    public void controllers_should_only_call_secured_code_units() {
        classes()
                .that().resideInAPackage("..controller..")
                .should().onlyCallCodeUnitsThat(areDeclaredInController().or(are(annotatedWith(Secured.class))))
                .check(classes);
    }

    @Test
    public void controllers_should_only_access_secured_fields() {
        classes()
                .that().resideInAPackage("..controller..")
                .should().onlyAccessFieldsThat(areDeclaredInController().or(are(annotatedWith(Secured.class))))
                .check(classes);
    }

    @Test
    public void controllers_should_only_access_secured_members() {
        classes()
                .that().resideInAPackage("..controller..")
                .should().onlyAccessMembersThat(areDeclaredInController().or(are(annotatedWith(Secured.class))))
                .check(classes);
    }

    private DescribedPredicate<JavaMember> areDeclaredInController() {
        DescribedPredicate<JavaClass> aPackageController = GET_PACKAGE_NAME.is(PackageMatchers.of("..controller..", "java.."))
                .as("a package '..controller..'");
        return are(declaredIn(aPackageController));
    }
}
