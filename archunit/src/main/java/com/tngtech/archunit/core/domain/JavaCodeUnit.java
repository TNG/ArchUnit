/*
 * Copyright 2019 TNG Technology Consulting GmbH
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.domain.DomainObjectCreationContext.AccessContext;
import com.tngtech.archunit.core.domain.properties.HasParameterTypes;
import com.tngtech.archunit.core.domain.properties.HasReturnType;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaCodeUnitBuilder;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Formatters.formatMethod;

/**
 * Represents a unit of code containing accesses to other units of code. A unit of code can be
 * <ul>
 * <li>a method</li>
 * <li>a constructor</li>
 * <li>a static initializer</li>
 * </ul>
 * in particular every place, where Java code with behavior, like calling other methods or accessing fields, can
 * be defined.
 */
public abstract class JavaCodeUnit extends JavaMember implements HasParameterTypes, HasReturnType {
    private final JavaClass returnType;
    private final List<JavaClass> parameters;
    private final String fullName;

    private Set<JavaFieldAccess> fieldAccesses = Collections.emptySet();
    private Set<JavaMethodCall> methodCalls = Collections.emptySet();
    private Set<JavaConstructorCall> constructorCalls = Collections.emptySet();

    JavaCodeUnit(JavaCodeUnitBuilder<?, ?> builder) {
        super(builder);
        this.returnType = builder.getReturnType();
        this.parameters = builder.getParameters();
        fullName = formatMethod(getOwner().getName(), getName(), getParameters());
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public JavaClassList getParameters() {
        return new JavaClassList(parameters);
    }

    @Override
    public JavaClass getReturnType() {
        return returnType;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaFieldAccess> getFieldAccesses() {
        return fieldAccesses;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodCall> getMethodCallsFromSelf() {
        return methodCalls;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructorCall> getConstructorCallsFromSelf() {
        return constructorCalls;
    }

    @PublicAPI(usage = ACCESS)
    public boolean isConstructor() {
        return false;
    }

    AccessContext.Part completeFrom(ImportContext context) {
        fieldAccesses = context.getFieldAccessesFor(this);
        methodCalls = context.getMethodCallsFor(this);
        constructorCalls = context.getConstructorCallsFor(this);

        return new AccessContext.Part(this);
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "Just part of a bigger resolution process")
    static Class<?>[] reflect(JavaClassList parameters) {
        List<Class<?>> result = new ArrayList<>();
        for (JavaClass parameter : parameters) {
            result.add(parameter.reflect());
        }
        return result.toArray(new Class<?>[result.size()]);
    }

    public static final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaCodeUnit> constructor() {
            return new DescribedPredicate<JavaCodeUnit>("constructor") {
                @Override
                public boolean apply(JavaCodeUnit input) {
                    return input.isConstructor();
                }
            };
        }
    }

    public static final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaCodeUnit, JavaClass> GET_RETURN_TYPE =
                new ChainableFunction<JavaCodeUnit, JavaClass>() {
                    @Override
                    public JavaClass apply(JavaCodeUnit input) {
                        return input.getReturnType();
                    }
                };
    }

}
