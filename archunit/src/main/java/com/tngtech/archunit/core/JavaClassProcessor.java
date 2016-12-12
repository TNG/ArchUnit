package com.tngtech.archunit.core;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.core.ArchUnitException.ReflectionException;
import com.tngtech.archunit.core.RawAccessRecord.CodeUnit;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.core.ClassFileProcessor.ASM_API_VERSION;

class JavaClassProcessor extends ClassVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(JavaClassProcessor.class);
    private static final AccessHandler NO_OP = new AccessHandler.NoOp();

    private JavaClass.Builder javaClassBuilder;
    private final AccessHandler accessHandler;
    private boolean canImportCurrentClass;
    private String className;

    JavaClassProcessor() {
        this(NO_OP);
    }

    JavaClassProcessor(AccessHandler accessHandler) {
        super(ASM_API_VERSION);
        this.accessHandler = accessHandler;
    }

    Optional<JavaClass> createJavaClass() {
        return javaClassBuilder != null ? Optional.of(javaClassBuilder.build()) : Optional.<JavaClass>absent();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        LOG.info("Analysing class '{}'", name);
        javaClassBuilder = init(name);
        this.className = name.replace("/", ".");
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (!canImportCurrentClass) {
            return super.visitField(access, name, desc, signature, value);
        }

        return new FieldProcessor(javaClassBuilder, new JavaField.Builder()
                .withName(name)
                .withType(TypeDetails.of(Type.getType(desc)))
                .withModifiers(JavaModifier.getModifiersFor(access))
                .withDescriptor(desc));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (!canImportCurrentClass) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        LOG.debug("Analysing method {}.{}:{}", className, name, desc);
        accessHandler.setContext(new CodeUnit(name, TypeDetails.allOf(Type.getArgumentTypes(desc)), className));
        return new MethodProcessor(accessHandler);
    }

    @Override
    public void visitEnd() {
        LOG.debug("Done analysing {}", className);
    }

    private JavaClass.Builder init(String classDescriptor) {
        canImportCurrentClass = true;
        try {
            Class<?> currentClass = JavaType.fromDescriptor(classDescriptor).asClass();
            return new JavaClass.Builder().withType(TypeDetails.of(currentClass));
        } catch (NoClassDefFoundError e) {
            LOG.warn("Can't analyse class '{}' because of missing dependency '{}'",
                    classDescriptor, e.getMessage());
            canImportCurrentClass = false;
        } catch (ReflectionException e) {
            LOG.warn("Can't analyse class '{}' because of missing dependency. Error was: '{}'",
                    classDescriptor, e.getMessage());
            canImportCurrentClass = false;
        }
        return null;
    }

    private static class MethodProcessor extends MethodVisitor {
        private final AccessHandler accessHandler;
        private int actualLineNumber;

        MethodProcessor(AccessHandler accessHandler) {
            super(ASM_API_VERSION);
            this.accessHandler = accessHandler;
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
        private final JavaClass.Builder javaClassBuilder;
        private final JavaField.Builder fieldBuilder;
        private final Set<JavaAnnotation> annotations = new HashSet<>();

        private FieldProcessor(JavaClass.Builder javaClassBuilder, JavaField.Builder fieldBuilder) {
            super(ASM_API_VERSION);

            this.javaClassBuilder = javaClassBuilder;
            this.fieldBuilder = fieldBuilder;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationProcessor(addAnnotationTo(annotations), annotationBuilderFor(desc));
        }

        @Override
        public void visitEnd() {
            fieldBuilder.withAnnotations(annotations);
            javaClassBuilder.addField(fieldBuilder);
        }
    }

    private static JavaAnnotation.Builder annotationBuilderFor(String desc) {
        return new JavaAnnotation.Builder()
                .withType(TypeDetails.of(Type.getType(desc)));
    }

    private static class AnnotationProcessor extends AnnotationVisitor {
        private final TakesAnnotation takesAnnotation;
        private final JavaAnnotation.Builder annotationBuilder;

        private AnnotationProcessor(TakesAnnotation takesAnnotation, JavaAnnotation.Builder annotationBuilder) {
            super(ASM_API_VERSION);
            this.takesAnnotation = takesAnnotation;
            this.annotationBuilder = annotationBuilder;
        }

        @Override
        public void visit(String name, Object value) {
            annotationBuilder.addProperty(name, AnnotationTypeConversion.convert(value));
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            annotationBuilder.addProperty(name, new JavaEnumConstant(TypeDetails.of(Type.getType(desc)), value));
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
            takesAnnotation.add(annotationBuilder.build());
        }
    }

    private static TakesAnnotation addAnnotationTo(final Collection<? super JavaAnnotation> collection) {
        return new TakesAnnotation() {
            @Override
            public void add(JavaAnnotation annotation) {
                collection.add(annotation);
            }
        };
    }

    private static TakesAnnotation addAnnotationAsProperty(final String name, final JavaAnnotation.Builder annotationBuilder) {
        return new TakesAnnotation() {
            @Override
            public void add(JavaAnnotation annotation) {
                annotationBuilder.addProperty(name, annotation);
            }
        };
    }

    private interface TakesAnnotation {
        void add(JavaAnnotation annotation);
    }

    private static class AnnotationArrayProcessor extends AnnotationVisitor {
        private final String name;
        private final JavaAnnotation.Builder annotationBuilder;
        private Class<?> componentType;
        private final List<Object> values = new ArrayList<>();

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
            return new AnnotationProcessor(addAnnotationTo(values), annotationBuilderFor(desc));
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            setComponentType(JavaEnumConstant.class);
            values.add(new JavaEnumConstant(TypeDetails.of(Type.getType(desc)), value));
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
            annotationBuilder.addProperty(name, createArray());
        }

        private Object createArray() {
            Object[] array = (Object[]) Array.newInstance(componentType, values.size());
            return values.toArray(array);
        }
    }

    private static class AnnotationTypeConversion {
        private static final Map<Class<?>, Class<?>> importedTypeToInternalType = ImmutableMap.<Class<?>, Class<?>>of(
                Type.class, TypeDetails.class
        );
        private static final Map<Class<?>, Function<Object, Object>> importedValueToInternalValue =
                ImmutableMap.<Class<?>, Function<Object, Object>>of(
                        Type.class, new Function<Object, Object>() {
                            @Override
                            public Object apply(Object input) {
                                return TypeDetails.of((Type) input);
                            }
                        }
                );

        static Class<?> convert(Class<?> type) {
            return importedTypeToInternalType.containsKey(type) ?
                    importedTypeToInternalType.get(type) :
                    type;
        }

        static Object convert(Object value) {
            return importedValueToInternalValue.containsKey(value.getClass()) ?
                    importedValueToInternalValue.get(value.getClass()).apply(value) :
                    value;
        }
    }
}
