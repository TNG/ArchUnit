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
package com.tngtech.archunit.base;

import com.tngtech.archunit.Internal;

@Internal
public class ClassLoaders {
    public static ClassLoader getCurrentClassLoader(Class<?> clazz) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader != null ? contextClassLoader : clazz.getClassLoader();
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "This is just an utility method. Callers will be checked recursively for @MayResolveTypesViaReflection")
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        try {
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            //fall through
        }
        return Class.forName(className);
    }
}
