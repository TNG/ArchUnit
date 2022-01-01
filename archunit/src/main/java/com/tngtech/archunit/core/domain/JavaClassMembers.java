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
package com.tngtech.archunit.core.domain;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Optional;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaModifier.ENUM;
import static com.tngtech.archunit.core.domain.JavaModifier.SYNTHETIC;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;

class JavaClassMembers {
    private final JavaClass owner;
    private final Set<JavaField> fields;
    private final Set<JavaCodeUnit> codeUnits;
    private final Set<JavaMethod> methods;
    private final Set<JavaMember> members;
    private final Set<JavaConstructor> constructors;
    private final Optional<JavaStaticInitializer> staticInitializer;
    private final Supplier<Set<JavaMethod>> allMethods;
    private final Supplier<Set<JavaConstructor>> allConstructors;
    private final Supplier<Set<JavaField>> allFields;
    private final Supplier<Set<JavaMember>> allMembers = Suppliers.memoize(new Supplier<Set<JavaMember>>() {
        @Override
        public Set<JavaMember> get() {
            return ImmutableSet.<JavaMember>builder()
                    .addAll(getAllFields())
                    .addAll(getAllMethods())
                    .addAll(getAllConstructors())
                    .build();
        }
    });

    JavaClassMembers(final JavaClass owner, Set<JavaField> fields, Set<JavaMethod> methods, Set<JavaConstructor> constructors, Optional<JavaStaticInitializer> staticInitializer) {
        this.owner = owner;
        this.fields = fields;
        this.methods = methods;
        this.constructors = constructors;
        this.staticInitializer = staticInitializer;
        this.codeUnits = ImmutableSet.<JavaCodeUnit>builder()
                .addAll(methods).addAll(constructors).addAll(staticInitializer.asSet())
                .build();
        this.members = ImmutableSet.<JavaMember>builder()
                .addAll(fields)
                .addAll(methods)
                .addAll(constructors)
                .build();
        allFields = Suppliers.memoize(new Supplier<Set<JavaField>>() {
            @Override
            public Set<JavaField> get() {
                ImmutableSet.Builder<JavaField> result = ImmutableSet.builder();
                for (JavaClass javaClass : concat(owner.getClassHierarchy(), owner.getAllRawInterfaces())) {
                    result.addAll(javaClass.getFields());
                }
                return result.build();
            }
        });
        allMethods = Suppliers.memoize(new Supplier<Set<JavaMethod>>() {
            @Override
            public Set<JavaMethod> get() {
                ImmutableSet.Builder<JavaMethod> result = ImmutableSet.builder();
                for (JavaClass javaClass : concat(owner.getClassHierarchy(), owner.getAllRawInterfaces())) {
                    result.addAll(javaClass.getMethods());
                }
                return result.build();
            }
        });
        allConstructors = Suppliers.memoize(new Supplier<Set<JavaConstructor>>() {
            @Override
            public Set<JavaConstructor> get() {
                ImmutableSet.Builder<JavaConstructor> result = ImmutableSet.builder();
                for (JavaClass javaClass : owner.getClassHierarchy()) {
                    result.addAll(javaClass.getConstructors());
                }
                return result.build();
            }
        });
    }

    Set<JavaMember> get() {
        return members;
    }

    Set<JavaMember> getAll() {
        return allMembers.get();
    }

    Set<JavaField> getFields() {
        return fields;
    }

    Set<JavaField> getAllFields() {
        return allFields.get();
    }

    public JavaField getField(String name) {
        Optional<JavaField> field = tryGetField(name);
        if (!field.isPresent()) {
            throw new IllegalArgumentException("No field with name '" + name + " in class " + owner.getName());
        }
        return field.get();
    }

    Optional<JavaField> tryGetField(String name) {
        for (JavaField field : fields) {
            if (name.equals(field.getName())) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }

    Set<JavaCodeUnit> getCodeUnits() {
        return codeUnits;
    }

    JavaCodeUnit getCodeUnitWithParameterTypeNames(String name, List<String> parameters) {
        return findMatchingCodeUnit(codeUnits, name, parameters);
    }

    Optional<JavaCodeUnit> tryGetCodeUnitWithParameterTypeNames(String name, List<String> parameters) {
        return tryFindMatchingCodeUnit(codeUnits, name, parameters);
    }

    JavaMethod getMethod(String name, List<String> parameterTypeNames) {
        return findMatchingCodeUnit(methods, name, ImmutableList.copyOf(parameterTypeNames));
    }

    Optional<JavaMethod> tryGetMethod(String name, List<String> parameterTypeNames) {
        return tryFindMatchingCodeUnit(methods, name, parameterTypeNames);
    }

    Set<JavaMethod> getMethods() {
        return methods;
    }

    Set<JavaMethod> getAllMethods() {
        return allMethods.get();
    }

    JavaConstructor getConstructor(List<String> parameterTypeNames) {
        return findMatchingCodeUnit(constructors, CONSTRUCTOR_NAME, parameterTypeNames);
    }

    Optional<JavaConstructor> tryGetConstructor(List<String> parameterTypeNames) {
        return tryFindMatchingCodeUnit(constructors, CONSTRUCTOR_NAME, parameterTypeNames);
    }

    Set<JavaConstructor> getConstructors() {
        return constructors;
    }

    Set<JavaConstructor> getAllConstructors() {
        return allConstructors.get();
    }

    Optional<JavaStaticInitializer> getStaticInitializer() {
        return staticInitializer;
    }

    Set<JavaEnumConstant> getEnumConstants() {
        ImmutableSet.Builder<JavaEnumConstant> result = ImmutableSet.builder();
        for (JavaField field : fields) {
            if (field.getModifiers().contains(ENUM)) {
                result.add(new JavaEnumConstant(owner, field.getName()));
            }
        }
        return result.build();
    }

    Set<ReferencedClassObject> getReferencedClassObjects() {
        ImmutableSet.Builder<ReferencedClassObject> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
            result.addAll(codeUnit.getReferencedClassObjects());
        }
        return result.build();
    }

    Set<JavaFieldAccess> getFieldAccessesFromSelf() {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
            result.addAll(codeUnit.getFieldAccesses());
        }
        return result.build();
    }

    Set<JavaMethodCall> getMethodCallsFromSelf() {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
            result.addAll(codeUnit.getMethodCallsFromSelf());
        }
        return result.build();
    }

    Set<JavaConstructorCall> getConstructorCallsFromSelf() {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
            result.addAll(codeUnit.getConstructorCallsFromSelf());
        }
        return result.build();
    }

    Set<JavaFieldAccess> getFieldAccessesToSelf() {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (JavaField field : fields) {
            result.addAll(field.getAccessesToSelf());
        }
        return result.build();
    }

    Set<JavaMethodCall> getMethodCallsToSelf() {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (JavaMethod method : methods) {
            result.addAll(method.getCallsOfSelf());
        }
        return result.build();
    }

    Set<JavaConstructorCall> getConstructorCallsToSelf() {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (JavaConstructor constructor : constructors) {
            result.addAll(constructor.getCallsOfSelf());
        }
        return result.build();
    }

    private <T extends JavaCodeUnit> T findMatchingCodeUnit(Set<T> codeUnits, String name, List<String> parameters) {
        Optional<T> codeUnit = tryFindMatchingCodeUnit(codeUnits, name, parameters);
        if (!codeUnit.isPresent()) {
            throw new IllegalArgumentException(
                    String.format("No code unit with name '%s' and parameters %s in codeUnits %s of class %s",
                            name, parameters, codeUnits, owner.getName()));
        }
        return codeUnit.get();
    }

    private <T extends JavaCodeUnit> Optional<T> tryFindMatchingCodeUnit(Set<T> codeUnits, String name, List<String> parameters) {
        Set<T> matching = findCodeUnitsWithMatchingNameAndParameters(codeUnits, name, parameters);

        if (matching.isEmpty()) {
            return Optional.empty();
        } else if (matching.size() == 1) {
            return Optional.of(getOnlyElement(matching));
        } else {
            // In this case we have some synthetic methods like bridge methods making name and parameters alone ambiguous
            // We want to return the non-synthetic method first because that is usually the relevant one for users
            SortedSet<T> sortedByPriority = new TreeSet<>(SORTED_BY_SYNTHETIC_LAST_THEN_FULL_NAME);
            sortedByPriority.addAll(matching);
            return Optional.of(sortedByPriority.first());
        }
    }

    private <T extends JavaCodeUnit> Set<T> findCodeUnitsWithMatchingNameAndParameters(Set<T> codeUnits, String name, List<String> parameters) {
        Set<T> matching = new HashSet<>();
        for (T codeUnit : codeUnits) {
            if (name.equals(codeUnit.getName()) && parameters.equals(namesOf(codeUnit.getRawParameterTypes()))) {
                matching.add(codeUnit);
            }
        }
        return matching;
    }

    void completeAnnotations(ImportContext context) {
        for (JavaMember member : members) {
            member.completeAnnotations(context);
        }
    }

    void completeAccessesFrom(ImportContext context) {
        for (JavaCodeUnit codeUnit : codeUnits) {
            codeUnit.completeAccessesFrom(context);
        }
    }

    void setReverseDependencies(ReverseDependencies reverseDependencies) {
        for (JavaMember member : members) {
            member.setReverseDependencies(reverseDependencies);
        }
    }

    private static final Comparator<JavaCodeUnit> SORTED_BY_SYNTHETIC_LAST_THEN_FULL_NAME = new Comparator<JavaCodeUnit>() {
        @Override
        public int compare(JavaCodeUnit codeUnit1, JavaCodeUnit codeUnit2) {
            return ComparisonChain.start()
                    .compareTrueFirst(!codeUnit1.getModifiers().contains(SYNTHETIC), !codeUnit2.getModifiers().contains(SYNTHETIC))
                    .compare(codeUnit1.getFullName(), codeUnit2.getFullName())
                    .result();
        }
    };

    static JavaClassMembers empty(JavaClass owner) {
        return new JavaClassMembers(
                owner,
                Collections.<JavaField>emptySet(),
                Collections.<JavaMethod>emptySet(),
                Collections.<JavaConstructor>emptySet(),
                Optional.<JavaStaticInitializer>empty());
    }

    static JavaClassMembers create(JavaClass owner, ImportContext context) {
        return new JavaClassMembers(
                owner,
                context.createFields(owner),
                context.createMethods(owner),
                context.createConstructors(owner),
                context.createStaticInitializer(owner));
    }
}
