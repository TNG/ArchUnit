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

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.AbstractClassesTransformer;
import com.tngtech.archunit.lang.ClassesTransformer;

class Transformers {
    static ClassesTransformer<JavaClass> classes() {
        return new ClassesIdentityTransformer();
    }

    static ClassesTransformer<JavaMember> members() {
        return new AbstractClassesTransformer<JavaMember>("members") {
            @Override
            public Iterable<JavaMember> doTransform(JavaClasses collection) {
                ImmutableSet.Builder<JavaMember> result = ImmutableSet.builder();
                for (JavaClass javaClass : collection) {
                    result.addAll(javaClass.getMembers());
                }
                return result.build();
            }
        };
    }

    static ClassesTransformer<JavaField> fields() {
        return new AbstractClassesTransformer<JavaField>("fields") {
            @Override
            public Iterable<JavaField> doTransform(JavaClasses collection) {
                ImmutableSet.Builder<JavaField> result = ImmutableSet.builder();
                for (JavaClass javaClass : collection) {
                    result.addAll(javaClass.getFields());
                }
                return result.build();
            }
        };
    }

    static ClassesTransformer<JavaCodeUnit> codeUnits() {
        return new AbstractClassesTransformer<JavaCodeUnit>("code units") {
            @Override
            public Iterable<JavaCodeUnit> doTransform(JavaClasses collection) {
                ImmutableSet.Builder<JavaCodeUnit> result = ImmutableSet.builder();
                for (JavaClass javaClass : collection) {
                    result.addAll(javaClass.getCodeUnits());
                }
                return result.build();
            }
        };
    }

    static ClassesTransformer<JavaConstructor> constructors() {
        return new AbstractClassesTransformer<JavaConstructor>("constructors") {
            @Override
            public Iterable<JavaConstructor> doTransform(JavaClasses collection) {
                ImmutableSet.Builder<JavaConstructor> result = ImmutableSet.builder();
                for (JavaClass javaClass : collection) {
                    result.addAll(javaClass.getConstructors());
                }
                return result.build();
            }
        };
    }

    static ClassesTransformer<JavaMethod> methods() {
        return new AbstractClassesTransformer<JavaMethod>("methods") {
            @Override
            public Iterable<JavaMethod> doTransform(JavaClasses collection) {
                ImmutableSet.Builder<JavaMethod> result = ImmutableSet.builder();
                for (JavaClass javaClass : collection) {
                    result.addAll(javaClass.getMethods());
                }
                return result.build();
            }
        };
    }
}
