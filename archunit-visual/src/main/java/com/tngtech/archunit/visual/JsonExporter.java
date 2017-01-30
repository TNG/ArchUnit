package com.tngtech.archunit.visual;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.JavaCodeUnit;
import com.tngtech.archunit.core.JavaConstructorCall;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaMethodCall;

public class JsonExporter {

    public static void main(String[] args) {

    }

    /**
     * export the given Java-classes to a JSON-file, ignoring all dependencies to classes not being in the basePath
     *
     * @param classes
     * @param file
     * @param basePath
     */
    public void export(JavaClasses classes, File file, String basePath) {
        final List<JsonJavaElement> elements = new ArrayList<>();
        parseJavaClasses(classesToList(classes), "", new Consumer<JsonJavaElement>() {
            @Override
            public void accept(JsonJavaElement j) {
                elements.add(j);
            }
        }, basePath);
        JsonJavaElement root = getOrCreateRoot(elements, "root");
        Gson gson = new Gson();
        try {
            String jsonString = gson.toJson(root);
            gson.toJson(root, new FileWriter(file));
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(jsonString);
            myOutWriter.close();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JsonJavaElement getOrCreateRoot(List<JsonJavaElement> list, String newRootName) {
        if (list.size() == 0) return null;
        if (list.size() == 1) {
            return list.get(0);
        } else {
            JsonJavaElement root = new JsonJavaPackage(newRootName, newRootName);
            for (JsonJavaElement e : list) {
                root.addChild(e);
            }
            return root;
        }
    }

    private List<JavaClass> classesToList(JavaClasses classes) {
        return ImmutableList.copyOf(classes);
    }

    private String expandPath(String partialPath, String fullPath, String sep) {
        return fullPath.indexOf(sep, partialPath.length() + 1) == -1 ?
                fullPath : fullPath.substring(0, fullPath.indexOf(sep, partialPath.length() + 1));
    }

    private void parseJavaClasses(List<JavaClass> classes, String parent, Consumer<JsonJavaElement> insert, String basePath) {
        String path = parent;
        String commonPath = parent;
        if (classes.isEmpty()) return;
        JavaClass c = classes.get(0);
        boolean fullPathNotReached = true;
        while (eachStartsWith(classes, path) && (fullPathNotReached = path.length() < c.getPackage().length())) {
            commonPath = path;
            path = expandPath(path, c.getPackage(), ".");
        }
        if (!fullPathNotReached) {
            commonPath = path;
        }
        if (!commonPath.equals(parent)) {
            int start = parent.isEmpty() ? 0 : parent.length() + 1;
            final JsonJavaPackage jpkg = new JsonJavaPackage(commonPath.substring(start), commonPath);
            insert.accept(jpkg);
            parseJavaClasses(classes, commonPath, new Consumer<JsonJavaElement>() {
                @Override
                public void accept(JsonJavaElement j) {
                    jpkg.addChild(j);
                }
            }, basePath);
        } else {
            if (fullPathNotReached) {
                SeparatedClasses s = SeparatedClasses.separate(classes, path);
                parseJavaClasses(s.matching, parent, insert, basePath);
                parseJavaClasses(s.notMatching, parent, insert, basePath);
            } else {
                insert.accept(parseJavaFile(c, basePath));
                parseJavaClasses(classes.subList(1, classes.size()), parent, insert, basePath);
            }
        }

    }

    private boolean eachStartsWith(Iterable<JavaClass> classes, String prefix) {
        for (JavaClass c : classes) {
            if (!c.getPackage().startsWith(prefix)) return false;
        }
        return true;
    }

    private JsonJavaFile parseJavaFile(JavaClass c, String basePath) {
        if (c.isInterface()) {
            return parseJavaInterface(c, basePath);
        } else {
            return parseJavaClass(c, basePath);
        }
    }

    private String getSuperClass(JavaClass c, String basePath) {
        return c.getSuperClass().isPresent() && c.getSuperClass().get().getName().startsWith(basePath) ?
                c.getSuperClass().get().getName() : "";
    }

    private JsonJavaFile parseJavaClass(final JavaClass c, String basePath) {
        final JsonJavaClazz res = new JsonJavaClazz(c.getSimpleName(), c.getName(), "class", getSuperClass(c, basePath));
        parseJavaElementsToJavaFile(c, basePath, res);
        return res;
    }

    private JsonJavaFile parseJavaInterface(JavaClass c, String basePath) {
        JsonJavaInterface res = new JsonJavaInterface(c.getSimpleName(), c.getName());
        parseJavaElementsToJavaFile(c, basePath, res);
        return res;
    }

    private String getStartCodeUnit(JavaCodeUnit origin, JavaClass c) {
        return origin.isConstructor() ? c.getSimpleName() : origin.getName();
    }

    private boolean isRelevantDep(String targetOwner, String origOwner, String basePath) {
        return targetOwner.startsWith(basePath) && !targetOwner.equals(origOwner);
    }

    private void parseJavaElementsToJavaFile(JavaClass c, String basePath, JsonJavaFile res) {
        if (c.getEnclosingClass().isPresent()) {
            res.addChild(parseJavaClass(c.getEnclosingClass().get(), basePath));
        }
        for (JavaClass i : c.getAllInterfaces()) {
            if (i.getName().startsWith(basePath)) {
                res.addInterface(i.getName());
            }
        }
        for (JavaFieldAccess fa : c.getFieldAccessesFromSelf()) {
            if (isRelevantDep(fa.getTargetOwner().getName(), c.getName(), basePath)) {
                res.addFieldAccess(new JsonFieldAccess(fa.getTargetOwner().getName(), getStartCodeUnit(fa.getOrigin(), c),
                        fa.getTarget().getName()));
            }
        }
        for (JavaMethodCall mc : c.getMethodCallsFromSelf()) {
            if (isRelevantDep(mc.getTargetOwner().getName(), c.getName(), basePath)) {
                res.addMethodCall(new JsonMethodCall(mc.getTargetOwner().getName(), getStartCodeUnit(mc.getOrigin(), c),
                        mc.getTarget().getName()));
            }
        }
        for (JavaConstructorCall cc : c.getConstructorCallsFromSelf()) {
            if (isRelevantDep(cc.getTargetOwner().getName(), c.getName(), basePath)) {
                res.addConstructorCall(new JsonConstructorCall(cc.getTargetOwner().getName(),
                        getStartCodeUnit(cc.getOrigin(), c), cc.getTargetOwner().getSimpleName()));
            }
        }
    }

    static class SeparatedClasses {
        private List<JavaClass> matching = new ArrayList<>();
        private List<JavaClass> notMatching = new ArrayList<>();

        public static SeparatedClasses separate(Iterable<JavaClass> classes, String prefix) {
            SeparatedClasses res = new SeparatedClasses();
            for (JavaClass c : classes) {
                if (c.getPackage().startsWith(prefix)) {
                    res.matching.add(c);
                } else {
                    res.notMatching.add(c);
                }
            }
            return res;
        }
    }

}
