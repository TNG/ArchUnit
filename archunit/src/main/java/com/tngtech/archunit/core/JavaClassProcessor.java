package com.tngtech.archunit.core;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.RawAccessRecord.CodeUnit;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.core.ClassFileProcessor.ASM_API_VERSION;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.JavaStaticInitializer.STATIC_INITIALIZER_NAME;

class JavaClassProcessor extends ClassVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(JavaClassProcessor.class);

    private static final AccessHandler NO_OP = new AccessHandler.NoOp();

    private JavaClass.Builder javaClassBuilder;
    private final Set<JavaAnnotation.Builder> annotations = new HashSet<>();
    private final URI sourceURI;
    private final DeclarationHandler declarationHandler;
    private final AccessHandler accessHandler;
    private String className;

    JavaClassProcessor(URI sourceURI, DeclarationHandler declarationHandler) {
        this(sourceURI, declarationHandler, NO_OP);
    }

    JavaClassProcessor(URI sourceURI, DeclarationHandler declarationHandler, AccessHandler accessHandler) {
        super(ASM_API_VERSION);
        this.sourceURI = sourceURI;
        this.declarationHandler = declarationHandler;
        this.accessHandler = accessHandler;
    }

    Optional<JavaClass> createJavaClass() {
        return javaClassBuilder != null ? Optional.of(javaClassBuilder.build()) : Optional.<JavaClass>absent();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        LOG.info("Analysing class '{}'", name);
        JavaType javaType = JavaType.From.fromAsmObjectTypeName(name);
        if (alreadyImported(javaType)) {
            return;
        }

        ImmutableSet<String> interfaceNames = createInterfaceNames(interfaces);
        LOG.debug("Found interfaces {} on class '{}'", interfaceNames, name);
        boolean opCodeForInterfaceIsPresent = (access & Opcodes.ACC_INTERFACE) != 0;
        Optional<String> superClassName = getSuperClassName(superName, opCodeForInterfaceIsPresent);
        LOG.debug("Found superclass {} on class '{}'", superClassName, name);

        javaClassBuilder = new JavaClass.Builder()
                .withSource(new Source(sourceURI))
                .withType(javaType)
                .withInterface(opCodeForInterfaceIsPresent)
                .withModifiers(JavaModifier.getModifiersForClass(access));

        className = javaType.getName();
        declarationHandler.onNewClass(className, superClassName, interfaceNames);
    }

    private boolean alreadyImported(JavaType javaType) {
        return !declarationHandler.isNew(javaType.getName());
    }

    // NOTE: For some reason ASM claims superName == java/lang/Object for Interfaces???
    //       This is inconsistent with the behavior of Class.getSuperClass()
    private Optional<String> getSuperClassName(String superName, boolean isInterface) {
        return superName != null && !isInterface ?
                Optional.of(createTypeName(superName)) :
                Optional.<String>absent();
    }

    private boolean importAborted() {
        return javaClassBuilder == null;
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (importAborted()) {
            return;
        }

        if (name != null && outerName != null) {
            String innerTypeName = createTypeName(name);
            correctModifiersForNestedClass(innerTypeName, access);
            declarationHandler.registerEnclosingClass(innerTypeName, createTypeName(outerName));
        }
    }

    // Modifier handling is somewhat counter intuitive for nested classes, even though we 'visit' the nested class
    // like any outer class in visit(..) before, the modifiers like 'PUBLIC' or 'PRIVATE'
    // are found in the access flags of visitInnerClass(..)
    private void correctModifiersForNestedClass(String innerTypeName, int access) {
        if (innerTypeName.equals(className)) {
            javaClassBuilder.withModifiers(JavaModifier.getModifiersForClass(access));
        }
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        if (importAborted()) {
            return;
        }

        declarationHandler.registerEnclosingClass(className, createTypeName(owner));
    }

    private ImmutableSet<String> createInterfaceNames(String[] interfaces) {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (String i : interfaces) {
            result.add(createTypeName(i));
        }
        return result.build();
    }

    private String createTypeName(String name) {
        return name.replace("/", ".");
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (importAborted()) {
            return super.visitField(access, name, desc, signature, value);
        }

        JavaField.Builder fieldBuilder = new JavaField.Builder()
                .withName(name)
                .withType(JavaType.From.asmType(Type.getType(desc)))
                .withModifiers(JavaModifier.getModifiersForField(access))
                .withDescriptor(desc);
        declarationHandler.onDeclaredField(fieldBuilder);
        return new FieldProcessor(fieldBuilder);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (importAborted()) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        LOG.debug("Analysing method {}.{}:{}", className, name, desc);
        accessHandler.setContext(new CodeUnit(name, namesOf(Type.getArgumentTypes(desc)), className));

        JavaCodeUnit.Builder<?, ?> codeUnitBuilder = addCodeUnitBuilder(name);
        Type methodType = Type.getMethodType(desc);
        codeUnitBuilder
                .withName(name)
                .withModifiers(JavaModifier.getModifiersForMethod(access))
                .withParameters(typesFrom(methodType.getArgumentTypes()))
                .withReturnType(JavaType.From.asmType(methodType.getReturnType()))
                .withDescriptor(desc);

        return new MethodProcessor(className, accessHandler, codeUnitBuilder);
    }

    private List<JavaType> typesFrom(Type[] asmTypes) {
        List<JavaType> result = new ArrayList<>();
        for (Type asmType : asmTypes) {
            result.add(JavaType.From.asmType(asmType));
        }
        return result;
    }

    private JavaCodeUnit.Builder<?, ?> addCodeUnitBuilder(String name) {
        if (CONSTRUCTOR_NAME.equals(name)) {
            JavaConstructor.Builder builder = new JavaConstructor.Builder();
            declarationHandler.onDeclaredConstructor(builder);
            return builder;
        } else if (STATIC_INITIALIZER_NAME.equals(name)) {
            JavaStaticInitializer.Builder builder = new JavaStaticInitializer.Builder();
            declarationHandler.onDeclaredStaticInitializer(builder);
            return builder;
        } else {
            JavaMethod.Builder builder = new JavaMethod.Builder();
            declarationHandler.onDeclaredMethod(builder);
            return builder;
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (importAborted()) {
            return super.visitAnnotation(desc, visible);
        }

        return new AnnotationProcessor(addAnnotationTo(annotations), annotationBuilderFor(desc));
    }

    @Override
    public void visitEnd() {
        if (importAborted()) {
            return;
        }

        declarationHandler.onDeclaredAnnotations(annotations);
        LOG.debug("Done analysing {}", className);
    }

    private static List<String> namesOf(Type[] types) {
        ImmutableList.Builder<String> result = ImmutableList.builder();
        for (Type type : types) {
            result.add(JavaType.From.asmType(type).getName());
        }
        return result.build();
    }

    private static class MethodProcessor extends MethodVisitor {
        private final String declaringClassName;
        private final AccessHandler accessHandler;
        private final JavaCodeUnit.Builder<?, ?> codeUnitBuilder;
        private final Set<JavaAnnotation.Builder> annotations = new HashSet<>();
        private int actualLineNumber;

        MethodProcessor(String declaringClassName, AccessHandler accessHandler, JavaCodeUnit.Builder<?, ?> codeUnitBuilder) {
            super(ASM_API_VERSION);
            this.declaringClassName = declaringClassName;
            this.accessHandler = accessHandler;
            this.codeUnitBuilder = codeUnitBuilder;
        }

        @Override
        public void visitCode() {
            actualLineNumber = 0;
        }

        // NOTE: ASM doesn't reliably visit this method, so if this method is skipped, line number 0 is recorded
        @Override
        public void visitLineNumber(int line, Label start) {
            LOG.debug("Examining line number {}", line);
            actualLineNumber = line;
            accessHandler.setLineNumber(actualLineNumber);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            accessHandler.handleFieldInstruction(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            accessHandler.handleMethodInstruction(owner, name, desc);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationProcessor(addAnnotationTo(annotations), annotationBuilderFor(desc));
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return new AnnotationDefaultProcessor(declaringClassName, codeUnitBuilder);
        }

        @Override
        public void visitEnd() {
            codeUnitBuilder.withAnnotations(annotations);
        }

        private static class AnnotationDefaultProcessor extends AnnotationVisitor {
            private final String annotationTypeName;
            private final JavaMethod.Builder methodBuilder;

            AnnotationDefaultProcessor(String annotationTypeName, JavaCodeUnit.Builder<?, ?> codeUnitBuilder) {
                super(ClassFileProcessor.ASM_API_VERSION);
                this.annotationTypeName = annotationTypeName;
                checkArgument(codeUnitBuilder instanceof JavaMethod.Builder,
                        "tried to import annotation defaults for code unit '%s' that is not a method " +
                                "(as any annotation.property() is assumed to be), " +
                                "this is likely a bug", codeUnitBuilder.getName());

                methodBuilder = (JavaMethod.Builder) codeUnitBuilder;
            }

            @Override
            public void visit(String name, Object value) {
                methodBuilder.withAnnotationDefaultValue(AnnotationTypeConversion.convert(value));
            }

            @Override
            public void visitEnum(String name, String desc, String value) {
                methodBuilder.withAnnotationDefaultValue(javaEnumBuilder(desc, value));
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String desc) {
                return new AnnotationProcessor(new SetAsAnnotationDefault(annotationTypeName, methodBuilder), annotationBuilderFor(desc));
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                return new AnnotationArrayProcessor(new SetAsAnnotationDefault(annotationTypeName, methodBuilder));
            }

        }
    }

    private static class SetAsAnnotationDefault implements TakesAnnotationBuilder, AnnotationArrayContext {
        private final String annotationTypeName;
        private final JavaMethod.Builder methodBuilder;

        private SetAsAnnotationDefault(String annotationTypeName, JavaMethod.Builder methodBuilder) {
            this.annotationTypeName = annotationTypeName;
            this.methodBuilder = methodBuilder;
        }

        @Override
        public void add(JavaAnnotation.Builder annotation) {
            setArrayResult(JavaAnnotation.ValueBuilder.from(annotation));
        }

        @Override
        public String getDeclaringAnnotationTypeName() {
            return annotationTypeName;
        }

        @Override
        public String getDeclaringAnnotationMemberName() {
            return methodBuilder.getName();
        }

        @Override
        public void setArrayResult(JavaAnnotation.ValueBuilder valueBuilder) {
            methodBuilder.withAnnotationDefaultValue(valueBuilder);
        }
    }

    interface DeclarationHandler {
        boolean isNew(String className);

        void onNewClass(String className, Optional<String> superClassName, Set<String> interfaceNames);

        void onDeclaredField(JavaField.Builder fieldBuilder);

        void onDeclaredConstructor(JavaConstructor.Builder builder);

        void onDeclaredMethod(JavaMethod.Builder builder);

        void onDeclaredStaticInitializer(JavaStaticInitializer.Builder builder);

        void onDeclaredAnnotations(Set<JavaAnnotation.Builder> annotations);

        void registerEnclosingClass(String ownerName, String enclosingClassName);
    }

    interface AccessHandler {
        void handleFieldInstruction(int opcode, String owner, String name, String desc);

        void setContext(CodeUnit codeUnit);

        void setLineNumber(int lineNumber);

        void handleMethodInstruction(String owner, String name, String desc);

        class NoOp implements AccessHandler {
            @Override
            public void handleFieldInstruction(int opcode, String owner, String name, String desc) {
            }

            @Override
            public void setContext(CodeUnit codeUnit) {
            }

            @Override
            public void setLineNumber(int lineNumber) {
            }

            @Override
            public void handleMethodInstruction(String owner, String name, String desc) {
            }
        }
    }

    private static class FieldProcessor extends FieldVisitor {
        private final JavaField.Builder fieldBuilder;
        private final Set<JavaAnnotation.Builder> annotations = new HashSet<>();

        private FieldProcessor(JavaField.Builder fieldBuilder) {
            super(ASM_API_VERSION);

            this.fieldBuilder = fieldBuilder;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationProcessor(addAnnotationTo(annotations), annotationBuilderFor(desc));
        }

        @Override
        public void visitEnd() {
            fieldBuilder.withAnnotations(annotations);
        }
    }

    private static JavaAnnotation.Builder annotationBuilderFor(String desc) {
        return new JavaAnnotation.Builder().withType(JavaType.From.asmType(Type.getType(desc)));
    }

    private static class AnnotationProcessor extends AnnotationVisitor {
        private final TakesAnnotationBuilder takesAnnotationBuilder;
        private final JavaAnnotation.Builder annotationBuilder;

        private AnnotationProcessor(TakesAnnotationBuilder takesAnnotationBuilder, JavaAnnotation.Builder annotationBuilder) {
            super(ASM_API_VERSION);
            this.takesAnnotationBuilder = takesAnnotationBuilder;
            this.annotationBuilder = annotationBuilder;
        }

        @Override
        public void visit(String name, Object value) {
            annotationBuilder.addProperty(name, AnnotationTypeConversion.convert(value));
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            annotationBuilder.addProperty(name, javaEnumBuilder(desc, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String name, String desc) {
            return new AnnotationProcessor(addAnnotationAsProperty(name, annotationBuilder), annotationBuilderFor(desc));
        }

        @Override
        public AnnotationVisitor visitArray(final String name) {
            return new AnnotationArrayProcessor(new AnnotationArrayContext() {
                @Override
                public String getDeclaringAnnotationTypeName() {
                    return annotationBuilder.getType().getName();
                }

                @Override
                public String getDeclaringAnnotationMemberName() {
                    return name;
                }

                @Override
                public void setArrayResult(JavaAnnotation.ValueBuilder valueBuilder) {
                    annotationBuilder.addProperty(name, valueBuilder);
                }
            });
        }

        @Override
        public void visitEnd() {
            takesAnnotationBuilder.add(annotationBuilder);
        }
    }

    private static TakesAnnotationBuilder addAnnotationTo(final Collection<? super JavaAnnotation.Builder> collection) {
        return new TakesAnnotationBuilder() {
            @Override
            public void add(JavaAnnotation.Builder annotation) {
                collection.add(annotation);
            }
        };
    }

    private static TakesAnnotationBuilder addAnnotationAsProperty(final String name, final JavaAnnotation.Builder annotationBuilder) {
        return new TakesAnnotationBuilder() {
            @Override
            public void add(JavaAnnotation.Builder builder) {
                annotationBuilder.addProperty(name, JavaAnnotation.ValueBuilder.from(builder));
            }
        };
    }

    private interface TakesAnnotationBuilder {
        void add(JavaAnnotation.Builder annotation);
    }

    private static class AnnotationArrayProcessor extends AnnotationVisitor {
        private final AnnotationArrayContext annotationArrayContext;
        private Class<?> derivedComponentType;
        private final List<JavaAnnotation.ValueBuilder> values = new ArrayList<>();

        private AnnotationArrayProcessor(AnnotationArrayContext annotationArrayContext) {
            super(ASM_API_VERSION);
            this.annotationArrayContext = annotationArrayContext;
        }

        @Override
        public void visit(String name, Object value) {
            setDerivedComponentType(value);
            values.add(AnnotationTypeConversion.convert(value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            setDerivedComponentType(JavaAnnotation.class);
            return new AnnotationProcessor(new TakesAnnotationBuilder() {
                @Override
                public void add(JavaAnnotation.Builder annotationBuilder) {
                    values.add(JavaAnnotation.ValueBuilder.from(annotationBuilder));
                }
            }, annotationBuilderFor(desc));
        }

        @Override
        public void visitEnum(String name, final String desc, final String value) {
            setDerivedComponentType(JavaEnumConstant.class);
            values.add(javaEnumBuilder(desc, value));
        }

        private void setDerivedComponentType(Object value) {
            setDerivedComponentType(value.getClass());
        }

        // NOTE: If the declared annotation is not imported itself, we can still use this heuristic,
        //       to determine additional information about the respective array.
        //       (It the annotation is imported itself, we could easily determine this from the respective
        //       JavaClass methods)
        private void setDerivedComponentType(Class<?> type) {
            type = AnnotationTypeConversion.convert(type);
            checkState(derivedComponentType == null || derivedComponentType.equals(type),
                    "Found mixed component types while importing array, this is most likely a bug");

            derivedComponentType = type;
        }

        @Override
        public void visitEnd() {
            annotationArrayContext.setArrayResult(new ArrayValueBuilder());
        }

        private class ArrayValueBuilder extends JavaAnnotation.ValueBuilder {
            @Override
            Optional<Object> build(ImportedClasses.ByTypeName importedClasses) {
                Optional<Class<?>> componentType = determineComponentType(importedClasses);
                if (!componentType.isPresent()) {
                    return Optional.absent();
                }

                return Optional.of(toArray(componentType.get(), buildValues(importedClasses)));
            }

            @SuppressWarnings("unchecked") // NOTE: We assume the component type matches the list
            private Object toArray(Class<?> componentType, List<Object> values) {
                if (componentType == boolean.class) {
                    return Booleans.toArray((Collection) values);
                } else if (componentType == byte.class) {
                    return Bytes.toArray((Collection) values);
                } else if (componentType == short.class) {
                    return Shorts.toArray((Collection) values);
                } else if (componentType == int.class) {
                    return Ints.toArray((Collection) values);
                } else if (componentType == long.class) {
                    return Longs.toArray((Collection) values);
                } else if (componentType == float.class) {
                    return Floats.toArray((Collection) values);
                } else if (componentType == double.class) {
                    return Doubles.toArray((Collection) values);
                } else if (componentType == char.class) {
                    return Chars.toArray((Collection) values);
                }
                return values.toArray((Object[]) Array.newInstance(componentType, values.size()));
            }

            private List<Object> buildValues(ImportedClasses.ByTypeName importedClasses) {
                List<Object> result = new ArrayList<>();
                for (JavaAnnotation.ValueBuilder value : values) {
                    result.addAll(value.build(importedClasses).asSet());
                }
                return result;
            }

            private Optional<Class<?>> determineComponentType(ImportedClasses.ByTypeName importedClasses) {
                if (derivedComponentType != null) {
                    return Optional.<Class<?>>of(derivedComponentType);
                }

                JavaClass annotationType = importedClasses.get(annotationArrayContext.getDeclaringAnnotationTypeName());
                Optional<JavaMethod> method = annotationType
                        .tryGetMethod(annotationArrayContext.getDeclaringAnnotationMemberName());

                return method.isPresent() ?
                        determineComponentTypeFromReturnValue(method) :
                        Optional.<Class<?>>absent();
            }

            private Optional<Class<?>> determineComponentTypeFromReturnValue(Optional<JavaMethod> method) {
                String name = method.get().getReturnType().getName();
                Optional<Class<?>> result = AnnotationTypeConversion.tryConvert(name);
                if (result.isPresent()) {
                    return Optional.<Class<?>>of(result.get().getComponentType());
                }
                return resolveComponentTypeFrom(name);
            }

            @MayResolveTypesViaReflection(reason = "Resolving primitives does not really use reflection")
            private Optional<Class<?>> resolveComponentTypeFrom(String name) {
                JavaType type = JavaType.From.name(name);
                JavaType componentType = getComponentType(type);

                if (componentType.isPrimitive()) {
                    return Optional.<Class<?>>of(componentType.resolveClass());
                }
                if (String.class.getName().equals(componentType.getName())) {
                    return Optional.<Class<?>>of(String.class);
                }

                // if we couldn't determine the type up to now, it must be an empty enum or annotation array,
                // it's not completely consistent, but we'll just treat this as Object array for now and see
                // if this will ever make a problem, since annotation proxy would to the conversion backwards
                // and if one has to handle get(property): Object, this to be an empty Object[]
                // instead of a JavaEnumConstant[] or JavaAnnotation[] should hardly cause any real problems
                return Optional.<Class<?>>of(Object.class);
            }

            private JavaType getComponentType(JavaType type) {
                Optional<JavaType> result = type.tryGetComponentType();
                checkState(result.isPresent(), "Couldn't determine component type of array return type %s, " +
                        "this is most likely a bug", type.getName());

                return result.get();
            }
        }
    }


    private interface AnnotationArrayContext {
        String getDeclaringAnnotationTypeName();

        String getDeclaringAnnotationMemberName();

        void setArrayResult(JavaAnnotation.ValueBuilder valueBuilder);
    }

    private static JavaAnnotation.ValueBuilder javaEnumBuilder(final String desc, final String value) {
        return new JavaAnnotation.ValueBuilder() {
            @Override
            Optional<Object> build(ImportedClasses.ByTypeName importedClasses) {
                return Optional.<Object>of(
                        new JavaEnumConstant(importedClasses.get(Type.getType(desc).getClassName()), value));
            }
        };
    }

    private static class AnnotationTypeConversion {
        private static final Map<String, Class<?>> externalTypeToInternalType = ImmutableMap.of(
                Type.class.getName(), JavaClass.class,
                Class.class.getName(), JavaClass.class,
                Class[].class.getName(), JavaClass[].class
        );
        private static final Map<Class<?>, Function<Object, JavaAnnotation.ValueBuilder>> importedValueToInternalValue =
                ImmutableMap.<Class<?>, Function<Object, JavaAnnotation.ValueBuilder>>of(
                        Type.class, new Function<Object, JavaAnnotation.ValueBuilder>() {
                            @Override
                            public JavaAnnotation.ValueBuilder apply(final Object input) {
                                return new JavaAnnotation.ValueBuilder() {
                                    @Override
                                    Optional<Object> build(ImportedClasses.ByTypeName importedClasses) {
                                        return Optional.<Object>of(importedClasses.get(((Type) input).getClassName()));
                                    }
                                };
                            }
                        }
                );

        static Class<?> convert(Class<?> type) {
            return externalTypeToInternalType.containsKey(type.getName()) ?
                    externalTypeToInternalType.get(type.getName()) :
                    type;
        }

        static Optional<Class<?>> tryConvert(String typeName) {
            return externalTypeToInternalType.containsKey(typeName) ?
                    Optional.<Class<?>>of(externalTypeToInternalType.get(typeName)) :
                    Optional.<Class<?>>absent();
        }

        static JavaAnnotation.ValueBuilder convert(Object value) {
            return importedValueToInternalValue.containsKey(value.getClass()) ?
                    importedValueToInternalValue.get(value.getClass()).apply(value) :
                    JavaAnnotation.ValueBuilder.ofFinished(value);
        }
    }
}
