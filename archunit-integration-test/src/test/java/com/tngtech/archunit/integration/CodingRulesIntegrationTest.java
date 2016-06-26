package com.tngtech.archunit.integration;

import com.tngtech.archunit.example.ClassViolatingCodingRules;
import com.tngtech.archunit.exampletest.CodingRulesTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.junit.ExpectedViolation.from;

public class CodingRulesIntegrationTest extends CodingRulesTest {
    @Rule
    public final ExpectedViolation expectViolation = ExpectedViolation.none();

    @Test
    @Override
    public void classes_should_not_write_to_standard_streams_defined_by_hand() {
        expectViolationByWritingToStandardStream(expectViolation);

        super.classes_should_not_write_to_standard_streams_defined_by_hand();
    }

    @Test
    @Override
    public void classes_should_not_write_to_standard_streams_from_library() {
        expectViolationByWritingToStandardStream(expectViolation);

        super.classes_should_not_write_to_standard_streams_from_library();
    }

    static void expectViolationByWritingToStandardStream(ExpectedViolation expectedViolation) {
        expectedViolation.ofRule("classes should not write to standard streams")
                .byAccess(from(ClassViolatingCodingRules.class, "printToStandardStream")
                        .accessing().field(System.class, "out")
                        .inLine(9))
                .byAccess(from(ClassViolatingCodingRules.class, "printToStandardStream")
                        .accessing().field(System.class, "err")
                        .inLine(10))
                .byCall(from(ClassViolatingCodingRules.class, "printToStandardStream")
                        .toMethod(Throwable.class, "printStackTrace")
                        .inLine(11));
    }

    @Test
    @Override
    public void classes_should_not_throw_generic_exceptions() {
        expectViolationByThrowingGenericException(expectViolation);

        super.classes_should_not_throw_generic_exceptions();
    }

    static void expectViolationByThrowingGenericException(ExpectedViolation expectedViolation) {
        expectedViolation.ofRule("classes should not throw generic exceptions")
                .byCall(from(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(Throwable.class)
                        .inLine(16))
                .byCall(from(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(Exception.class, String.class)
                        .inLine(18))
                .byCall(from(ClassViolatingCodingRules.class, "throwGenericExceptions")
                        .toConstructor(RuntimeException.class, String.class, Throwable.class)
                        .inLine(20));
    }

    @Test
    @Override
    public void classes_should_not_use_java_util_logging() {
        expectViolationByUsingJavaUtilLogging(expectViolation);

        super.classes_should_not_use_java_util_logging();
    }

    static void expectViolationByUsingJavaUtilLogging(ExpectedViolation expectedViolation) {
        expectedViolation.ofRule("classes should not use java.util.logging")
                .byAccess(from(ClassViolatingCodingRules.class, "<clinit>")
                        .setting().field(ClassViolatingCodingRules.class, "log")
                        .inLine(6));
    }
}
