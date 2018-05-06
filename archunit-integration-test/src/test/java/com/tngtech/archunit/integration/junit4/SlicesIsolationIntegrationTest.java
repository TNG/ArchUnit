package com.tngtech.archunit.integration.junit4;

import com.tngtech.archunit.example.controller.one.UseCaseOneThreeController;
import com.tngtech.archunit.example.controller.one.UseCaseOneTwoController;
import com.tngtech.archunit.example.controller.three.UseCaseThreeController;
import com.tngtech.archunit.example.controller.two.UseCaseTwoController;
import com.tngtech.archunit.exampletest.junit4.SlicesIsolationTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.CalledByArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.ExpectsViolations;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.example.controller.one.UseCaseOneTwoController.doSomethingOne;
import static com.tngtech.archunit.example.controller.three.UseCaseThreeController.doSomethingThree;
import static com.tngtech.archunit.example.controller.two.UseCaseTwoController.doSomethingTwo;
import static com.tngtech.archunit.integration.junit4.SliceDependencyErrorMatcher.sliceDependency;
import static com.tngtech.archunit.junit.ExpectedAccess.callFromMethod;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class SlicesIsolationIntegrationTest {
    @ArchTest
    @ExpectedViolationFrom(location = SlicesIsolationIntegrationTest.class, method = "expectSliceViolationsFromDependencies")
    public static final ArchRule controllers_should_only_use_their_own_slice =
            SlicesIsolationTest.controllers_should_only_use_their_own_slice;

    @CalledByArchUnitIntegrationTestRunner
    static void expectSliceViolationsFromDependencies(ExpectsViolations expectsViolations) {
        expectSliceViolationsFromDependenciesWithIgnore(expectsViolations);

        expectsViolations
                .by(sliceDependency()
                        .described("Controller one calls Controller two")
                        .by(callFromMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .toConstructor(UseCaseTwoController.class)
                                .inLine(10))
                        .by(callFromMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .toMethod(UseCaseTwoController.class, doSomethingTwo)
                                .inLine(10)))
                .by(sliceDependency()
                        .described("Controller three calls Controller one")
                        .by(callFromMethod(UseCaseThreeController.class, doSomethingThree)
                                .toConstructor(UseCaseOneTwoController.class)
                                .inLine(9))
                        .by(callFromMethod(UseCaseThreeController.class, doSomethingThree)
                                .toMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .inLine(9)));
    }

    @ArchTest
    @ExpectedViolationFrom(location = SlicesIsolationIntegrationTest.class, method = "expectSliceViolationsFromDependenciesWithIgnore")
    public static final ArchRule controllers_should_only_use_their_own_slice_with_custom_ignore =
            SlicesIsolationTest.controllers_should_only_use_their_own_slice_with_custom_ignore;

    @CalledByArchUnitIntegrationTestRunner
    static void expectSliceViolationsFromDependenciesWithIgnore(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("Controllers should not depend on each other")
                .by(sliceDependency()
                        .described("Controller one calls Controller three")
                        .by(callFromMethod(UseCaseOneThreeController.class, doSomethingOne)
                                .toConstructor(UseCaseThreeController.class)
                                .inLine(9))
                        .by(callFromMethod(UseCaseOneThreeController.class, doSomethingOne)
                                .toMethod(UseCaseThreeController.class, doSomethingThree)
                                .inLine(9)))
                .by(sliceDependency()
                        .described("Controller two calls Controller one")
                        .by(callFromMethod(UseCaseTwoController.class, doSomethingTwo)
                                .toConstructor(UseCaseOneTwoController.class)
                                .inLine(9))
                        .by(callFromMethod(UseCaseTwoController.class, doSomethingTwo)
                                .toMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .inLine(9)));
    }

    @ArchTest
    @ExpectedViolationFrom(location = SlicesIsolationIntegrationTest.class, method = "expectFilteredSliceViolationsFromDependencies")
    public static final ArchRule specific_controllers_should_only_use_their_own_slice =
            SlicesIsolationTest.specific_controllers_should_only_use_their_own_slice;

    @CalledByArchUnitIntegrationTestRunner
    static void expectFilteredSliceViolationsFromDependencies(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("Controllers one and two should not depend on each other")
                .by(sliceDependency()
                        .described("Controller one calls Controller two")
                        .by(callFromMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .toConstructor(UseCaseTwoController.class)
                                .inLine(10))
                        .by(callFromMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .toMethod(UseCaseTwoController.class, doSomethingTwo)
                                .inLine(10)))
                .by(sliceDependency()
                        .described("Controller two calls Controller one")
                        .by(callFromMethod(UseCaseTwoController.class, doSomethingTwo)
                                .toConstructor(UseCaseOneTwoController.class)
                                .inLine(9))
                        .by(callFromMethod(UseCaseTwoController.class, doSomethingTwo)
                                .toMethod(UseCaseOneTwoController.class, doSomethingOne)
                                .inLine(9)));
    }
}
