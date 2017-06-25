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

import java.util.HashSet;
import java.util.Set;

import com.google.gson.annotations.Expose;

class JsonJavaPackage extends JsonElement {
    private static final String TYPE = "package";

    private boolean isDefault;

    @Expose
    private Set<JsonElement> children = new HashSet<>();

    private Set<JsonJavaPackage> subPackages = new HashSet<>();
    private Set<JsonJavaElement> classes = new HashSet<>();

    JsonJavaPackage(String name, String fullName) {
        this(name, fullName, false);
    }

    private JsonJavaPackage(String name, String fullName, boolean isDefault) {
        super(name, fullName, TYPE);
        this.isDefault = isDefault;
    }

    @Override
    Set<? extends JsonElement> getChildren() {
        return children;
    }


    void insertPackage(String pkg) {
        if (!fullName.equals(pkg)) {
            if (!insertPackageToCorrespondingChild(pkg)) {
                JsonJavaPackage newPkg = PackageStructureCreator.createPackage(fullName, isDefault, pkg);
                addPackage(newPkg);
                newPkg.insertPackage(pkg);
            }
        }
    }

    private void addPackage(JsonJavaPackage pkg) {
        subPackages.add(pkg);
        children.add(pkg);
    }

    private boolean insertPackageToCorrespondingChild(String pkg) {
        for (JsonJavaPackage c : subPackages) {
            if (pkg.startsWith(c.fullName)) {
                c.insertPackage(pkg);
                return true;
            }
        }
        return false;
    }

    void insertJavaElement(JsonJavaElement element) {
        if (fullName.equals(element.getPath())) {
            addJavaElement(element);
        } else {
            insertJavaElementToCorrespondingChild(element);
        }
    }

    private void addJavaElement(JsonJavaElement el) {
        classes.add(el);
        children.add(el);
    }

    private void insertJavaElementToCorrespondingChild(JsonJavaElement element) {
        for (JsonElement child : children) {
            if (element.fullName.startsWith(child.fullName)) {
                child.insertJavaElement(element);
                break;
            }
        }
    }

    void normalize() {
        if (subPackages.size() == 1 && classes.size() == 0) {
            mergeWithSubpackages();
            normalize();
        } else {
            normalizeSubpackages();
        }
    }

    private void mergeWithSubpackages() {
        // FIXME: For loop does not loop??
        for (JsonJavaPackage c : subPackages) {
            if (isDefault) {
                fullName = c.fullName;
                name = c.name;
                isDefault = false;
            } else {
                fullName = c.fullName;
                name += "." + c.name;
            }
            subPackages = c.subPackages;
            classes = c.classes;
            children = c.children;
            break;
        }
    }

    private void normalizeSubpackages() {
        for (JsonJavaPackage c : subPackages) {
            c.normalize();
        }
    }

    static JsonJavaPackage getDefaultPackage() {
        return new JsonJavaPackage(DEFAULT_ROOT, DEFAULT_ROOT, true);
    }
}
