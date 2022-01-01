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

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Strings.repeat;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class Formatters {
    private Formatters() {
    }

    private static String format(String ownerName, String methodName, String parameters) {
        return ownerName + "." + methodName + "(" + parameters + ")";
    }

    /**
     * @param ownerName  Class name where the method is declared (may be simple or fqn)
     * @param methodName Name of the method
     * @param parameters Names of parameter types (may be simple or fqn)
     * @return Arguments formatted as "simple(ownerName).methodName(simple(param1), simple(param2), ...)",
     * where simple(..) ensures the simple type name (compare {@link #ensureSimpleName(String)})
     */
    @PublicAPI(usage = ACCESS)
    public static String formatMethodSimple(String ownerName, String methodName, List<String> parameters) {
        List<String> simpleParams = new ArrayList<>();
        for (String parameter : parameters) {
            simpleParams.add(ensureSimpleName(parameter));
        }
        return formatMethod(ensureSimpleName(ownerName), methodName, simpleParams);
    }

    /**
     * @param ownerName  Class name where the method is declared
     * @param methodName Name of the method
     * @param parameters Names of parameter types
     * @return Arguments formatted (as passed) as "ownerName.methodName(param1, param2, ...)"
     */
    @PublicAPI(usage = ACCESS)
    public static String formatMethod(String ownerName, String methodName, List<String> parameters) {
        return format(ownerName, methodName, formatMethodParameterTypeNames(parameters));
    }

    /**
     * @param typeNames List of method parameter type names
     * @return Arguments formatted as "param1, param2, ..."
     */
    @PublicAPI(usage = ACCESS)
    public static String formatMethodParameterTypeNames(List<String> typeNames) {
        return Joiner.on(", ").join(typeNames);
    }

    /**
     * @param typeNames List of throws declaration type names
     * @return Arguments formatted as "param1, param2, ..."
     */
    @PublicAPI(usage = ACCESS)
    public static String formatThrowsDeclarationTypeNames(List<String> typeNames) {
        return Joiner.on(", ").join(typeNames);
    }

    /**
     * @see #formatNamesOf(Iterable)
     */
    @PublicAPI(usage = ACCESS)
    public static List<String> formatNamesOf(Class<?>... paramTypes) {
        return formatNamesOf(copyOf(paramTypes));
    }

    /**
     * @param paramTypes an iterable of {@link Class} objects
     * @return A {@link List} of fully qualified class names of the passed {@link Class} objects
     */
    @PublicAPI(usage = ACCESS)
    public static List<String> formatNamesOf(Iterable<Class<?>> paramTypes) {
        ImmutableList.Builder<String> result = ImmutableList.builder();
        for (Class<?> paramType : paramTypes) {
            result.add(paramType.getName());
        }
        return result.build();
    }

    // Excluding the '$' character might be incorrect, but since '$' is a valid character of a class name
    // and also the delimiter within the fully qualified name between an inner class and its enclosing class,
    // there is no clean way to derive the simple name from just a fully qualified class name without
    // further information
    // Luckily for imported classes we can read this information from the bytecode
    /**
     * @param name A possibly fully qualified class name
     * @return A best guess of the simple name, i.e. prefixes like 'a.b.c.' cut off, 'Some$' of 'Some$Inner' as well.
     * Returns an empty String, if the name belongs to an anonymous class (e.g. some.Type$1).
     */
    @PublicAPI(usage = ACCESS)
    public static String ensureSimpleName(String name) {
        int lastIndexOfDot = name.lastIndexOf('.');
        String partAfterDot = lastIndexOfDot >= 0 ? name.substring(lastIndexOfDot + 1) : name;

        int lastIndexOf$ = partAfterDot.lastIndexOf('$');
        String simpleNameCandidate = lastIndexOf$ >= 0 ? partAfterDot.substring(lastIndexOf$ + 1) : partAfterDot;

        for (int i = 0; i < simpleNameCandidate.length(); i++) {
            if (Character.isJavaIdentifierStart(simpleNameCandidate.charAt(i))) {
                return simpleNameCandidate.substring(i);
            }
        }
        return "";
    }

    /**
     * Returns the canonical array type name of any array type name passed in. Otherwise
     * returns the passed type name as is. For example {@code [Ljava.lang.String;} will
     * be reformatted to {@code java.lang.String[]} or {@code [I} will be reformatted
     * to {@code int[]}, while {@code java.lang.String} would simply be returned as is.
     * @param typeName A Java type name
     * @return the passed type name, but for array type names the canonical array type name
     */
    @PublicAPI(usage = ACCESS)
    public static String ensureCanonicalArrayTypeName(String typeName) {
        if (isNoArrayClassName(typeName)) {
            return typeName;
        }

        JavaClassDescriptor descriptor = JavaClassDescriptor.From.name(typeName);
        int dimensions = 0;
        while (descriptor.tryGetComponentType().isPresent()) {
            descriptor = descriptor.tryGetComponentType().get();
            dimensions++;
        }
        return descriptor.getFullyQualifiedClassName() + repeat("[]", dimensions);
    }

    // we only consider (non-canonical) array class names. Those all have the form `[xxx`
    // where xxx is a type name. E.g. String[].class.getName() -> `[Ljava.lang.String;
    private static boolean isNoArrayClassName(String typeName) {
        return !typeName.startsWith("[");
    }
}
