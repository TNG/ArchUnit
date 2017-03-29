package com.tngtech.archunit.visual;

import java.util.Set;

public class PackageStructureCreator {
    private static String DEFAULTROOT = "default";

    static JsonJavaPackage createPackageStructure(Set<String> pkgs) {
        JsonJavaPackage root = new JsonJavaPackage(DEFAULTROOT, DEFAULTROOT, true);
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
        int end = newFullname.indexOf(".", length);
        end = end == -1 ? newFullname.length() : end;
        String fullName = newFullname.substring(0, end);
        int start = parentIsDeafult || parentFullname.length() == 0 ? 0 : parentFullname.length() + 1;
        String name = newFullname.substring(start, end);
        return new JsonJavaPackage(name, fullName, false);
    }
}
