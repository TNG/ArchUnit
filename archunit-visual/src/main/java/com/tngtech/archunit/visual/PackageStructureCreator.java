package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClass;

import java.util.HashSet;
import java.util.Set;

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
