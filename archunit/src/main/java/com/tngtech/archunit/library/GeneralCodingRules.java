package com.tngtech.archunit.library;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.OpenArchRule;

import static com.tngtech.archunit.core.DescribedPredicate.all;
import static com.tngtech.archunit.lang.OpenArchRule.rule;
import static com.tngtech.archunit.lang.conditions.ArchConditions.classAccessesField;
import static com.tngtech.archunit.lang.conditions.ArchConditions.classCallsMethod;
import static com.tngtech.archunit.lang.conditions.ArchConditions.classCallsMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.classSetsFieldWith;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.fieldTypeIn;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.targetHasName;

public class GeneralCodingRules {
    public static final ArchCondition<JavaClass> NO_ACCESS_TO_STANDARD_STREAMS = noAccessToStandardStreams();

    private static ArchCondition<JavaClass> noAccessToStandardStreams() {
        ArchCondition<JavaClass> noAccessToSystemOut = never(classAccessesField(System.class, "out"));
        ArchCondition<JavaClass> noAccessToSystemErr = never(classAccessesField(System.class, "err"));
        ArchCondition<JavaClass> noCallOfPrintStackTrace = never(classCallsMethod(Throwable.class, "printStackTrace"));

        return noAccessToSystemOut.and(noAccessToSystemErr).and(noCallOfPrintStackTrace);
    }

    /**
     * It is generally good practice to use correct logging instead of writing to the console.
     * <ul>
     * <li>Writing to the console cannot be configured in production</li>
     * <li>Writing to the console is synchronized and can lead to bottle necks</li>
     * </ul>
     */
    public static final OpenArchRule<JavaClass> CLASSES_SHOULD_NOT_ACCESS_STANDARD_STREAMS =
            rule(all(JavaClass.class))
                    .should("not write to standard streams")
                    .assertedBy(NO_ACCESS_TO_STANDARD_STREAMS);

    public static final ArchCondition<JavaClass> NO_GENERIC_EXCEPTIONS = noGenericExceptions();

    private static ArchCondition<JavaClass> noGenericExceptions() {
        ArchCondition<JavaClass> noCreationOfThrowable = never(classCallsMethodWhere(targetHasName(Throwable.class, "<init>")));
        ArchCondition<JavaClass> noCreationOfException = never(classCallsMethodWhere(targetHasName(Exception.class, "<init>")));
        ArchCondition<JavaClass> noCreationOfRuntimeException = never(classCallsMethodWhere(targetHasName(RuntimeException.class, "<init>")));

        return noCreationOfThrowable.and(noCreationOfException).and(noCreationOfRuntimeException);
    }

    /**
     * It is generally good practice to throw specific exceptions like {@link java.lang.IllegalArgumentException}
     * or custom exceptions, instead of throwing generic exceptions like {@link java.lang.RuntimeException}.
     */
    public static final OpenArchRule<JavaClass> CLASSES_SHOULD_NOT_THROW_GENERIC_EXCEPTIONS =
            rule(all(JavaClass.class))
                    .should("not throw generic exceptions")
                    .assertedBy(NO_GENERIC_EXCEPTIONS);

    public static final ArchCondition<JavaClass> NO_SETTING_OF_JAVA_UTIL_LOGGING_FIELDS =
            never(classSetsFieldWith(fieldTypeIn("java.util.logging..")));

    /**
     * Most projects use the more powerful LOG4J or Logback instead of java.util.logging, often hidden behind
     * SLF4J. In this case it's important to ensure consistent use of the agreed logging framework.
     */
    public static final OpenArchRule<JavaClass> CLASSES_SHOULD_NOT_USE_JAVA_UTIL_LOGGING =
            rule(all(JavaClass.class))
                    .should("not use java.util.logging")
                    .assertedBy(NO_SETTING_OF_JAVA_UTIL_LOGGING_FIELDS);
}
