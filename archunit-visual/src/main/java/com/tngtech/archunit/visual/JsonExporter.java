package com.tngtech.archunit.visual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tngtech.archunit.core.*;

import java.io.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class JsonExporter {

    /**
     * export the given Java-classes to a JSON-file, ignoring all dependencies to classes not being in the basePath
     */
    public void export(JavaClasses classes, File file, VisualizationContext context) {
        JsonJavaPackage root = createStructure(classes, context);
        writeToFile(file, root);
    }

    private JsonJavaPackage createStructure(JavaClasses classes, VisualizationContext context) {
        Set<String> pkgs = collectPackages(classes, context);
        JsonJavaPackage root = JsonJavaPackage.createTreeStructure(pkgs);
        List<JavaClass> innerClasses = collectInnerClasses(classes, context, root);
        handleInnerClasses(context, root, innerClasses);
        root.normalizeForExport();
        return root;
    }

    private void handleInnerClasses(VisualizationContext context, JsonJavaPackage root, List<JavaClass> innerClasses) {
        for (JavaClass c : innerClasses) {
            if (c.getSimpleName().isEmpty()) {
                addDependenciesOfAnonymousInnerClassToParent(context, root, c);
            } else {
                root.insertJavaElement(parseJavaElement(c, context));
            }
        }
    }

    private List<JavaClass> collectInnerClasses(JavaClasses classes, VisualizationContext context, JsonJavaPackage root) {
        List<JavaClass> innerClasses = new LinkedList<>();
        for (JavaClass c : classes) {
            if (context.isElementIncluded(c.getName())) {
                if (c.getName().contains("$")) {
                    innerClasses.add(c);
                } else if (!c.getSimpleName().isEmpty()) {
                    root.insertJavaElement(parseJavaElement(c, context));
                }
            }
        }
        return innerClasses;
    }

    private Set<String> collectPackages(JavaClasses classes, VisualizationContext context) {
        Set<String> pkgs = new HashSet<>();
        for (JavaClass c : classes) {
            if (context.isElementIncluded(c.getName())) {
                pkgs.add(c.getPackage());
            }
        }
        return pkgs;
    }

    private void writeToFile(File file, JsonJavaPackage root) {
        final GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();
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


    private void addDependenciesOfAnonymousInnerClassToParent(VisualizationContext context, JsonJavaPackage root, JavaClass c) {
        Optional<? extends JsonElement> parent = root.getChild(getFullnameOfParentClass(c.getName()));
        if (parent.isPresent() && parent.get() instanceof JsonJavaElement) {
            JsonJavaElement el = (JsonJavaElement) parent.get();
            parseAnonymousImplemenationToJavaElement(c, context, el);
            parseAccessesToJavaElement(c, context, el);
        }
    }

    private String getFullnameOfParentClass(String innerClass) {
        return innerClass.substring(0, innerClass.indexOf("$"));
    }

    private JsonJavaElement parseJavaElement(JavaClass c, VisualizationContext context) {
        if (c.isInterface()) {
            return parseJavaInterface(c, context);
        } else {
            return parseJavaClass(c, context);
        }
    }

    private String getSuperClass(JavaClass c, VisualizationContext context) {
        return c.getSuperClass().isPresent() && context.isElementIncluded(c.getSuperClass().get().getName()) ?
                c.getSuperClass().get().getName() : "";
    }

    private String getCleanedFullname(String fullname) {
        return fullname.replaceAll("§", ".");
    }

    private JsonJavaElement parseJavaClass(final JavaClass c, VisualizationContext context) {
        final JsonJavaClazz res = new JsonJavaClazz(c.getSimpleName(), getCleanedFullname(c.getName()),
                "class", getSuperClass(c, context));
        parseToJavaElement(c, context, res);
        return res;
    }

    private JsonJavaElement parseJavaInterface(JavaClass c, VisualizationContext context) {
        JsonJavaInterface res = new JsonJavaInterface(c.getSimpleName(), getCleanedFullname(c.getName()));
        parseToJavaElement(c, context, res);
        return res;
    }

    private String getStartCodeUnit(JavaCodeUnit origin, JavaClass c) {
        return origin.isConstructor() ? c.getSimpleName() : origin.getName();
    }

    private void parseToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        parseImplementationToJavaElement(c, context, res);
        parseAccessesToJavaElement(c, context, res);
    }

    private void parseImplementationToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        for (JavaClass i : c.getAllInterfaces()) {
            if (context.isElementIncluded(i.getName())) {
                res.addInterface(i.getName());
            }
        }
    }

    private void parseAnonymousImplemenationToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        for (JavaClass i : c.getAllInterfaces()) {
            if (context.isElementIncluded(i.getName())) {
                res.addAnonImpl(i.getName());
            }
        }
    }

    private void parseAccessesToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        for (JavaFieldAccess fa : c.getFieldAccessesFromSelf()) {
            if (!fa.getTargetOwner().getSimpleName().isEmpty() && context.isDependencyIncluded(res, fa.getTargetOwner().getName(), false)) {
                res.addFieldAccess(new JsonFieldAccess(fa.getTargetOwner().getName(), getStartCodeUnit(fa.getOrigin(), c),
                        fa.getTarget().getName()));
            }
        }
        for (JavaMethodCall mc : c.getMethodCallsFromSelf()) {
            if (!mc.getTargetOwner().getSimpleName().isEmpty() && context.isDependencyIncluded(res, mc.getTargetOwner().getName(), false)) {
                res.addMethodCall(new JsonMethodCall(mc.getTargetOwner().getName(), getStartCodeUnit(mc.getOrigin(), c),
                        mc.getTarget().getName()));
            }
        }
        for (JavaConstructorCall cc : c.getConstructorCallsFromSelf()) {
            if (!cc.getTargetOwner().getSimpleName().isEmpty() && context.isDependencyIncluded(res, cc.getTargetOwner().getName(), true)) {
                res.addConstructorCall(new JsonConstructorCall(cc.getTargetOwner().getName(),
                        getStartCodeUnit(cc.getOrigin(), c), cc.getTargetOwner().getSimpleName()));
            }
        }
    }
}
