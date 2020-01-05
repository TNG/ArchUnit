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
package com.tngtech.archunit.junit;

import java.lang.reflect.Field;
import java.util.Objects;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import org.junit.platform.engine.DiscoverySelector;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class FieldSelector implements DiscoverySelector {
    private final Class<?> clazz;
    private final Field field;

    private FieldSelector(Class<?> clazz, Field field) {
        this.clazz = clazz;
        this.field = field;
    }

    Class<?> getJavaClass() {
        return clazz;
    }

    Field getJavaField() {
        return field;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, field);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final FieldSelector other = (FieldSelector) obj;
        return Objects.equals(this.clazz, other.clazz)
                && Objects.equals(this.field, other.field);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "clazz=" + clazz +
                ", field=" + field +
                '}';
    }

    @PublicAPI(usage = ACCESS)
    public static FieldSelector selectField(String className, String fieldName) {
        return selectField(classForName(className), fieldName);
    }

    @MayResolveTypesViaReflection(reason = "Within the ArchUnitTestEngine we may resolve types via reflection, since they are needed anyway")
    private static Class<?> classForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not load requested class " + className, e);
        }
    }

    @PublicAPI(usage = ACCESS)
    public static FieldSelector selectField(Class<?> javaClass, String fieldName) {
        Field selectedField = findFieldInHierarchy(javaClass, fieldName);
        if (selectedField == null) {
            throw new IllegalArgumentException(String.format("Could not find field %s.%s", javaClass.getName(), fieldName));
        }
        return selectField(javaClass, selectedField);
    }

    private static Field findFieldInHierarchy(Class<?> javaClass, String fieldName) {
        Field selectedField = null;
        Class<?> currentClass = javaClass;
        while (selectedField == null && !currentClass.equals(Object.class)) {
            try {
                selectedField = currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return selectedField;
    }

    @PublicAPI(usage = ACCESS)
    public static FieldSelector selectField(Class<?> javaClass, Field field) {
        return new FieldSelector(javaClass, field);
    }
}
