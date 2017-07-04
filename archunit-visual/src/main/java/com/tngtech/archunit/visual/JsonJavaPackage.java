/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.visual;

import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.Set;

class JsonJavaPackage extends JsonElement {
    static final String PACKAGE_SEPARATOR = ".";
    private static final String TYPE = "package";

    private boolean isDefault;

    @Expose
    private Set<JsonElement> children = new HashSet<>();

    private Set<JsonJavaPackage> subPackages = new HashSet<>();
    private Set<JsonJavaElement> classes = new HashSet<>();

    JsonJavaPackage(String name, String fullName) {
        super(name, fullName, TYPE);
    }

    private static JsonJavaPackage createDefaultPackage() {
        JsonJavaPackage defaultPackage = new JsonJavaPackage(DEFAULT_ROOT, DEFAULT_ROOT);
        defaultPackage.isDefault = true;
        return defaultPackage;
    }

    static JsonJavaPackage createPackageStructure(Set<String> packages) {
        JsonJavaPackage root = createDefaultPackage();
        for (String p : packages) {
            root.insertPackage(p);
        }
        return root;
    }

    @Override
    Set<? extends JsonElement> getChildren() {
        return children;
    }

    void insertPackage(String pkg) {
        if (!fullName.equals(pkg) && !tryToInsertToExistingPackage(pkg)) {
            JsonJavaPackage newPkg = createPackage(pkg, fullName, isDefault);
            subPackages.add(newPkg);
            children.add(newPkg);
            newPkg.insertPackage(pkg);
        }
    }

    private boolean tryToInsertToExistingPackage(String pkg) {
        for (JsonJavaPackage c : subPackages) {
            if (pkg.startsWith(c.fullName)) {
                c.insertPackage(pkg);
                return true;
            }
        }
        return false;
    }

    /**
     * creates a JsonJavaPackage one level under this parent using the next sub-package in newFullName
     */
    private static JsonJavaPackage createPackage(String newFullName, String parentFullName, boolean parentIsDefault) {
        int length = parentIsDefault ? 0 : parentFullName.length() + 1;
        int end = newFullName.indexOf(PACKAGE_SEPARATOR, length);
        end = end == -1 ? newFullName.length() : end;
        String fullName = newFullName.substring(0, end);
        int start = parentIsDefault || parentFullName.length() == 0 ? 0 : parentFullName.length() + 1;
        String name = newFullName.substring(start, end);
        return new JsonJavaPackage(name, fullName);
    }

    void insert(JsonJavaElement element) {
        if (fullName.equals(element.getPath())) {
            classes.add(element);
            children.add(element);
        } else {
            insertToSubPackage(element);
        }
    }

    private void insertToSubPackage(JsonJavaElement jsonJavaElement) {
        for (JsonElement child : children) {
            if (jsonJavaElement.fullName.startsWith(child.fullName)) {
                child.insert(jsonJavaElement);
                break;
            }
        }
    }

    void normalize() {
        if (subPackages.size() == 1 && classes.size() == 0) {
            mergeWithSubpackage();
            normalize();
        } else {
            for (JsonJavaPackage c : subPackages) {
                c.normalize();
            }
        }
    }

    private void mergeWithSubpackage() {
        JsonJavaPackage newRoot = subPackages.iterator().next();
        if (isDefault) {
            fullName = newRoot.fullName;
            name = newRoot.name;
            isDefault = false;
        } else {
            fullName = newRoot.fullName;
            name += "." + newRoot.name;
        }
        subPackages = newRoot.subPackages;
        classes = newRoot.classes;
        children = newRoot.children;
    }
}
