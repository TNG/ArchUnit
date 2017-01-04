package com.tngtech.archunit.library;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.lang.ArchRule.Definition.all;
import static com.tngtech.archunit.lang.ArchRule.Definition.classes;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessField;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethod;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchConditions.setFieldWhere;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.callOrigin;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.callTarget;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.targetTypeResidesIn;

public class GeneralCodingRules {
    public static final ArchCondition<JavaClass> NOT_ACCESS_STANDARD_STREAMS = notAccessStandardStreams();

    private static ArchCondition<JavaClass> notAccessStandardStreams() {
        ArchCondition<JavaClass> noAccessToSystemOut = never(accessField(System.class, "out"));
        ArchCondition<JavaClass> noAccessToSystemErr = never(accessField(System.class, "err"));
        ArchCondition<JavaClass> noCallOfPrintStackTrace = never(callMethod("printStackTrace").inHierarchyOf(Throwable.class));

        return noAccessToSystemOut.and(noAccessToSystemErr).and(noCallOfPrintStackTrace).as("not access standard streams");
    }

    /**
     * It is generally good practice to use correct logging instead of writing to the console.
     * <ul>
     * <li>Writing to the console cannot be configured in production</li>
     * <li>Writing to the console is synchronized and can lead to bottle necks</li>
     * </ul>
     */
    public static final ArchRule CLASSES_SHOULD_NOT_ACCESS_STANDARD_STREAMS =
            all(classes()).should(NOT_ACCESS_STANDARD_STREAMS);

    public static final ArchCondition<JavaClass> NOT_THROW_GENERIC_EXCEPTIONS = noGenericExceptions();

    private static ArchCondition<JavaClass> noGenericExceptions() {
        ArchCondition<JavaClass> noCreationOfThrowable =
                never(callMethodWhere(callTarget().isDeclaredIn(Throwable.class).hasName("<init>")));
        ArchCondition<JavaClass> noCreationOfException =
                never(callMethodWhere(callTarget().isDeclaredIn(Exception.class).hasName("<init>")
                        .and(not(callOrigin().isAssignableTo(Exception.class)))));
        ArchCondition<JavaClass> noCreationOfRuntimeException =
                never(callMethodWhere(callTarget().isDeclaredIn(RuntimeException.class).hasName("<init>")
                        .and(not(callOrigin().isAssignableTo(RuntimeException.class)))));

        return noCreationOfThrowable.and(noCreationOfException).and(noCreationOfRuntimeException).as("not throw generic exceptions");
    }

    /**
     * It is generally good practice to throw specific exceptions like {@link java.lang.IllegalArgumentException}
     * or custom exceptions, instead of throwing generic exceptions like {@link java.lang.RuntimeException}.
     */
    public static final ArchRule CLASSES_SHOULD_NOT_THROW_GENERIC_EXCEPTIONS =
            all(classes()).should(NOT_THROW_GENERIC_EXCEPTIONS);

    public static final ArchCondition<JavaClass> NOT_SET_JAVA_UTIL_LOGGING_FIELDS =
            never(setFieldWhere(targetTypeResidesIn("java.util.logging.."))).as("not use java.util.logging");

    /**
     * Most projects use the more powerful LOG4J or Logback instead of java.util.logging, often hidden behind
     * SLF4J. In this case it's important to ensure consistent use of the agreed logging framework.
     */
    public static final ArchRule CLASSES_SHOULD_NOT_USE_JAVA_UTIL_LOGGING =
            all(classes()).should(NOT_SET_JAVA_UTIL_LOGGING_FIELDS);
}
