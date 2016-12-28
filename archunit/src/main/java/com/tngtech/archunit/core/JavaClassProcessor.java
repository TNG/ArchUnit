package com.tngtech.archunit.core;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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

import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.core.ClassFileProcessor.ASM_API_VERSION;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.JavaStaticInitializer.STATIC_INITIALIZER_NAME;
import static com.tngtech.archunit.core.ReflectionUtils.ensureCorrectArrayTypeName;

class JavaClassProcessor extends ClassVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(JavaClassProcessor.class);

    private static final AccessHandler NO_OP = new AccessHandler.NoOp();

    private JavaClass.Builder javaClassBuilder;
    private final Set<JavaAnnotation.Builder> annotations = new HashSet<>();
    private final DeclarationHandler declarationHandler;
    private final AccessHandler accessHandler;
    private String className;

    JavaClassProcessor(DeclarationHandler declarationHandler) {
        this(declarationHandler, NO_OP);
    }

    JavaClassProcessor(DeclarationHandler declarationHandler, AccessHandler accessHandler) {
        super(ASM_API_VERSION);
        this.declarationHandler = declarationHandler;
        this.accessHandler = accessHandler;
    }

    Optional<JavaClass> createJavaClass() {
        return Optional.of(javaClassBuilder.build());
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        LOG.info("Analysing class '{}'", name);
        ImmutableSet<String> interfaceNames = createInterfaceNames(interfaces);
        LOG.debug("Found interfaces {} on class '{}'", interfaceNames, name);
        Optional<String> superClassName = superName != null ?
                Optional.of(createTypeName(superName)) :
                Optional.<String>absent();
        LOG.debug("Found superclass {} on class '{}'", superClassName, name);

        javaClassBuilder = new JavaClass.Builder().withType(JavaType.From.fromAsmObjectTypeName(name));
        boolean opCodeForInterfaceIsPresent = (access & Opcodes.ACC_INTERFACE) != 0;
        javaClassBuilder.withInterface(opCodeForInterfaceIsPresent);
        className = createTypeName(name);
        declarationHandler.onNewClass(className, superClassName, interfaceNames);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (name != null && outerName != null) {
            declarationHandler.registerEnclosingClass(createTypeName(name), createTypeName(outerName));
        }
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
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
        JavaField.Builder fieldBuilder = new JavaField.Builder()
                .withName(name)
                .withType(Type.getType(desc))
                .withModifiers(JavaModifier.getModifiersFor(access))
                .withDescriptor(desc);
        declarationHandler.onDeclaredField(fieldBuilder);
        return new FieldProcessor(fieldBuilder);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        LOG.debug("Analysing method {}.{}:{}", className, name, desc);
        accessHandler.setContext(new CodeUnit(name, namesOf(Type.getArgumentTypes(desc)), className));

        JavaCodeUnit.Builder<?, ?> codeUnitBuilder = addCodeUnitBuilder(name);
        Type methodType = Type.getMethodType(desc);
        codeUnitBuilder
                .withName(name)
                .withModifiers(JavaModifier.getModifiersFor(access))
                .withParameters(methodType.getArgumentTypes())
                .withReturnType(methodType.getReturnType())
                .withDescriptor(desc);

        return new MethodProcessor(accessHandler, codeUnitBuilder);
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
        return new AnnotationProcessor(addAnnotationTo(annotations), annotationBuilderFor(desc));
    }

    @Override
    public void visitEnd() {
        declarationHandler.onDeclaredAnnotations(annotations);
        LOG.debug("Done analysing {}", className);
    }

    private static List<String> namesOf(Type[] types) {
        ImmutableList.Builder<String> result = ImmutableList.builder();
        for (Type type : types) {
            result.add(ensureCorrectArrayTypeName(type.getClassName())); // FIXME: Centralise the array handling and write test for array parameter, since all tests up to ClassCacheTest passed with wrong handling
        }
        return result.build();
    }

    private static class MethodProcessor extends MethodVisitor {
        private final AccessHandler accessHandler;
        private final JavaCodeUnit.Builder<?, ?> codeUnitBuilder;
        private final Set<JavaAnnotation.Builder> annotations = new HashSet<>();
        private int actualLineNumber;

        MethodProcessor(AccessHandler accessHandler, JavaCodeUnit.Builder<?, ?> codeUnitBuilder) {
            super(ASM_API_VERSION);
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
            accessHandler.handleMethodInstruction(opcode, owner, name, desc);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationProcessor(addAnnotationTo(annotations), annotationBuilderFor(desc));
        }

        @Override
        public void visitEnd() {
            codeUnitBuilder.withAnnotations(annotations);
        }
    }

    interface DeclarationHandler {
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

        void handleMethodInstruction(int opcode, String owner, String name, String desc);

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
            public void handleMethodInstruction(int opcode, String owner, String name, String desc) {
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
        return new JavaAnnotation.Builder()
                .withType(Type.getType(desc));
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
        public AnnotationVisitor visitArray(String name) {
            return new AnnotationArrayProcessor(name, annotationBuilder);
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
        private final String name;
        private final JavaAnnotation.Builder annotationBuilder;
        private Class<?> componentType;
        private final List<JavaAnnotation.ValueBuilder> values = new ArrayList<>();

        private AnnotationArrayProcessor(String name, JavaAnnotation.Builder annotationBuilder) {
            super(ASM_API_VERSION);
            this.name = name;
            this.annotationBuilder = annotationBuilder;
        }

        @Override
        public void visit(String name, Object value) {
            setComponentType(value);
            values.add(AnnotationTypeConversion.convert(value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            setComponentType(JavaAnnotation.class);
            return new AnnotationProcessor(new TakesAnnotationBuilder() {
                @Override
                public void add(JavaAnnotation.Builder annotationBuilder) {
                    values.add(JavaAnnotation.ValueBuilder.from(annotationBuilder));
                }
            }, annotationBuilderFor(desc));
        }

        @Override
        public void visitEnum(String name, final String desc, final String value) {
            setComponentType(JavaEnumConstant.class);
            values.add(javaEnumBuilder(desc, value));
        }

        private void setComponentType(Object value) {
            setComponentType(value.getClass());
        }

        private void setComponentType(Class<?> type) {
            type = AnnotationTypeConversion.convert(type);
            checkState(componentType == null || componentType.equals(type),
                    "Found mixed component types while importing array, this is most likely a bug");

            componentType = type;
        }

        @Override
        public void visitEnd() {
            annotationBuilder.addProperty(name, valueArrayBuilder());
        }

        private JavaAnnotation.ValueBuilder valueArrayBuilder() {
            return new JavaAnnotation.ValueBuilder() {
                @Override
                Object build(ImportedClasses.ByTypeName importedClasses) {
                    Object[] array = (Object[]) Array.newInstance(componentType, values.size());
                    for (int i = 0; i < values.size(); i++) {
                        array[i] = values.get(i).build(importedClasses);
                    }
                    return array;
                }
            };
        }
    }

    private static JavaAnnotation.ValueBuilder javaEnumBuilder(final String desc, final String value) {
        return new JavaAnnotation.ValueBuilder() {
            @Override
            Object build(ImportedClasses.ByTypeName importedClasses) {
                return new JavaEnumConstant(importedClasses.get(Type.getType(desc).getClassName()), value);
            }
        };
    }

    private static class AnnotationTypeConversion {
        private static final Map<Class<?>, Class<?>> importedTypeToInternalType = ImmutableMap.<Class<?>, Class<?>>of(
                Type.class, JavaClass.class
        );
        private static final Map<Class<?>, Function<Object, JavaAnnotation.ValueBuilder>> importedValueToInternalValue =
                ImmutableMap.<Class<?>, Function<Object, JavaAnnotation.ValueBuilder>>of(
                        Type.class, new Function<Object, JavaAnnotation.ValueBuilder>() {
                            @Override
                            public JavaAnnotation.ValueBuilder apply(final Object input) {
                                return new JavaAnnotation.ValueBuilder() {
                                    @Override
                                    Object build(ImportedClasses.ByTypeName importedClasses) {
                                        return importedClasses.get(((Type) input).getClassName());
                                    }
                                };
                            }
                        }
                );

        static Class<?> convert(Class<?> type) {
            return importedTypeToInternalType.containsKey(type) ?
                    importedTypeToInternalType.get(type) :
                    type;
        }

        static JavaAnnotation.ValueBuilder convert(Object value) {
            return importedValueToInternalValue.containsKey(value.getClass()) ?
                    importedValueToInternalValue.get(value.getClass()).apply(value) :
                    JavaAnnotation.ValueBuilder.ofFinished(value);
        }
    }
}
