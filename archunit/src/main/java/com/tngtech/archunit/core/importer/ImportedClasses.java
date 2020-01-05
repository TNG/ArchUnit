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
package com.tngtech.archunit.core.importer;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;

class ImportedClasses {
    private static final ImmutableSet<JavaModifier> PRIMITIVE_AND_ARRAY_TYPE_MODIFIERS =
            Sets.immutableEnumSet(PUBLIC, ABSTRACT, FINAL);

    private final ImmutableMap<String, JavaClass> directlyImported;
    private final Map<String, JavaClass> additionalClasses = new HashMap<>();
    private final ClassResolver resolver;

    ImportedClasses(Map<String, JavaClass> directlyImported, ClassResolver resolver) {
        this.directlyImported = ImmutableMap.copyOf(directlyImported);
        this.resolver = resolver;
    }

    Map<String, JavaClass> getDirectlyImported() {
        return directlyImported;
    }

    void add(JavaClass clazz) {
        additionalClasses.put(clazz.getName(), clazz);
    }

    JavaClass getOrResolve(String typeName) {
        ensurePresent(typeName);
        return directlyImported.containsKey(typeName) ?
                directlyImported.get(typeName) :
                additionalClasses.get(typeName);
    }

    void ensurePresent(String typeName) {
        if (!contain(typeName)) {
            Optional<JavaClass> resolved = resolver.tryResolve(typeName);
            JavaClass newClass = resolved.isPresent() ? resolved.get() : simpleClassOf(typeName);
            additionalClasses.put(typeName, newClass);
        }
    }

    private boolean contain(String name) {
        return directlyImported.containsKey(name) || additionalClasses.containsKey(name);
    }

    Map<String, JavaClass> getAll() {
        return ImmutableMap.<String, JavaClass>builder()
                .putAll(directlyImported)
                .putAll(additionalClasses)
                .build();
    }

    ClassesByTypeName byTypeName() {
        return new ClassesByTypeName() {
            @Override
            public JavaClass get(String typeName) {
                return ImportedClasses.this.getOrResolve(typeName);
            }
        };
    }

    private static JavaClass simpleClassOf(String typeName) {
        JavaType type = JavaType.From.name(typeName);
        DomainBuilders.JavaClassBuilder builder = new DomainBuilders.JavaClassBuilder().withType(type);
        addModifiersIfPossible(builder, type);
        return builder.build();
    }

    private static void addModifiersIfPossible(DomainBuilders.JavaClassBuilder builder, JavaType type) {
        if (type.isPrimitive() || type.isArray()) {
            builder.withModifiers(PRIMITIVE_AND_ARRAY_TYPE_MODIFIERS);
        }
    }

}
