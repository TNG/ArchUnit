/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.modules.syntax;

import java.lang.annotation.Annotation;
import java.util.function.Function;
import java.util.function.Predicate;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ArchModules;
import com.tngtech.archunit.library.modules.syntax.AbstractGivenModulesInternal.GivenModulesByAnnotationInternal;
import com.tngtech.archunit.library.modules.syntax.AbstractGivenModulesInternal.GivenModulesInternal;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * @see #modules()
 */
@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public final class ModuleRuleDefinition {
    private ModuleRuleDefinition() {
    }

    /**
     * Entrypoint to define {@link ArchRule rules} based on {@link ArchModules}.
     *
     * @return A syntax element to create {@link ArchModules} {@link ArchRule rules}
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static Creator modules() {
        return new Creator();
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static final class Creator {
        private Creator() {
        }

        /**
         * @see ArchModules#defineBy(ArchModules.IdentifierAssociation)
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public GenericDefinition definedBy(DescribedFunction<JavaClass, ArchModule.Identifier> identifierFunction) {
            return new GenericDefinition(identifierFunction);
        }

        /**
         * @see ArchModules#defineByRootClasses(Predicate)
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public RootClassesDefinition<ArchModule.Descriptor> definedByRootClasses(DescribedPredicate<? super JavaClass> predicate) {
            return RootClassesDefinition.create(predicate);
        }

        /**
         * @see ArchModules#defineByAnnotation(Class)
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public <A extends Annotation> GivenModulesByAnnotation<A> definedByAnnotation(Class<A> annotationType) {
            return new GivenModulesByAnnotationInternal<>(classes -> ArchModules
                    .defineByAnnotation(annotationType)
                    .modularize(classes)
            ).as("modules defined by annotation @%s", annotationType.getSimpleName());
        }

        /**
         * @see ArchModules#defineByPackages(String)
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public PackagesDefinition definedByPackages(String packageIdentifier) {
            return new PackagesDefinition(packageIdentifier);
        }

        /**
         * @see ArchModules#defineByAnnotation(Class, Function)
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public <A extends Annotation> GivenModulesByAnnotation<A> definedByAnnotation(Class<A> annotationType, Function<A, String> nameFunction) {
            return new GivenModulesByAnnotationInternal<>(classes -> ArchModules
                    .defineByAnnotation(annotationType, nameFunction)
                    .modularize(classes)
            ).as("modules defined by annotation @%s", annotationType.getSimpleName());
        }
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static final class RootClassesDefinition<DESCRIPTOR extends ArchModule.Descriptor> implements GivenModules<DESCRIPTOR> {
        private final Predicate<? super JavaClass> rootClassPredicate;
        private final Function<? super JavaClass, DESCRIPTOR> descriptorFunction;
        private final String description;

        private RootClassesDefinition(Predicate<? super JavaClass> rootClassPredicate, Function<? super JavaClass, DESCRIPTOR> descriptorFunction, String description) {
            this.rootClassPredicate = rootClassPredicate;
            this.descriptorFunction = descriptorFunction;
            this.description = description;
        }

        /**
         * @see ArchModules.CreatorByRootClass#describeModuleByRootClass(ArchModules.RootClassDescriptorCreator)
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public <D extends ArchModule.Descriptor> GivenModules<D> derivingModuleFromRootClassBy(DescribedFunction<? super JavaClass, D> descriptorFunction) {
            return new RootClassesDefinition<>(rootClassPredicate, descriptorFunction, description + " deriving module from root class by " + descriptorFunction.getDescription());
        }

        @Override
        public ArchRule should(ArchCondition<? super ArchModule<DESCRIPTOR>> condition) {
            return newGivenModules().should(condition);
        }

        @Override
        public GivenModulesConjunction<DESCRIPTOR> that(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate) {
            return newGivenModules().that(predicate);
        }

        @Override
        public ModulesShould<DESCRIPTOR> should() {
            return newGivenModules().should();
        }

        @Override
        public GivenModules<DESCRIPTOR> as(String description, Object... args) {
            return newGivenModules().as(description, args);
        }

        private GivenModules<DESCRIPTOR> newGivenModules() {
            return new GivenModulesInternal<>(classes -> ArchModules
                    .defineByRootClasses(rootClassPredicate)
                    .describeModuleByRootClass((__, rootClass) -> descriptorFunction.apply(rootClass))
                    .modularize(classes)
            ).as(description);
        }

        private static RootClassesDefinition<ArchModule.Descriptor> create(DescribedPredicate<? super JavaClass> rootClassPredicate) {
            return new RootClassesDefinition<>(
                    rootClassPredicate,
                    javaClass -> ArchModule.Descriptor.create(javaClass.getPackageName()),
                    "modules defined by root classes " + rootClassPredicate.getDescription());
        }
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static final class PackagesDefinition implements GivenModules<ArchModule.Descriptor> {
        private final String description;
        private final ArchModules.Creator creator;

        PackagesDefinition(String packageIdentifier) {
            this(ArchModules.defineByPackages(packageIdentifier), String.format("modules defined by packages '%s'", packageIdentifier));
        }

        private PackagesDefinition(ArchModules.Creator creator, String description) {
            this.creator = creator;
            this.description = description;
        }

        @SuppressWarnings("unchecked") // The descriptor of ArchModules<DESCRIPTOR> is covariant, so we can always "upcast"
        private GivenModules<ArchModule.Descriptor> newGivenModules() {
            return new GivenModulesInternal<>(classes -> (ArchModules<ArchModule.Descriptor>) creator.modularize(classes)).as(description);
        }

        /**
         * @see ArchModules.Creator#deriveNameFromPattern(String)
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public PackagesDefinition derivingNameFromPattern(String namePattern) {
            return new PackagesDefinition(creator.deriveNameFromPattern(namePattern), description + String.format(" deriving name from pattern '%s'", namePattern));
        }

        @Override
        public ArchRule should(ArchCondition<? super ArchModule<ArchModule.Descriptor>> condition) {
            return newGivenModules().should(condition);
        }

        @Override
        public GivenModulesConjunction<ArchModule.Descriptor> that(DescribedPredicate<? super ArchModule<ArchModule.Descriptor>> predicate) {
            return newGivenModules().that(predicate);
        }

        @Override
        public ModulesShould<ArchModule.Descriptor> should() {
            return newGivenModules().should();
        }

        @Override
        public GivenModules<ArchModule.Descriptor> as(String description, Object... args) {
            return newGivenModules().as(description, args);
        }
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static final class GenericDefinition {
        private final DescribedFunction<JavaClass, ArchModule.Identifier> identifierFunction;

        private GenericDefinition(DescribedFunction<JavaClass, ArchModule.Identifier> identifierFunction) {
            this.identifierFunction = identifierFunction;
        }

        /**
         * @see ArchModules.Creator#describeBy(ArchModules.DescriptorCreator)
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public <D extends ArchModule.Descriptor> GivenModules<D> derivingModule(DescriptorFunction<D> descriptorFunction) {
            return new GivenModulesInternal<>(classes -> ArchModules
                    .defineBy(identifierFunction::apply)
                    .describeBy(descriptorFunction::apply)
                    .modularize(classes)
            ).as("modules defined by %s deriving module %s", identifierFunction.getDescription(), descriptorFunction.getDescription());
        }
    }
}
