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
import java.util.List;

import com.google.common.base.Joiner;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class Formatters {
    private static final String LOCATION_TEMPLATE = "(%s:%d)";

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

    /**
     * @param name A possibly fully qualified class name
     * @return A best guess of the simple name, i.e. prefixes like 'a.b.c.' cut off, 'Some$' of 'Some$Inner' as well.
     * Returns an empty String, if the name belongs to an anonymous class (e.g. some.Type$1).
     */
    @PublicAPI(usage = ACCESS)
    public static String ensureSimpleName(String name) {
        int mostNestedClassStart = name.lastIndexOf('$');
        if (mostNestedClassStart > -1) {
            String lastPart = name.substring(mostNestedClassStart + 1);
            // lastPart contains "1" for anonymous classes, "NestedClass" for nested classes, and "1LocalClass" for local classes

            int javaIdentifierStart = indexOfJavaIdentifierStart(lastPart);
            if (javaIdentifierStart > -1) {
                return lastPart.substring(javaIdentifierStart);
            } else {
                // name is an anonymous class
                // return empty string, because java.lang.Class.getSimpleName() also returns empty string
                return "";
            }
        }

        int classStart = name.lastIndexOf('.');
        if (classStart > -1) {
            return name.substring(classStart + 1);
        }

        // name may be a class in default package or may be empty
        return name;
    }

    /**
     * Returns the index of the first character, that is permissible as the first character in a Java identifier.
     * Returns -1 if there is no such character.
     *
     * See also JLS 3.8
     */
    private static int indexOfJavaIdentifierStart(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isJavaIdentifierStart(s.charAt(i))) {
                return i;
            }
        }
        return -1;
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
