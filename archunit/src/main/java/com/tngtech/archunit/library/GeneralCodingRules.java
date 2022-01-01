/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.library;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess.Functions.Get;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.constructor;
import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.originOwner;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessField;
import static com.tngtech.archunit.lang.conditions.ArchConditions.beAnnotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callCodeUnitWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.dependOnClassesThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.setFieldWhere;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.is;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * GeneralCodingRules provides a set of very general {@link ArchCondition ArchConditions}
 * and {@link ArchRule ArchRules} for coding that might be useful in various projects.
 *
 * <p>
 * When checking these rules, it is always important to remember that all necessary classes need to be
 * imported. E.g. if {@link #ACCESS_STANDARD_STREAMS} is checked, which looks for calls on classes
 * assignable to {@link Throwable}, it is important to ensure that {@link Exception} and {@link RuntimeException}
 * are imported as well, otherwise ArchUnit does not know about the inheritance structure of these exceptions,
 * and thus will not consider a call of {@link RuntimeException} a violation, since it does not know that
 * {@link RuntimeException} extends {@link Throwable}.
 * For further information refer to {@link ClassFileImporter}.
 * </p>
 */
public final class GeneralCodingRules {
    private GeneralCodingRules() {
    }

    /**
     * A condition that matches classes that access {@code System.out} or {@code System.err}.
     *
     * <p>
     * Example:
     * <pre>{@code
     * System.out.println("foo"); // matches
     * System.err.println("bar"); // matches
     *
     * OutputStream out = System.out; // matches
     * out.write(bytes);
     *
     * try {
     *     // ...
     * } catch (Exception e) {
     *     e.printStackTrace(); // matches
     * }
     * }</pre>
     * </p>
     *
     * <p>
     * For information about checking this condition, refer to {@link GeneralCodingRules}.
     * </p>
     *
     * @see #NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchCondition<JavaClass> ACCESS_STANDARD_STREAMS = accessStandardStreams();

    private static ArchCondition<JavaClass> accessStandardStreams() {
        ArchCondition<JavaClass> accessToSystemOut = accessField(System.class, "out");
        ArchCondition<JavaClass> accessToSystemErr = accessField(System.class, "err");
        ArchCondition<JavaClass> callOfPrintStackTrace = callMethodWhere(
                target(name("printStackTrace"))
                        .and(target(owner(assignableTo(Throwable.class))))
                        .and(target(rawParameterTypes(new Class[0]))));

        return accessToSystemOut.or(accessToSystemErr).or(callOfPrintStackTrace).as("access standard streams");
    }

    /**
     * A rule that checks that none of the given classes access the standard streams
     * {@code System.out} and {@code System.err}.
     *
     * <p>
     * It is generally good practice to use correct logging instead of writing to the console.
     * <ul>
     * <li>Writing to the console cannot be configured in production</li>
     * <li>Writing to the console is synchronized and can lead to bottle necks</li>
     * </ul>
     * </p>
     *
     * <p>
     * Example:
     * <pre>{@code
     * System.out.println("foo"); // violation
     * System.err.println("bar"); // violation
     *
     * OutputStream out = System.out; // violation
     * out.write(bytes);
     *
     * try {
     *     // ...
     * } catch (Exception e) {
     *     e.printStackTrace(); // violation
     * }
     * }</pre>
     * </p>
     *
     * <p>
     * For information about checking this rule, refer to {@link GeneralCodingRules}.
     * </p>
     *
     * @see #ACCESS_STANDARD_STREAMS
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchRule NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS =
            noClasses().should(ACCESS_STANDARD_STREAMS);

    /**
     * A condition that matches classes that throw generic exceptions like
     * {@code Exception}, {@code RuntimeException}, or {@code Throwable}.
     * More precisely, the condition matches when a constructor of the
     * mentioned classes is called.
     *
     * <p>
     * Example:
     * <pre>{@code
     * throw new Exception(); // matches
     * throw new RuntimeException("error"); // matches
     * throw new Throwable("error"); // matches
     *
     * class CustomException extends Throwable { // matches
     * }
     *
     * class CustomException extends Exception {
     *     CustomException() {
     *         super("error"); // does not match
     *     }
     * }
     * }</pre>
     * </p>
     *
     * <p>
     * For information about checking this condition, refer to {@link GeneralCodingRules}.
     * </p>
     *
     * @see #NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchCondition<JavaClass> THROW_GENERIC_EXCEPTIONS = throwGenericExceptions();

    private static ArchCondition<JavaClass> throwGenericExceptions() {
        ArchCondition<JavaClass> creationOfThrowable =
                callCodeUnitWhere(target(is(constructor()).and(is(declaredIn(Throwable.class)))));
        ArchCondition<JavaClass> creationOfException =
                callCodeUnitWhere(target(is(constructor()).and(is(declaredIn(Exception.class))))
                        .and(not(originOwner(is(assignableTo(Exception.class))))));
        ArchCondition<JavaClass> creationOfRuntimeException =
                callCodeUnitWhere(target(is(constructor()).and(is(declaredIn(RuntimeException.class))))
                        .and(not(originOwner(is(assignableTo(RuntimeException.class))))));

        return creationOfThrowable.or(creationOfException).or(creationOfRuntimeException).as("throw generic exceptions");
    }

    /**
     * A rule that checks that none of the given classes throw generic exceptions like
     * {@code Exception}, {@code RuntimeException}, or {@code Throwable}.
     * More precisely, the rule reports a violation when a constructor of the
     * mentioned classes is called.
     *
     * <p>
     * It is generally good practice to throw specific exceptions like {@link java.lang.IllegalArgumentException}
     * or custom exceptions, instead of throwing generic exceptions like {@link java.lang.RuntimeException}.
     * </p>
     *
     * <p>
     * Example:
     * <pre>{@code
     * throw new Exception(); // violation
     * throw new RuntimeException("error"); // violation
     * throw new Throwable("error"); // violation
     *
     * class CustomException extends Throwable { // violation
     * }
     *
     * class CustomException extends Exception {
     *     CustomException() {
     *         super("error"); // no violation
     *     }
     * }
     * }</pre>
     * </p>
     *
     * <p>
     * For information about checking this rule, refer to {@link GeneralCodingRules}.
     * </p>
     *
     * @see #THROW_GENERIC_EXCEPTIONS
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchRule NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS =
            noClasses().should(THROW_GENERIC_EXCEPTIONS);

    /**
     * A condition that matches classes that access Java Util Logging.
     *
     * <p>
     * Example:
     * <pre>{@code
     * import java.util.logging.Logger;
     *
     * Logger logger = Logger.getLogger("Example"); // matches
     * }</pre>
     * </p>
     *
     * <p>
     * For information about checking this condition, refer to {@link GeneralCodingRules}.
     * </p>
     *
     * @see #NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchCondition<JavaClass> USE_JAVA_UTIL_LOGGING =
            setFieldWhere(resideInAPackage("java.util.logging..")
                    .onResultOf(Get.<JavaFieldAccess, FieldAccessTarget>target().then(GET_RAW_TYPE)))
                    .as("use java.util.logging");

    /**
     * A rule that checks that none of the given classes access Java Util Logging.
     *
     * <p>
     * Most projects use the more powerful LOG4J or Logback instead of java.util.logging, often hidden behind
     * SLF4J. In this case it's important to ensure consistent use of the agreed logging framework.
     * </p>
     *
     * <p>
     * Example:
     * <pre>{@code
     * import java.util.logging.Logger;
     *
     * Logger logger = Logger.getLogger("Example"); // violation
     * }</pre>
     * </p>
     *
     * <p>
     * For information about checking this rule, refer to {@link GeneralCodingRules}.
     * </p>
     *
     * @see #USE_JAVA_UTIL_LOGGING
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchRule NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING =
            noClasses().should(USE_JAVA_UTIL_LOGGING);

    /**
     * A condition that matches classes that access JodaTime.
     *
     * <p>
     * Example:
     * <pre>{@code
     * import org.joda.time.DateTime;
     *
     * DateTime now = DateTime.now(); // matches
     * }</pre>
     * </p>
     *
     * <p>
     * For information about checking this condition, refer to {@link GeneralCodingRules}.
     * </p>
     *
     * @see #NO_CLASSES_SHOULD_USE_JODATIME
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchCondition<JavaClass> USE_JODATIME =
            dependOnClassesThat(resideInAPackage("org.joda.time"))
                    .as("use JodaTime");

    /**
     * A rule that checks that none of the given classes access JodaTime.
     *
     * <p>
     * Modern Java projects use the [java.time] API instead of the JodaTime library.
     * </p>
     *
     * <p>
     * Example:
     * <pre>{@code
     * import org.joda.time.DateTime;
     *
     * DateTime now = DateTime.now(); // violation
     * }</pre>
     * </p>
     *
     * <p>
     * For information about checking this rule, refer to {@link GeneralCodingRules}.
     * </p>
     *
     * @see #USE_JODATIME
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchRule NO_CLASSES_SHOULD_USE_JODATIME =
            noClasses().should(USE_JODATIME).because("modern Java projects use the [java.time] API instead");

    /**
     * A condition that matches fields that have an annotation for injection.
     *
     * <p>
     * Example:
     * <pre>{@code
     * class Example {
     *
     *     @Resource
     *     DataSource dataSource; // matches
     *
     *     @Inject
     *     File configFile; // matches
     *
     *     @Autowired
     *     CustomerService customerService; // matches
     * }
     * }</pre>
     * </p>
     *
     * <p>
     * For information about checking this condition, refer to {@link GeneralCodingRules}.
     * </p>
     *
     * @see #NO_CLASSES_SHOULD_USE_FIELD_INJECTION
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchCondition<JavaField> BE_ANNOTATED_WITH_AN_INJECTION_ANNOTATION = beAnnotatedWithAnInjectionAnnotation();

    private static ArchCondition<JavaField> beAnnotatedWithAnInjectionAnnotation() {
        ArchCondition<JavaField> annotatedWithSpringAutowired = beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");
        ArchCondition<JavaField> annotatedWithSpringValue = beAnnotatedWith("org.springframework.beans.factory.annotation.Value");
        ArchCondition<JavaField> annotatedWithGuiceInject = beAnnotatedWith("com.google.inject.Inject");
        ArchCondition<JavaField> annotatedWithJakartaInject = beAnnotatedWith("javax.inject.Inject");
        ArchCondition<JavaField> annotatedWithJakartaResource = beAnnotatedWith("javax.annotation.Resource");
        return annotatedWithSpringAutowired.or(annotatedWithSpringValue)
                .or(annotatedWithGuiceInject)
                .or(annotatedWithJakartaInject).or(annotatedWithJakartaResource)
                .as("be annotated with an injection annotation");
    }

    /**
     * A rule that checks that none of the given classes uses field injection.
     *
     * <p>
     * Field injection is seen as an anti-pattern.
     * It is a good practice to use constructor injection for mandatory dependencies and setter injection for optional dependencies.
     * </p>
     *
     * <p>
     * Example:
     * <pre>{@code
     * class Example {
     *
     *     @Resource
     *     DataSource dataSource; // violation
     *
     *     @Inject
     *     File configFile; // violation
     *
     *     @Autowired
     *     CustomerService customerService; // violation
     * }
     * }</pre>
     * </p>
     *
     * <p>
     * For information about checking this rule, refer to {@link GeneralCodingRules}.
     * </p>
     *
     * @see #BE_ANNOTATED_WITH_AN_INJECTION_ANNOTATION
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchRule NO_CLASSES_SHOULD_USE_FIELD_INJECTION =
            noFields().should(BE_ANNOTATED_WITH_AN_INJECTION_ANNOTATION)
                    .as("no classes should use field injection")
                    .because("field injection is considered harmful; use constructor injection or setter injection instead; "
                            + "see https://stackoverflow.com/q/39890849 for detailed explanations");
}
