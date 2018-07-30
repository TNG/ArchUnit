package com.tngtech.archunit.integration;

import com.tngtech.archunit.example.ClassViolatingCodingRules;
import com.tngtech.archunit.example.SomeCustomException;
import com.tngtech.archunit.example.service.ServiceViolatingLayerRules;
import com.tngtech.archunit.exampletest.CodingRulesTest;
import com.tngtech.archunit.junit.CalledByArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.ExpectedViolation;
import com.tngtech.archunit.junit.ExpectsViolations;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.junit.ExpectedAccess.callFromMethod;
import static com.tngtech.archunit.junit.ExpectedAccess.callFromStaticInitializer;

public class CodingRulesIntegrationTest extends CodingRulesTest {
    @Rule
    public final ExpectedViolation expectViolation = ExpectedViolation.none();

    @Test
    @Override
    public void classes_should_not_access_standard_streams_defined_by_hand() {
        expectViolationByWritingToStandardStream(expectViolation);

        super.classes_should_not_access_standard_streams_defined_by_hand();
    }

    @Test
    @Override
    public void classes_should_not_access_standard_streams_from_library() {
        expectViolationByWritingToStandardStream(expectViolation);

        super.classes_should_not_access_standard_streams_from_library();
    }

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationByWritingToStandardStream(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("no classes should access standard streams")
                .by(callFromMethod(ClassViolatingCodingRules.class, "printToStandardStream")
                        .getting().field(System.class, "out")
                        .inLine(12))
                .by(callFromMethod(ClassViolatingCodingRules.class, "printToStandardStream")
                        .getting().field(System.class, "err")
                        .inLine(13))
                .by(callFromMethod(ClassViolatingCodingRules.class, "printToStandardStream")
                        .toMethod(SomeCustomException.class, "printStackTrace")
                        .inLine(14))
                .by(callFromMethod(ServiceViolatingLayerRules.class, "illegalAccessToController")
                        .getting().field(System.class, "out")
                        .inLine(13));
    }

    @Test
    @Override
    public void classes_should_not_throw_generic_exceptions() {
        expectViolationByThrowingGenericException(expectViolation);

        super.classes_should_not_throw_generic_exceptions();
    }

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationByThrowingGenericException(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("no classes should throw generic exceptions")
                .by(callFromMethod(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(Throwable.class)
                        .inLine(22))
                .by(callFromMethod(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(Exception.class, String.class)
                        .inLine(24))
                .by(callFromMethod(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(RuntimeException.class, String.class, Throwable.class)
                        .inLine(26))
                .by(callFromMethod(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(Exception.class, String.class)
                        .inLine(26));
    }

    @Test
    @Override
    public void classes_should_not_use_java_util_logging() {
        expectViolationByUsingJavaUtilLogging(expectViolation);

        super.classes_should_not_use_java_util_logging();
    }

    @CalledByArchUnitIntegrationTestRunner
    public static void expectViolationByUsingJavaUtilLogging(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("no classes should use java.util.logging")
                .by(callFromStaticInitializer(ClassViolatingCodingRules.class)
                        .setting().field(ClassViolatingCodingRules.class, "log")
                        .inLine(9));
    }
}
