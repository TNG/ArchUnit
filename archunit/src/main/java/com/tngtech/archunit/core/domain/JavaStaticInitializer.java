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
package com.tngtech.archunit.core.domain;

import java.lang.reflect.Member;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaStaticInitializerBuilder;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.emptySet;

/**
 * Represents the static initialization block of a class, e.g. a block like
 * <br><br>
 * <pre><code>
 * class Example {
 *     private static final String someStaticField;
 *
 *     static {
 *         // this is the static initializer, it can for example initialize static fields
 *         someStaticField = readSomeConfig();
 *     }
 * }
 * </code></pre>
 */
@PublicAPI(usage = ACCESS)
public final class JavaStaticInitializer extends JavaCodeUnit {
    @PublicAPI(usage = ACCESS)
    public static final String STATIC_INITIALIZER_NAME = "<clinit>";

    JavaStaticInitializer(JavaStaticInitializerBuilder builder) {
        super(builder);
    }

    @Override
    @SuppressWarnings("unchecked") // Cast is safe, because OWNER always refers to this object
    public List<JavaTypeVariable<JavaStaticInitializer>> getTypeParameters() {
        return (List<JavaTypeVariable<JavaStaticInitializer>>) super.getTypeParameters();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<? extends JavaAccess<?>> getAccessesToSelf() {
        return emptySet();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Member reflect() {
        throw new UnsupportedOperationException("Can't reflect on a static initializer");
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return "Static Initializer <" + getFullName() + ">";
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public ThrowsClause<JavaStaticInitializer> getThrowsClause() {
        return ThrowsClause.empty(this);
    }

    @Override
    public Set<? extends JavaCall<?>> getCallsOfSelf() {
        return emptySet();
    }
}
