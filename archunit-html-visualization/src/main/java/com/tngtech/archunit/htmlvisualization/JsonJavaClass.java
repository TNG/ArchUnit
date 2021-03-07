/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.htmlvisualization;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.tngtech.archunit.core.domain.JavaClass;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

class JsonJavaClass implements ArchJsonElement {
    static final String CLASS_TYPE = "class";
    static final String INTERFACE_TYPE = "interface";

    // An anonymous class name always has the pattern some.fully.qualified.Name$MaybeInner$x where x is some integer
    private static final Pattern ANONYMOUS_CLASS_NAME_INDEX_PATTERN = Pattern.compile(".*\\$(\\d+)$");

    private final Map<String, JsonJavaClass> nestedClasses = new TreeMap<>();
    private final String simpleName;
    private final String fullName;
    private final String type;

    JsonJavaClass(String simpleName, String fullName, String type) {
        this.simpleName = simpleName;
        this.fullName = fullName;
        this.type = type;
    }

    @Override
    public JsonSerializable toJsonSerializable() {
        Set<JsonSerializable> childSerializables = new HashSet<>();
        for (JsonJavaClass child : nestedClasses.values()) {
            childSerializables.add(child.toJsonSerializable());
        }
        return new JsonSerializable(
                simpleName,
                fullName,
                type,
                childSerializables);
    }

    private static String serializeSimpleName(JavaClass javaClass) {
        return javaClass.isAnonymousClass()
                ? ANONYMOUS_CLASS_NAME_INDEX_PATTERN.matcher(javaClass.getFullName()).replaceAll("<<Anonymous[$1]>>")
                : javaClass.getSimpleName();
    }

    void addClass(List<JavaClass> nestedClassPath, JavaClass javaClass) {
        if (nestedClassPath.isEmpty()) {
            nestedClasses.put(javaClass.getName(), of(javaClass));
        } else {
            LinkedList<JavaClass> nestedClassPathRest = new LinkedList<>(nestedClassPath);
            JavaClass nextEnclosingClass = requireNonNull(nestedClassPathRest.pollFirst());
            nestedClasses.get(nextEnclosingClass.getName()).addClass(nestedClassPathRest, javaClass);
        }
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("simpleName", simpleName)
                .add("fullyQualifiedName", fullName)
                .add("type", type)
                .add("nestedClasses", nestedClasses.keySet())
                .toString();
    }

    static JsonJavaClass of(JavaClass javaClass) {
        checkArgument(!javaClass.isArray(), "Array types are not supported to be serialized");
        return new JsonJavaClass(serializeSimpleName(javaClass), javaClass.getName(), javaClass.isInterface() ? INTERFACE_TYPE : CLASS_TYPE);
    }
}
