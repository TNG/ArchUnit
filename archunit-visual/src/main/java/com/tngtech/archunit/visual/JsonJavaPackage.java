package com.tngtech.archunit.visual;

import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.Set;

class JsonJavaPackage extends JsonElement {
    private boolean isDefault;

    @Expose
    private Set<JsonElement> children = new HashSet<>();

    protected Set<JsonJavaPackage> subPackages = new HashSet<>();
    private Set<JsonJavaElement> classes = new HashSet<>();

    JsonJavaPackage(String name, String fullname, boolean isDefault) {
        super(name, fullname, "package");
        this.isDefault = isDefault;
    }

    @Override
    Set<? extends JsonElement> getChildren() {
        return children;
    }


    void insertPackage(String pkg) {
        if (!fullname.equals(pkg)) {
            if (!insertPackageToCorrespondingChild(pkg)) {
                JsonJavaPackage newPkg = PackageStructureCreator.createPackage(fullname, isDefault, pkg);
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
            if (pkg.startsWith(c.fullname)) {
                c.insertPackage(pkg);
                return true;
            }
        }
        return false;
    }

    void insertJavaElement(JsonJavaElement element) {
        if (fullname.equals(element.getPath())) {
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
            if (element.fullname.startsWith(child.fullname)) {
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
        for (JsonJavaPackage c : subPackages) {
            if (isDefault) {
                fullname = c.fullname;
                name = c.name;
                isDefault = false;
            } else {
                fullname = c.fullname;
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
}
