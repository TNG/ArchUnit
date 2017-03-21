package visualOld;

import java.util.Set;

class JsonJavaPackage extends JsonJavaElement {
    private static String DEFAULTROOT = "default";

    JsonJavaPackage(String name, String fullname) {
        super(name, fullname, "package");
    }

    void insertPackage(String pkg) {
        for (JsonJavaElement c : children) {

        }
    }

    void insertClass(JsonJavaFile clazz) {

    }

    public static JsonJavaPackage createTreeStructure(Set<String> pkgs) {
        JsonJavaPackage root = new JsonJavaPackage(DEFAULTROOT, DEFAULTROOT);
        for (String p : pkgs) {
            root.insertPackage(p);
        }
        if (root.children.size() == 1) {
            for (JsonJavaElement e : root.children) {

            }
        }
        return null;
    }
}
