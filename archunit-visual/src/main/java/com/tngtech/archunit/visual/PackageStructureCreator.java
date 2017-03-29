package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;

import java.util.HashSet;
import java.util.Set;

public class PackageStructureCreator {
    static final String PACKAGESEP = ".";
    private static final String DEFAULTROOT = "default";

    static JsonJavaPackage createPackageStructure(JavaClasses classes, VisualizationContext context) {
        return createPackageStructure(collectPackages(classes, context));
    }

    private static JsonJavaPackage createPackageStructure(Set<String> pkgs) {
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
        int end = newFullname.indexOf(PACKAGESEP, length);
        end = end == -1 ? newFullname.length() : end;
        String fullName = newFullname.substring(0, end);
        int start = parentIsDeafult || parentFullname.length() == 0 ? 0 : parentFullname.length() + 1;
        String name = newFullname.substring(start, end);
        return new JsonJavaPackage(name, fullName, false);
    }

    private static Set<String> collectPackages(JavaClasses classes, VisualizationContext context) {
        Set<String> pkgs = new HashSet<>();
        for (JavaClass c : classes) {
            if (context.isElementIncluded(c.getName())) {
                pkgs.add(c.getPackage());
            }
        }
        return pkgs;
    }
}
