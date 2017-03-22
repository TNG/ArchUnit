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
        Set<String> pkgs = new HashSet<>();
        for (JavaClass c : classes) {
            if (context.isIncluded(c.getName())) {
                pkgs.add(c.getPackage());
            }
        }
        JsonJavaPackage root = JsonJavaPackage.createTreeStructure(pkgs);
        List<JavaClass> innerClasses = new LinkedList<>();
        for (JavaClass c : classes) {
            if (c.getName().contains("$")) {
                innerClasses.add(c);
            } else if (!c.getSimpleName().isEmpty() && context.isIncluded(c.getName())) {
                root.insertJavaElement(parseJavaElement(c, context));
            }
        }
        for (JavaClass c : innerClasses) {
            if (!c.getSimpleName().isEmpty() && context.isIncluded(c.getName())) {
                root.insertJavaElement(parseJavaElement(c, context));
            }
        }
        root.normalizeForExport();

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

    private JsonJavaElement parseJavaElement(JavaClass c, VisualizationContext context) {
        if (c.isInterface()) {
            return parseJavaInterface(c, context);
        } else {
            return parseJavaClass(c, context);
        }
    }

    private String getSuperClass(JavaClass c, VisualizationContext context) {
        return c.getSuperClass().isPresent() && context.isIncluded(c.getSuperClass().get().getName()) ?
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

    private boolean isRelevantDep(String targetOwner, String targetOwnerSimpleName, String origOwner,
                                  VisualizationContext context) {
        return !targetOwnerSimpleName.isEmpty() && context.isIncluded(targetOwner) &&
                !targetOwner.equals(origOwner);
    }

    private void parseToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        /**if (c.getEnclosingClass().isPresent()) {
         res.addChild(parseJavaClass(c.getEnclosingClass().get(), basePath));
         }*/
        for (JavaClass i : c.getAllInterfaces()) {
            if (context.isIncluded(i.getName())) {
                res.addInterface(i.getName());
            }
        }
        for (JavaFieldAccess fa : c.getFieldAccessesFromSelf()) {
            if (isRelevantDep(fa.getTargetOwner().getName(), fa.getTargetOwner().getSimpleName(), c.getName(), context)) {
                res.addFieldAccess(new JsonFieldAccess(fa.getTargetOwner().getName(), getStartCodeUnit(fa.getOrigin(), c),
                        fa.getTarget().getName()));
            }
        }
        for (JavaMethodCall mc : c.getMethodCallsFromSelf()) {
            if (isRelevantDep(mc.getTargetOwner().getName(), mc.getTargetOwner().getSimpleName(), c.getName(), context)) {
                res.addMethodCall(new JsonMethodCall(mc.getTargetOwner().getName(), getStartCodeUnit(mc.getOrigin(), c),
                        mc.getTarget().getName()));
            }
        }
        for (JavaConstructorCall cc : c.getConstructorCallsFromSelf()) {
            if (isRelevantDep(cc.getTargetOwner().getName(), cc.getTargetOwner().getSimpleName(), c.getName(), context)) {
                res.addConstructorCall(new JsonConstructorCall(cc.getTargetOwner().getName(),
                        getStartCodeUnit(cc.getOrigin(), c), cc.getTargetOwner().getSimpleName()));
            }
        }
    }
}
