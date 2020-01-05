/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
import static com.tngtech.archunit.lang.conditions.ArchConditions.callCodeUnitWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.dependOnClassesThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.setFieldWhere;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.is;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * When checking these rules, it is always important to remember that all necessary classes need to be
 * imported. E.g. if {@link #ACCESS_STANDARD_STREAMS} is checked, which looks for calls on classes
 * assignable to {@link Throwable}, it is important to ensure that {@link Exception} and {@link RuntimeException}
 * are imported as well, otherwise ArchUnit does not know about the inheritance structure of these exceptions,
 * and thus will not consider a call of {@link RuntimeException} a violation, since it does not know that
 * {@link RuntimeException} extends {@link Throwable}.
 * For further information refer to {@link ClassFileImporter}.
 */
public final class GeneralCodingRules {
    private GeneralCodingRules() {
    }

    /**
     * For information about checking this condition, refer to {@link GeneralCodingRules}.
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
     * It is generally good practice to use correct logging instead of writing to the console.
     * <ul>
     * <li>Writing to the console cannot be configured in production</li>
     * <li>Writing to the console is synchronized and can lead to bottle necks</li>
     * </ul>
     *
     * For information about checking this rule, refer to {@link GeneralCodingRules}.
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchRule NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS =
            noClasses().should(ACCESS_STANDARD_STREAMS);

    /**
     * For information about checking this condition, refer to {@link GeneralCodingRules}.
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
     * It is generally good practice to throw specific exceptions like {@link java.lang.IllegalArgumentException}
     * or custom exceptions, instead of throwing generic exceptions like {@link java.lang.RuntimeException}.
     * <br>
     * For information about checking this rule, refer to {@link GeneralCodingRules}.
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchRule NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS =
            noClasses().should(THROW_GENERIC_EXCEPTIONS);

    /**
     * For information about checking this condition, refer to {@link GeneralCodingRules}.
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchCondition<JavaClass> USE_JAVA_UTIL_LOGGING =
            setFieldWhere(resideInAPackage("java.util.logging..")
                    .onResultOf(Get.<JavaFieldAccess, FieldAccessTarget>target().then(GET_RAW_TYPE)))
                    .as("use java.util.logging");

    /**
     * Most projects use the more powerful LOG4J or Logback instead of java.util.logging, often hidden behind
     * SLF4J. In this case it's important to ensure consistent use of the agreed logging framework.
     * <br>
     * For information about checking this rule, refer to {@link GeneralCodingRules}.
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchRule NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING =
            noClasses().should(USE_JAVA_UTIL_LOGGING);

    /**
     * For information about checking this condition, refer to {@link GeneralCodingRules}.
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchCondition<JavaClass> USE_JODATIME =
            dependOnClassesThat(resideInAPackage("org.joda.time"))
                    .as("use JodaTime");

    /**
     * Modern Java projects use the [java.time] API instead of the JodaTime library
     * <br>
     * For information about checking this rule, refer to {@link GeneralCodingRules}.
     */
    @PublicAPI(usage = ACCESS)
    public static final ArchRule NO_CLASSES_SHOULD_USE_JODATIME =
            noClasses().should(USE_JODATIME).because("modern Java projects use the [java.time] API instead");
}
