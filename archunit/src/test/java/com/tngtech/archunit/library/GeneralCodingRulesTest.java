package com.tngtech.archunit.library;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.testclasses.packages.correct.ImplementationClassWithCorrectPackage;
import com.tngtech.archunit.library.testclasses.packages.incorrect.ImplementationClassWithWrongTestClassPackage;
import com.tngtech.archunit.library.testclasses.packages.incorrect.wrongsubdir.ImplementationClassWithWrongTestClassPackageTest;
import com.tngtech.archunit.library.testclasses.packages.incorrect.wrongsubdir.ImplementationClassWithWrongTestClassPackageTestingScenario;
import org.junit.Test;

import static com.tngtech.archunit.library.GeneralCodingRules.testClassesShouldResideInTheSamePackageAsImplementation;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;

public class GeneralCodingRulesTest {

    @Test
    public void test_class_in_same_package_should_fail_when_test_class_reside_in_different_package_as_implementation() {
        assertThatRule(testClassesShouldResideInTheSamePackageAsImplementation())
                .checking(new ClassFileImporter().importPackagesOf(ImplementationClassWithWrongTestClassPackage.class))
                .hasOnlyOneViolationWithStandardPattern(ImplementationClassWithWrongTestClassPackageTest.class,
                        "does not reside in same package as implementation class <" + ImplementationClassWithWrongTestClassPackage.class.getName() + ">");
    }

    @Test
    public void test_class_in_same_package_should_fail_when_test_class_reside_in_different_package_as_implementation_with_custom_suffix() {
        assertThatRule(testClassesShouldResideInTheSamePackageAsImplementation("TestingScenario"))
                .checking(new ClassFileImporter().importPackagesOf(ImplementationClassWithWrongTestClassPackage.class))
                .hasOnlyOneViolationWithStandardPattern(ImplementationClassWithWrongTestClassPackageTestingScenario.class,
                        "does not reside in same package as implementation class <" + ImplementationClassWithWrongTestClassPackage.class.getName() + ">");
    }

    @Test
    public void test_class_in_same_package_should_pass_when_test_class_and_implementation_class_reside_in_the_same_package() {
        assertThatRule(testClassesShouldResideInTheSamePackageAsImplementation())
                .checking(new ClassFileImporter().importPackagesOf(ImplementationClassWithCorrectPackage.class))
                .hasNoViolation();
    }

    @Test
    public void test_class_in_same_package_should_pass_when_test_class_and_implementation_class_reside_in_the_same_package_with_custom_suffix() {
        assertThatRule(testClassesShouldResideInTheSamePackageAsImplementation("TestingScenario"))
                .checking(new ClassFileImporter().importPackagesOf(ImplementationClassWithCorrectPackage.class))
                .hasNoViolation();
    }
}
