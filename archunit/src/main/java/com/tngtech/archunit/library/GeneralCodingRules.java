package com.tngtech.archunit.library;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.JavaFieldAccess.Predicates.targetTypeResidesInPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessField;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callCodeUnitWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethod;
import static com.tngtech.archunit.lang.conditions.ArchConditions.setFieldWhere;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.callOrigin;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.callTarget;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class GeneralCodingRules {
    public static final ArchCondition<JavaClass> ACCESS_STANDARD_STREAMS = accessStandardStreams();

    private static ArchCondition<JavaClass> accessStandardStreams() {
        ArchCondition<JavaClass> accessToSystemOut = accessField(System.class, "out");
        ArchCondition<JavaClass> accessToSystemErr = accessField(System.class, "err");
        ArchCondition<JavaClass> callOfPrintStackTrace = callMethod("printStackTrace").inHierarchyOf(Throwable.class);

        return accessToSystemOut.or(accessToSystemErr).or(callOfPrintStackTrace).as("access standard streams");
    }

    /**
     * It is generally good practice to use correct logging instead of writing to the console.
     * <ul>
     * <li>Writing to the console cannot be configured in production</li>
     * <li>Writing to the console is synchronized and can lead to bottle necks</li>
     * </ul>
     */
    public static final ArchRule NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS =
            noClasses().should(ACCESS_STANDARD_STREAMS);

    public static final ArchCondition<JavaClass> THROW_GENERIC_EXCEPTIONS = throwGenericExceptions();

    private static ArchCondition<JavaClass> throwGenericExceptions() {
        ArchCondition<JavaClass> creationOfThrowable =
                callCodeUnitWhere(callTarget().isDeclaredIn(Throwable.class).isConstructor());
        ArchCondition<JavaClass> creationOfException =
                callCodeUnitWhere(callTarget().isDeclaredIn(Exception.class).isConstructor()
                        .and(not(callOrigin().isAssignableTo(Exception.class))));
        ArchCondition<JavaClass> creationOfRuntimeException =
                callCodeUnitWhere(callTarget().isDeclaredIn(RuntimeException.class).isConstructor()
                        .and(not(callOrigin().isAssignableTo(RuntimeException.class))));

        return creationOfThrowable.or(creationOfException).or(creationOfRuntimeException).as("throw generic exceptions");
    }

    /**
     * It is generally good practice to throw specific exceptions like {@link java.lang.IllegalArgumentException}
     * or custom exceptions, instead of throwing generic exceptions like {@link java.lang.RuntimeException}.
     */
    public static final ArchRule NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS =
            noClasses().should(THROW_GENERIC_EXCEPTIONS);

    public static final ArchCondition<JavaClass> USE_JAVA_UTIL_LOGGING =
            setFieldWhere(targetTypeResidesInPackage("java.util.logging.."))
                    .as("use java.util.logging");

    /**
     * Most projects use the more powerful LOG4J or Logback instead of java.util.logging, often hidden behind
     * SLF4J. In this case it's important to ensure consistent use of the agreed logging framework.
     */
    public static final ArchRule NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING =
            noClasses().should(USE_JAVA_UTIL_LOGGING);
}
