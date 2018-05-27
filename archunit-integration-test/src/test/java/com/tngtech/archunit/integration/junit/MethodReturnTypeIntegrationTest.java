package com.tngtech.archunit.integration.junit;

import com.tngtech.archunit.example.anticorruption.WithIllegalReturnType;
import com.tngtech.archunit.example.anticorruption.WrappedResult;
import com.tngtech.archunit.example.anticorruption.internal.InternalType;
import com.tngtech.archunit.exampletest.junit.MethodReturnTypeTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.CalledByArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.ExpectsViolations;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.junit.ExpectedMethod.method;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class MethodReturnTypeIntegrationTest {
    @ArchTest
    @ExpectedViolationFrom(location = MethodReturnTypeIntegrationTest.class, method = "expectViolationForWrongMethodReturnType")
    public static final ArchRule all_public_methods_in_the_controller_layer_should_return_API_response_wrappers =
            MethodReturnTypeTest.all_public_methods_in_the_controller_layer_should_return_API_response_wrappers;

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationForWrongMethodReturnType(ExpectsViolations expectViolations) {
        String expectedRuleText = String.format(
                "methods that reside in a package '..anticorruption..' and are public "
                        + "should return type %s, "
                        + "because we don't want to couple the client code directly to the return types of the encapsulated module",
                WrappedResult.class.getName());

        expectViolations.ofRule(expectedRuleText)
                .by(method(WithIllegalReturnType.class, "directlyReturnInternalType").returningType(InternalType.class))
                .by(method(WithIllegalReturnType.class, "otherIllegalMethod", String.class).returningType(int.class));
    }
}
