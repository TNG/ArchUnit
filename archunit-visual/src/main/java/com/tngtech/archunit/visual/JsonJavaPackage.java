package com.tngtech.archunit.visual;

import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.Set;

class JsonJavaPackage extends JsonElement {
    private static String DEFAULTROOT = "default";

    private boolean isDefault;

    @Expose
    private Set<JsonElement> children = new HashSet<>();

    protected Set<JsonJavaPackage> subPackages = new HashSet<>();
    private Set<JsonJavaElement> classes = new HashSet<>();

    JsonJavaPackage(String name, String fullname, boolean isDefault) {
        super(name, fullname, "package");
        this.isDefault = isDefault;
    }

    private void addPackage(JsonJavaPackage pkg) {
        subPackages.add(pkg);
        children.add(pkg);
    }

    private void addJavaElement(JsonJavaElement el) {
        classes.add(el);
        children.add(el);
    }

    private static JsonJavaPackage createPackage(String pathParent, String newPath) {
        System.out.println(pathParent + "----" + newPath);
        int end = newPath.indexOf(".", pathParent.length() + 1);
        end = end == -1 ? newPath.length() : end;
        String fullName = newPath.substring(0, end);
        int start = pathParent.length() == 0 ? 0 : pathParent.length() + 1;
        String name = newPath.substring(start, end);
        return new JsonJavaPackage(name, fullName, false);
    }

    private void insertPackage(String pkg) {
        if (!fullname.equals(pkg)) {
            for (JsonJavaPackage c : subPackages) {
                if (pkg.startsWith(c.fullname)) {
                    c.insertPackage(pkg);
                    return;
                }
            }
            JsonJavaPackage newPkg = createPackage(fullname, pkg);
            addPackage(newPkg);
            newPkg.insertPackage(pkg);
        }
    }

    void insertJavaElement(JsonJavaElement el) {
        if (fullname.equals(el.getPath())) {
            addJavaElement(el);
        } else {
            for (JsonElement c : children) {
                if (el.fullname.startsWith(c.fullname)) {
                    c.insertJavaElement(el);
                }
            }
        }
    }

    void normalizeForExport() {
        if (subPackages.size() == 1 && classes.size() == 0) {
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
            normalizeForExport();
        } else {
            for (JsonJavaPackage c : subPackages) {
                c.normalizeForExport();
            }
        }
    }

    static JsonJavaPackage createTreeStructure(Set<String> pkgs) {
        JsonJavaPackage root = new JsonJavaPackage("", "", true);
        for (String p : pkgs) {
            root.insertPackage(p);
        }
        root.name = DEFAULTROOT;
        root.fullname = DEFAULTROOT;
        return root;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JsonJavaPackage that = (JsonJavaPackage) o;

        if (isDefault != that.isDefault) return false;
        if (children != null ? !children.equals(that.children) : that.children != null) return false;
        if (subPackages != null ? !subPackages.equals(that.subPackages) : that.subPackages != null) return false;
        return classes != null ? classes.equals(that.classes) : that.classes == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (isDefault ? 1 : 0);
        result = 31 * result + (children != null ? children.hashCode() : 0);
        result = 31 * result + (subPackages != null ? subPackages.hashCode() : 0);
        result = 31 * result + (classes != null ? classes.hashCode() : 0);
        return result;
    }
}
