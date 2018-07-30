package com.tngtech.archunit.integration;

import com.tngtech.archunit.example.EvilCoreAccessor;
import com.tngtech.archunit.example.SomeOtherBusinessInterface;
import com.tngtech.archunit.example.controller.WronglyAnnotated;
import com.tngtech.archunit.example.core.CoreSatellite;
import com.tngtech.archunit.example.core.HighSecurity;
import com.tngtech.archunit.example.core.VeryCentralCore;
import com.tngtech.archunit.example.web.AnnotatedController;
import com.tngtech.archunit.exampletest.SingleClassTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.Rule;

import static com.tngtech.archunit.example.core.VeryCentralCore.DO_CORE_STUFF_METHOD_NAME;
import static com.tngtech.archunit.junit.ExpectedAccess.callFromMethod;
import static com.tngtech.archunit.junit.ExpectedClass.javaClass;

public class SingleClassIntegrationTest extends SingleClassTest {
    @Rule
    public final ExpectedViolation expectedViolation = ExpectedViolation.none();

    @Override
    public void core_should_only_be_accessed_by_satellites() {
        expectedViolation
                .ofRule(String.format("the class %s should only be accessed by classes that implement %s",
                        VeryCentralCore.class.getName(), CoreSatellite.class.getName()))
                .by(callFromMethod(EvilCoreAccessor.class, "iShouldNotAccessCore")
                        .toConstructor(VeryCentralCore.class)
                        .inLine(8))
                .by(callFromMethod(EvilCoreAccessor.class, "iShouldNotAccessCore")
                        .toMethod(VeryCentralCore.class, DO_CORE_STUFF_METHOD_NAME)
                        .inLine(8));

        super.core_should_only_be_accessed_by_satellites();
    }

    @Override
    public void core_should_only_access_classes_in_core_itself() {
        expectedViolation
                .ofRule(String.format("no class %s should access classes that reside outside of packages ['..core..', 'java..']",
                        VeryCentralCore.class.getName()))
                .by(callFromMethod(VeryCentralCore.class, "coreDoingIllegalStuff")
                        .toConstructor(AnnotatedController.class)
                        .inLine(15));

        super.core_should_only_access_classes_in_core_itself();
    }

    @Override
    public void the_only_class_with_high_security_is_central_core() {
        expectedViolation
                .ofRule(String.format("classes that are annotated with @%s should be %s",
                        HighSecurity.class.getSimpleName(), VeryCentralCore.class.getName()))
                .by(javaClass(WronglyAnnotated.class).notBeing(VeryCentralCore.class));

        super.the_only_class_with_high_security_is_central_core();
    }

    @Override
    public void central_core_should_not_implement_some_business_interface() {
        expectedViolation
                .ofRule(String.format("classes that implement %s should not be %s",
                        SomeOtherBusinessInterface.class.getName(), VeryCentralCore.class.getName()))
                .by(javaClass(VeryCentralCore.class).being(VeryCentralCore.class));

        super.central_core_should_not_implement_some_business_interface();
    }
}
