package com.tngtech.archunit.visual;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethodCall;

class JsonExporter {
    private static final String INNERCLASSSEP = "$";

    void export(JavaClasses classes, File file, VisualizationContext context) {
        JsonJavaPackage root = createStructure(classes, context);
        writeToFile(file, root);
    }

    private JsonJavaPackage createStructure(JavaClasses classes, VisualizationContext context) {
        JsonJavaPackage root = PackageStructureCreator.createPackageStructure(classes, context);
        insertClassesToRoot(classes, context, root);
        root.normalize();
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

    private void insertClassesToRoot(JavaClasses classes, VisualizationContext context, JsonJavaPackage root) {
        List<JavaClass> innerClasses = new LinkedList<>();
        for (JavaClass c : classes) {
            if (context.isElementIncluded(c.getName())) {
                if (c.getName().contains(INNERCLASSSEP)) {
                    innerClasses.add(c);
                } else if (!c.getSimpleName().isEmpty()) {
                    root.insertJavaElement(parseJavaElement(c, context));
                }
            }
        }
        handleInnerClasses(context, root, innerClasses);
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
            parseAnonymousImplementationToJavaElement(c, context, el);
            parseAccessesToJavaElement(c, context, el);
        }
    }

    private String getFullnameOfParentClass(String innerClass) {
        return innerClass.substring(0, innerClass.indexOf(INNERCLASSSEP));
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
        return fullname.replace(INNERCLASSSEP, PackageStructureCreator.PACKAGESEP);
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
                res.addInterface(getCleanedFullname(i.getName()));
            }
        }
    }

    private void parseAnonymousImplementationToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        for (JavaClass i : c.getAllInterfaces()) {
            if (context.isElementIncluded(i.getName())) {
                res.addAnonImpl(getCleanedFullname(i.getName()));
            }
        }
    }

    private void parseAccessesToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        for (JavaFieldAccess fa : c.getFieldAccessesFromSelf()) {
            String targetOwner = getCleanedFullname(fa.getTargetOwner().getName());
            if (!fa.getTargetOwner().getSimpleName().isEmpty() && context.isDependencyIncluded(res, targetOwner, false)) {
                res.addFieldAccess(new JsonFieldAccess(targetOwner, getStartCodeUnit(fa.getOrigin(), c),
                        fa.getTarget().getName()));
            }
        }
        for (JavaMethodCall mc : c.getMethodCallsFromSelf()) {
            String targetOwner = getCleanedFullname(mc.getTargetOwner().getName());
            if (!mc.getTargetOwner().getSimpleName().isEmpty() && context.isDependencyIncluded(res, targetOwner, false)) {
                res.addMethodCall(new JsonMethodCall(targetOwner, getStartCodeUnit(mc.getOrigin(), c),
                        mc.getTarget().getName()));
            }
        }
        for (JavaConstructorCall cc : c.getConstructorCallsFromSelf()) {
            String targetOwner = getCleanedFullname(cc.getTargetOwner().getName());
            if (!cc.getTargetOwner().getSimpleName().isEmpty() && context.isDependencyIncluded(res, targetOwner,
                    cc.getOrigin().isConstructor())) {
                res.addConstructorCall(new JsonConstructorCall(targetOwner,
                        getStartCodeUnit(cc.getOrigin(), c), cc.getTargetOwner().getSimpleName()));
            }
        }
    }
}
