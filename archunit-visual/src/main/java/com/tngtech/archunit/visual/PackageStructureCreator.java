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

import com.tngtech.archunit.core.domain.JavaClass;

class PackageStructureCreator {
    static final String PACKAGE_SEPARATOR = ".";

    static JsonJavaPackage createPackageStructure(Iterable<JavaClass> classes) {
        return createPackageStructure(collectPackages(classes), JsonJavaPackage.getDefaultPackage());
    }

    private static JsonJavaPackage createPackageStructure(Set<String> pkgs, JsonJavaPackage root) {
        for (String p : pkgs) {
            root.insertPackage(p);
        }
        return root;
    }

    /**
     * creates a JsonJavaPackage one level under this parent using the next sub-package in newFullname
     */
    static JsonJavaPackage createPackage(String parentFullname, boolean parentIsDeafult, String newFullname) {
        int length = parentIsDeafult ? 0 : parentFullname.length() + 1;
        int end = newFullname.indexOf(PACKAGE_SEPARATOR, length);
        end = end == -1 ? newFullname.length() : end;
        String fullName = newFullname.substring(0, end);
        int start = parentIsDeafult || parentFullname.length() == 0 ? 0 : parentFullname.length() + 1;
        String name = newFullname.substring(start, end);
        return new JsonJavaPackage(name, fullName);
    }

    private static Set<String> collectPackages(Iterable<JavaClass> classes) {
        Set<String> pkgs = new HashSet<>();
        for (JavaClass c : classes) {
            pkgs.add(c.getPackage());
        }
        return pkgs;
    }
}
