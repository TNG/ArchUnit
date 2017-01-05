package com.tngtech.archunit.integration.junit;

import com.tngtech.archunit.example.controller.one.UseCaseOneController;
import com.tngtech.archunit.example.controller.two.UseCaseTwoController;
import com.tngtech.archunit.exampletest.junit.SlicesIsolationTest;
import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.example.controller.one.UseCaseOneController.doSomethingOne;
import static com.tngtech.archunit.example.controller.two.UseCaseTwoController.doSomethingTwo;
import static com.tngtech.archunit.integration.junit.SliceDependencyErrorMatcher.sliceDependency;
import static com.tngtech.archunit.junit.ExpectedViolation.from;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class SlicesIsolationIntegrationTest {
    @ArchTest
    @ExpectedViolationFrom(location = SlicesIsolationIntegrationTest.class, method = "expectViolationFromDependencies")
    public static final ArchRule controllers_should_only_use_their_own_slice =
            SlicesIsolationTest.controllers_should_only_use_their_own_slice;

    static void expectViolationFromDependencies(ExpectedViolation expectViolation) {
        expectViolation.ofRule("Controllers should only depend on their own slice")
                .by(sliceDependency()
                        .described("Controller one calls Controller two")
                        .byAccess(from(UseCaseOneController.class, doSomethingOne)
                                .toConstructor(UseCaseTwoController.class)
                                .inLine(10))
                        .byAccess(from(UseCaseOneController.class, doSomethingOne)
                                .toMethod(UseCaseTwoController.class, doSomethingTwo)
                                .inLine(10)))
                .by(sliceDependency()
                        .described("Controller two calls Controller one")
                        .byAccess(from(UseCaseTwoController.class, doSomethingTwo)
                                .toConstructor(UseCaseOneController.class)
                                .inLine(9))
                        .byAccess(from(UseCaseTwoController.class, doSomethingTwo)
                                .toMethod(UseCaseOneController.class, doSomethingOne)
                                .inLine(9)));
    }
}
