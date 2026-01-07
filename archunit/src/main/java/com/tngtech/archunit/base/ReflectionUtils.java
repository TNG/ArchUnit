/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.base;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import com.tngtech.archunit.Internal;

@Internal
public final class ReflectionUtils {
    private ReflectionUtils() {
    }

    public static <T> T newInstanceOf(Class<T> type, Object... parameters) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor(typesOf(parameters));
            constructor.setAccessible(true);
            return constructor.newInstance(parameters);
        } catch (Exception e) {
            throw new ArchUnitException.ReflectionException(e);
        }
    }

    private static Class<?>[] typesOf(Object[] parameters) {
        return Arrays.stream(parameters).map(Object::getClass).toArray(Class<?>[]::new);
    }
}
