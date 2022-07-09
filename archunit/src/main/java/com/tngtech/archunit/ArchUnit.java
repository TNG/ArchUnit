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
package com.tngtech.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.lang.syntax.elements.GivenClass;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;
import com.tngtech.archunit.lang.syntax.elements.GivenCodeUnits;
import com.tngtech.archunit.lang.syntax.elements.GivenConstructors;
import com.tngtech.archunit.lang.syntax.elements.GivenFields;
import com.tngtech.archunit.lang.syntax.elements.GivenMembers;
import com.tngtech.archunit.lang.syntax.elements.GivenMethods;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;
import com.tngtech.archunit.library.Architectures.OnionArchitecture;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public class ArchUnit {

    public static class RuleDefinitions {

        /**
         * @see ArchRuleDefinition#all(ClassesTransformer)
         */
        @PublicAPI(usage = ACCESS)
        public static <TYPE> GivenObjects<TYPE> all(ClassesTransformer<TYPE> classesTransformer) {
            return ArchRuleDefinition.all(classesTransformer);
        }

        /**
         * @see ArchRuleDefinition#no(ClassesTransformer)
         */
        @PublicAPI(usage = ACCESS)
        public static <TYPE> GivenObjects<TYPE> no(ClassesTransformer<TYPE> classesTransformer) {
            return ArchRuleDefinition.no(classesTransformer);
        }

        @PublicAPI(usage = ACCESS)
        public static ArchRuleDefinition.Creator priority(Priority priority) {
            return ArchRuleDefinition.priority(priority);
        }

        @PublicAPI(usage = ACCESS)
        public static GivenClasses classes() {
            return ArchRuleDefinition.classes();
        }

        @PublicAPI(usage = ACCESS)
        public static GivenClasses noClasses() {
            return ArchRuleDefinition.noClasses();
        }

        @PublicAPI(usage = ACCESS)
        public static GivenClass theClass(Class<?> clazz) {
            return ArchRuleDefinition.theClass(clazz);
        }

        @PublicAPI(usage = ACCESS)
        public static GivenClass theClass(String className) {
            return ArchRuleDefinition.theClass(className);
        }

        @PublicAPI(usage = ACCESS)
        public static GivenClass noClass(Class<?> clazz) {
            return ArchRuleDefinition.noClass(clazz);
        }

        @PublicAPI(usage = ACCESS)
        public static GivenClass noClass(String className) {
            return ArchRuleDefinition.noClass(className);
        }

        @PublicAPI(usage = ACCESS)
        public static GivenMembers<JavaMember> members() {
            return ArchRuleDefinition.members();
        }

        @PublicAPI(usage = ACCESS)
        public static GivenMembers<JavaMember> noMembers() {
            return ArchRuleDefinition.noMembers();
        }

        @PublicAPI(usage = ACCESS)
        public static GivenFields fields() {
            return ArchRuleDefinition.fields();
        }

        @PublicAPI(usage = ACCESS)
        public static GivenFields noFields() {
            return ArchRuleDefinition.noFields();
        }

        @PublicAPI(usage = ACCESS)
        public static GivenCodeUnits<JavaCodeUnit> codeUnits() {
            return ArchRuleDefinition.codeUnits();
        }

        @PublicAPI(usage = ACCESS)
        public static GivenCodeUnits<JavaCodeUnit> noCodeUnits() {
            return ArchRuleDefinition.noCodeUnits();
        }

        @PublicAPI(usage = ACCESS)
        public static GivenConstructors constructors() {
            return ArchRuleDefinition.constructors();
        }

        @PublicAPI(usage = ACCESS)
        public static GivenConstructors noConstructors() {
            return ArchRuleDefinition.noConstructors();
        }

        @PublicAPI(usage = ACCESS)
        public static GivenMethods methods() {
            return ArchRuleDefinition.methods();
        }

        @PublicAPI(usage = ACCESS)
        public static GivenMethods noMethods() {
            return ArchRuleDefinition.noMethods();
        }

        /**
         * @see SlicesRuleDefinition#slices()
         */
        @PublicAPI(usage = ACCESS)
        public static SlicesRuleDefinition.Creator slices() {
            return SlicesRuleDefinition.slices();
        }

        /**
         * @see LayeredArchitecture#layeredArchitecture()
         */
        @PublicAPI(usage = ACCESS)
        public static LayeredArchitecture.DependencySettings layeredArchitecture() {
            return Architectures.layeredArchitecture();
        }

        /**
         * @see OnionArchitecture#onionArchitecture()
         */
        @PublicAPI(usage = ACCESS)
        public static OnionArchitecture onionArchitecture() {
            return Architectures.onionArchitecture();
        }
    }

    public static class Predicates {
        public static class ForJavaClass {
            public static DescribedPredicate<JavaClass> simpleName(String name) {
                return JavaClass.Predicates.simpleName(name);
            }
        }

        public static class ForJavaMethod {
        }

        public static class ForJavaAccess {
        }

        public static class ForDependency {
        }

        public static class ForJavaMember {
        }

        public static class ForJavaCodeUnit {
        }

        public static class ForJavaField {
        }

        public static class ForJavaCall {
        }

        public static class ForJavaConstructorCall {
        }

        public static class ForJavaCodeUnitReference {
        }

        public static class ForJavaMethodCall {
        }

        public static class ForJavaConstructor {
        }

        public static class ForJavaCodeUnitAccess {
        }

        public static class ForJavaFieldAccess {
        }

        public static class ForJavaMethodReference {
        }

        public static class ForJavaStaticInitializer {
        }

        public static class ForJavaConstructorReference {
        }

        public static class ForAccessTarget {
            @PublicAPI(usage = ACCESS)
            public static DescribedPredicate<AccessTarget> constructor() {
                return AccessTarget.Predicates.constructor();
            }
        }

        public static class ForCodeUnitAccessTarget extends ForAccessTarget {
        }

        public static class ForFieldAccessTarget extends ForAccessTarget {
        }

        public static class ForCodeUnitReferenceTarget extends ForCodeUnitAccessTarget {
        }

        public static class ForCodeUnitCallTarget extends ForCodeUnitAccessTarget {
        }

        public static class ForConstructorReferenceTarget extends ForCodeUnitReferenceTarget {
        }

        public static class ForMethodReferenceTarget extends ForCodeUnitReferenceTarget {
        }

        public static class ForConstructorCallTarget extends ForCodeUnitCallTarget {
        }

        public static class ForMethodCallTarget extends ForCodeUnitCallTarget {
        }
    }

    public static class Conditions {

    }
}
