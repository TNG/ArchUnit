package com.tngtech.archunit.visual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.*;

import java.io.*;

class JsonExporter {
    private static final String INNER_CLASS_SEPARATOR = "$";

    void export(JavaClasses classes, File file, VisualizationContext context) {
        VisualizedClasses visualizedClasses = VisualizedClasses.from(classes, context);
        JsonJavaPackage root = createStructure(visualizedClasses, context);
        writeToFile(file, root);
    }

    private JsonJavaPackage createStructure(VisualizedClasses visualizedClasses, VisualizationContext context) {
        JsonJavaPackage root = PackageStructureCreator.createPackageStructure(visualizedClasses.getAll());
        insertVisualizedClassesToRoot(visualizedClasses, context, root);
        root.normalize();
        return root;
    }

    /*private void handleInnerClasses(VisualizationContext context, JsonJavaPackage root, Collection<JavaClass> innerClasses) {
        for (JavaClass c : innerClasses) {
            if (c.getSimpleName().isEmpty()) {
                addDependenciesOfAnonymousInnerClassToParent(context, root, c);
            } else {
                root.insertJavaElement(parseJavaElement(c, context));
            }
        }
    }*/

    private void insertVisualizedClassesToRoot(VisualizedClasses visualizedClasses, VisualizationContext context, JsonJavaPackage root) {
        insertClassesToRoot(visualizedClasses.getClasses(), context, root);
        insertInnerClassesToRoot(visualizedClasses.getInnerClasses(), context, root);
        insertDependenciesToRoot(visualizedClasses.getDependencies(), root);

        /*Collection<JavaClass> innerClasses = new LinkedList<>();
        for (JavaClass c : classes.getClasses()) {
            if (context.isElementIncluded(c.getName())) {
                // FIXME: Test for being inner class as javaClass.getEnclosingClass().isPresent()
                if (c.getName().contains(INNER_CLASS_SEPARATOR)) {
                    innerClasses.add(c);
                } else if (!c.getSimpleName().isEmpty()) { // FIXME: Test via javaClass.isAnonymous()
                    root.insertJavaElement(parseJavaElement(c, context));
                }
            }
        }
        handleInnerClasses(context, root, innerClasses);*/
    }

    private void insertDependenciesToRoot(Iterable<JavaClass> dependencies, JsonJavaPackage root) {
        for (JavaClass c : dependencies) {
            root.insertJavaElement(parseJavaElementWithoutDependencies(c));
        }
    }

    private void insertInnerClassesToRoot(Iterable<JavaClass> innerClasses, VisualizationContext context, JsonJavaPackage root) {
        for (JavaClass c : innerClasses) {
            if (c.getSimpleName().isEmpty()) {
                addDependenciesOfAnonymousInnerClassToParent(context, root, c);
            } else {
                root.insertJavaElement(parseJavaElement(c, context));
            }
        }
    }

    private void insertClassesToRoot(Iterable<JavaClass> classes, VisualizationContext context, JsonJavaPackage root) {
        for (JavaClass c : classes) {
            root.insertJavaElement(parseJavaElement(c, context));
        }
    }

    private void writeToFile(File file, JsonJavaPackage root) {
        final GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();
        try {
            // FIXME: Is there a less bloated way with Guava?
            String jsonString = gson.toJson(root);
            gson.toJson(root, new FileWriter(file));
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(jsonString);
            myOutWriter.close();
            fOut.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
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

    // FIXME: Operate on JavaClass, i.e. javaClass.getEnclosingClass.get().getName()
    private String getFullnameOfParentClass(String innerClass) {
        return innerClass.substring(0, innerClass.indexOf(INNER_CLASS_SEPARATOR));
    }

    private JsonJavaElement parseJavaElementWithoutDependencies(JavaClass c) {
        if (c.isInterface()) {
            return new JsonJavaInterface(c.getSimpleName(), getCleanedFullname(c.getName()));
        } else {
            return new JsonJavaClass(c.getSimpleName(), getCleanedFullname(c.getName()), "class", "");
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
        String superClass;
        return c.getSuperClass().isPresent() && context.isElementIncluded(superClass = getCleanedFullname(c.getSuperClass().get().getName())) ?
                superClass : "";
    }

    static String getCleanedFullname(String fullname) {
        return fullname.replace(INNER_CLASS_SEPARATOR, PackageStructureCreator.PACKAGE_SEPARATOR);
    }

    private JsonJavaElement parseJavaClass(final JavaClass c, VisualizationContext context) {
        final JsonJavaClass res = new JsonJavaClass(c.getSimpleName(), getCleanedFullname(c.getName()),
                "class", getSuperClass(c, context));
        parseToJavaElement(c, context, res);
        return res;
    }

    private JsonJavaElement parseJavaInterface(JavaClass c, VisualizationContext context) {
        JsonJavaInterface res = new JsonJavaInterface(c.getSimpleName(), getCleanedFullname(c.getName()));
        parseToJavaElement(c, context, res);
        return res;
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
        // FIXME: Don't use shortcuts like c, fa, etc., we're not writing Assembler ;-) The chars don't cost, rather make simpler if(..) expressions
        for (JavaFieldAccess fa : c.getFieldAccessesFromSelf()) {
            String targetOwner = getCleanedFullname(fa.getTargetOwner().getName());
            if (isValidDependency(res.fullname, targetOwner, fa.getTargetOwner().getSimpleName()) && context.isElementIncluded(targetOwner)) {
                res.addFieldAccess(new JsonFieldAccess(targetOwner, fa.getOrigin().getName(),
                        fa.getTarget().getName()));
            }
        }
        for (JavaMethodCall mc : c.getMethodCallsFromSelf()) {
            String targetOwner = getCleanedFullname(mc.getTargetOwner().getName());
            if (isValidDependency(res.fullname, targetOwner, mc.getTargetOwner().getSimpleName()) && context.isElementIncluded(targetOwner)) {
                res.addMethodCall(new JsonMethodCall(targetOwner, mc.getOrigin().getName(),
                        mc.getTarget().getName()));
            }
        }
        for (JavaConstructorCall cc : c.getConstructorCallsFromSelf()) {
            String targetOwner = getCleanedFullname(cc.getTargetOwner().getName());
            if (isValidDependency(res.fullname, targetOwner, cc.getTargetOwner().getSimpleName()) && context.isElementIncluded(targetOwner)) {
                res.addConstructorCall(new JsonConstructorCall(targetOwner,
                        cc.getOrigin().getName(), cc.getTarget().getName()));
            }
        }
    }

    private boolean isValidDependency(String origin, String targetOwnerFullname, String targetOwnerSimplename) {
        return !targetOwnerSimplename.isEmpty() && !targetOwnerFullname.equals(origin);
    }
}
