package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.example.ClassViolatingCodingRules;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_ACCESS_TO_STANDARD_STREAMS;

public class CodingRulesTest {
    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(ClassViolatingCodingRules.class);
    }

    @Ignore
    @Test
    public void classes_should_not_write_to_standard_streams_defined_by_hand() {
        all(classes).should("not write to standard streams").assertedBy(NO_ACCESS_TO_STANDARD_STREAMS);
    }

    @Ignore
    @Test
    public void classes_should_not_write_to_standard_streams_from_library() {
        CLASSES_SHOULD_NOT_ACCESS_STANDARD_STREAMS.check(classes);
    }

    @Ignore
    @Test
    public void classes_should_not_throw_generic_exceptions() {
        CLASSES_SHOULD_NOT_THROW_GENERIC_EXCEPTIONS.check(classes);
    }

    @Ignore
    @Test
    public void classes_should_not_use_java_util_logging() {
        CLASSES_SHOULD_NOT_USE_JAVA_UTIL_LOGGING.check(classes);
    }
}
