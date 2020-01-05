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
package com.tngtech.archunit.core.domain;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class Formatters {
    private Formatters() {
    }

    /**
     * @param ownerName  Class name where the method is declared
     * @param methodName Name of the method
     * @param parameters Parameters of the method
     * @return Arguments formatted as "ownerName.methodName(fqn.param1, fqn.param2, ...)"
     */
    @PublicAPI(usage = ACCESS)
    public static String formatMethod(String ownerName, String methodName, JavaClassList parameters) {
        return format(ownerName, methodName, formatMethodParameters(parameters));
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

    private static String formatMethodParameters(List<? extends HasName> parameters) {
        List<String> names = new ArrayList<>();
        for (HasName type : parameters) {
            names.add(type.getName());
        }
        return formatMethodParameterTypeNames(names);
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
     * @param clazz      Class determining the location
     * @param lineNumber Line number of the location
     * @return Arguments formatted as "(${clazz.getSimpleName()}.java:${lineNumber})". This format is (at least
     * by IntelliJ Idea) recognized as location, if it's the end of a failure line, thus enabling IDE support
     * to jump to a violation.
     * @deprecated use {@link SourceCodeLocation}
     * @see SourceCodeLocation
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public static String formatLocation(JavaClass clazz, int lineNumber) {
        return SourceCodeLocation.of(clazz, lineNumber).toString();
    }
}
