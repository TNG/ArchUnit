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
package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Function.Functions;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.AbstractClassesTransformer;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.AbstractGivenCodeUnitsInternal.GivenCodeUnitsInternal;
import com.tngtech.archunit.lang.syntax.AbstractGivenMembersInternal.GivenMembersInternal;
import com.tngtech.archunit.lang.syntax.elements.GivenClass;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;
import com.tngtech.archunit.lang.syntax.elements.GivenCodeUnits;
import com.tngtech.archunit.lang.syntax.elements.GivenConstructors;
import com.tngtech.archunit.lang.syntax.elements.GivenFields;
import com.tngtech.archunit.lang.syntax.elements.GivenMembers;
import com.tngtech.archunit.lang.syntax.elements.GivenMethods;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.lang.Priority.MEDIUM;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static java.util.Collections.singleton;

public final class ArchRuleDefinition {
    private ArchRuleDefinition() {
    }

    /**
     * @see Creator#all(ClassesTransformer)
     */
    @PublicAPI(usage = ACCESS)
    public static <TYPE> GivenObjects<TYPE> all(ClassesTransformer<TYPE> classesTransformer) {
        return priority(MEDIUM).all(classesTransformer);
    }

    /**
     * @see Creator#no(ClassesTransformer)
     */
    @PublicAPI(usage = ACCESS)
    public static <TYPE> GivenObjects<TYPE> no(ClassesTransformer<TYPE> classesTransformer) {
        return priority(MEDIUM).no(classesTransformer);
    }

    @PublicAPI(usage = ACCESS)
    public static Creator priority(Priority priority) {
        return new Creator(priority);
    }

    @PublicAPI(usage = ACCESS)
    public static GivenClasses classes() {
        return priority(MEDIUM).classes();
    }

    @PublicAPI(usage = ACCESS)
    public static GivenClasses noClasses() {
        return priority(MEDIUM).noClasses();
    }

    @PublicAPI(usage = ACCESS)
    public static GivenClass theClass(Class<?> clazz) {
        return priority(MEDIUM).theClass(clazz);
    }

    @PublicAPI(usage = ACCESS)
    public static GivenClass theClass(String className) {
        return priority(MEDIUM).theClass(className);
    }

    @PublicAPI(usage = ACCESS)
    public static GivenClass noClass(Class<?> clazz) {
        return priority(MEDIUM).noClass(clazz);
    }

    @PublicAPI(usage = ACCESS)
    public static GivenClass noClass(String className) {
        return priority(MEDIUM).noClass(className);
    }

    @PublicAPI(usage = ACCESS)
    public static GivenMembers<JavaMember> members() {
        return priority(MEDIUM).members();
    }

    @PublicAPI(usage = ACCESS)
    public static GivenMembers<JavaMember> noMembers() {
        return priority(MEDIUM).noMembers();
    }

    @PublicAPI(usage = ACCESS)
    public static GivenFields fields() {
        return priority(MEDIUM).fields();
    }

    @PublicAPI(usage = ACCESS)
    public static GivenFields noFields() {
        return priority(MEDIUM).noFields();
    }

    @PublicAPI(usage = ACCESS)
    public static GivenCodeUnits<JavaCodeUnit> codeUnits() {
        return priority(MEDIUM).codeUnits();
    }

    @PublicAPI(usage = ACCESS)
    public static GivenCodeUnits<JavaCodeUnit> noCodeUnits() {
        return priority(MEDIUM).noCodeUnits();
    }

    @PublicAPI(usage = ACCESS)
    public static GivenConstructors constructors() {
        return priority(MEDIUM).constructors();
    }

    @PublicAPI(usage = ACCESS)
    public static GivenConstructors noConstructors() {
        return priority(MEDIUM).noConstructors();
    }

    @PublicAPI(usage = ACCESS)
    public static GivenMethods methods() {
        return priority(MEDIUM).methods();
    }

    @PublicAPI(usage = ACCESS)
    public static GivenMethods noMethods() {
        return priority(MEDIUM).noMethods();
    }

    public static final class Creator {
        private final Priority priority;

        private Creator(Priority priority) {
            this.priority = priority;
        }

        @PublicAPI(usage = ACCESS)
        public GivenClasses classes() {
            return new GivenClassesInternal(priority, Transformers.classes());
        }

        @PublicAPI(usage = ACCESS)
        public GivenClasses noClasses() {
            return new GivenClassesInternal(
                    priority,
                    Transformers.classes().as("no " + Transformers.classes().getDescription()),
                    ArchRuleDefinition.<JavaClass>negateCondition());
        }

        @PublicAPI(usage = ACCESS)
        public GivenMembers<JavaMember> members() {
            return new GivenMembersInternal(priority, Transformers.members());
        }

        @PublicAPI(usage = ACCESS)
        public GivenMembers<JavaMember> noMembers() {
            return new GivenMembersInternal(
                    priority,
                    Transformers.members().as("no " + Transformers.members().getDescription()),
                    ArchRuleDefinition.<JavaMember>negateCondition());
        }

        @PublicAPI(usage = ACCESS)
        public GivenFields fields() {
            return new GivenFieldsInternal(priority, Transformers.fields());
        }

        @PublicAPI(usage = ACCESS)
        public GivenFields noFields() {
            return new GivenFieldsInternal(
                    priority,
                    Transformers.fields().as("no " + Transformers.fields().getDescription()),
                    ArchRuleDefinition.<JavaField>negateCondition());
        }

        @PublicAPI(usage = ACCESS)
        public GivenCodeUnits<JavaCodeUnit> codeUnits() {
            return new GivenCodeUnitsInternal(priority, Transformers.codeUnits());
        }

        @PublicAPI(usage = ACCESS)
        public GivenCodeUnits<JavaCodeUnit> noCodeUnits() {
            return new GivenCodeUnitsInternal(
                    priority,
                    Transformers.codeUnits().as("no " + Transformers.codeUnits().getDescription()),
                    ArchRuleDefinition.<JavaCodeUnit>negateCondition());
        }

        @PublicAPI(usage = ACCESS)
        public GivenConstructors constructors() {
            return new GivenConstructorsInternal(priority, Transformers.constructors());
        }

        @PublicAPI(usage = ACCESS)
        public GivenConstructors noConstructors() {
            return new GivenConstructorsInternal(
                    priority,
                    Transformers.constructors().as("no " + Transformers.constructors().getDescription()),
                    ArchRuleDefinition.<JavaConstructor>negateCondition());
        }

        @PublicAPI(usage = ACCESS)
        public GivenMethods methods() {
            return new GivenMethodsInternal(priority, Transformers.methods());
        }

        @PublicAPI(usage = ACCESS)
        public GivenMethods noMethods() {
            return new GivenMethodsInternal(
                    priority,
                    Transformers.methods().as("no " + Transformers.methods().getDescription()),
                    ArchRuleDefinition.<JavaMethod>negateCondition());
        }

        /**
         * Takes a {@link ClassesTransformer} to specify how the set of objects of interest is to be created
         * from {@link JavaClasses} (which are the general input obtained from a {@link ClassFileImporter}).
         *
         * @param <TYPE>             The target type to which the later used {@link ArchCondition ArchCondition&lt;TYPE&gt;}
         *                           will have to refer to
         * @param classesTransformer Transformer specifying how the imported {@link JavaClasses} are to be transformed
         * @return {@link GivenObjects} to guide the creation of an {@link ArchRule}
         */
        @PublicAPI(usage = ACCESS)
        public <TYPE> GivenObjects<TYPE> all(ClassesTransformer<TYPE> classesTransformer) {
            return new GivenObjectsInternal<>(priority, classesTransformer);
        }

        /**
         * Same as {@link #all(ClassesTransformer)}, but negates the following condition.
         */
        @PublicAPI(usage = ACCESS)
        public <TYPE> GivenObjects<TYPE> no(ClassesTransformer<TYPE> classesTransformer) {
            return new GivenObjectsInternal<>(
                    priority,
                    classesTransformer.as("no " + classesTransformer.getDescription()),
                    ArchRuleDefinition.<TYPE>negateCondition());
        }

        @PublicAPI(usage = ACCESS)
        public GivenClass theClass(final Class<?> clazz) {
            return theClass(clazz.getName());
        }

        @PublicAPI(usage = ACCESS)
        public GivenClass theClass(final String className) {
            ClassesTransformer<JavaClass> theClass = theClassTransformer(className);
            return new GivenClassInternal(priority, theClass, Functions.<ArchCondition<JavaClass>>identity());
        }

        @PublicAPI(usage = ACCESS)
        public GivenClass noClass(final Class<?> clazz) {
            return noClass(clazz.getName());
        }

        @PublicAPI(usage = ACCESS)
        public GivenClass noClass(final String className) {
            ClassesTransformer<JavaClass> noClass = theClassTransformer(className).as("no class " + className);
            return new GivenClassInternal(priority, noClass, ArchRuleDefinition.<JavaClass>negateCondition());
        }

        private ClassesTransformer<JavaClass> theClassTransformer(final String className) {
            return new AbstractClassesTransformer<JavaClass>("the class " + className) {
                @Override
                public Iterable<JavaClass> doTransform(JavaClasses classes) {
                    return singleton(classes.get(className));
                }
            };
        }
    }

    private static <T> Function<ArchCondition<T>, ArchCondition<T>> negateCondition() {
        return new Function<ArchCondition<T>, ArchCondition<T>>() {
            @Override
            public ArchCondition<T> apply(ArchCondition<T> condition) {
                return never(condition).as(condition.getDescription());
            }
        };
    }
}
