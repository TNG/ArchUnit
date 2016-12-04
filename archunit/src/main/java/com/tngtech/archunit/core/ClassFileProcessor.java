package com.tngtech.archunit.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.ForwardingMap;
import com.tngtech.archunit.core.JavaClass.TypeAnalysisListener;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.RawAccessRecord.ConstructorTargetInfo;
import com.tngtech.archunit.core.RawAccessRecord.FieldTargetInfo;
import com.tngtech.archunit.core.RawAccessRecord.MethodTargetInfo;
import com.tngtech.archunit.core.RawAccessRecord.TargetInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.ClassFileProcessor.CodeUnitIdentifier.staticInitializerIdentifier;
import static com.tngtech.archunit.core.ImportWorkaround.getAllSuperClasses;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.JavaStaticInitializer.STATIC_INITIALIZER_NAME;
import static com.tngtech.archunit.core.RawAccessRecord.CodeUnit.staticInitializerOf;
import static org.objectweb.asm.Opcodes.ASM5;

class ClassFileProcessor extends ClassVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(ClassFileProcessor.class);

    private final ClassFileImportRecord importRecord = new ClassFileImportRecord();

    private final ProcessingContext processingContext = new ProcessingContext();

    ClassFileProcessor() {
        super(ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        LOG.info("Analysing class '{}'", name);
        processingContext.init(name);

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (!processingContext.canImportCurrentClass) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
        LOG.debug("Analysing method {}.{}:{}", processingContext.getCurrentClassName(), name, desc);
        MethodVisitor delegate = super.visitMethod(access, name, desc, signature, exceptions);
        return new MethodProcessor(processingContext.getCodeUnit(name, desc), importRecord, delegate);
    }

    @Override
    public void visitEnd() {
        if (processingContext.canImportCurrentClass) {
            LOG.debug("Done analysing {}", processingContext.getCurrentClassName());
            importRecord.add(processingContext.finish());
        }
        super.visitEnd();
    }

    JavaClasses process(ClassFileSource source) {
        ClassFileProcessor child = new ClassFileProcessor();
        for (Supplier<InputStream> stream : source) {
            try (InputStream s = stream.get()) {
                new ClassReader(s).accept(child, 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new ClassFileImportContext(child.importRecord).complete();
    }

    private static class ProcessingContext {
        private Class<?> currentClass;
        private CodeUnitRecorder codeUnitRecorder;
        private JavaClass.Builder currentClassBuilder;

        private boolean canImportCurrentClass;

        void init(String classDescriptor) {
            canImportCurrentClass = true;
            try {
                tryInit(classDescriptor);
            } catch (NoClassDefFoundError e) {
                LOG.warn("Can't analyse class '{}' because of missing dependency '{}'",
                        classDescriptor, e.getMessage());
                canImportCurrentClass = false;
            } catch (ReflectionException e) {
                LOG.warn("Can't analyse class '{}' because of missing dependency. Error was: '{}'",
                        classDescriptor, e.getMessage());
                canImportCurrentClass = false;
            }
        }

        private void tryInit(String classDescriptor) {
            currentClass = classForDescriptor(classDescriptor);
            codeUnitRecorder = new CodeUnitRecorder(currentClass);
            currentClassBuilder = new JavaClass.Builder(codeUnitRecorder).withType(TypeDetails.of(currentClass));
        }

        private Class<?> classForDescriptor(String descriptor) {
            return JavaType.fromDescriptor(descriptor).asClass();
        }

        String getCurrentClassName() {
            return currentClass.getName();
        }

        CodeUnit getCodeUnit(String name, String desc) {
            return codeUnitRecorder.get(CodeUnitIdentifier.of(name, desc, currentClass.getName()));
        }

        JavaClass finish() {
            JavaClass javaClass = currentClassBuilder.build();
            currentClass = null;
            codeUnitRecorder = null;
            currentClassBuilder = null;
            return javaClass;
        }
    }

    private static class MethodProcessor extends MethodVisitor {
        private final CodeUnit currentCodeUnit;
        private final ClassFileImportRecord importRecord;

        private Set<RawAccessRecord.ForField> fieldAccessRecordBuilders = new HashSet<>();
        private Set<RawAccessRecord> methodCallRecordBuilders = new HashSet<>();
        private Set<RawAccessRecord> constructorCallRecordBuilders = new HashSet<>();
        private int actualLineNumber;

        MethodProcessor(CodeUnit currentCodeUnit, ClassFileImportRecord importRecord, MethodVisitor mv) {
            super(ASM5, mv);
            this.currentCodeUnit = currentCodeUnit;
            this.importRecord = importRecord;
        }

        @Override
        public void visitCode() {
            actualLineNumber = 0;
            super.visitCode();
        }

        // NOTE: ASM doesn't reliably visit this method, so if this method is skipped, line number 0 is recorded
        @Override
        public void visitLineNumber(int line, Label start) {
            LOG.debug("Examining line number {}", line);
            actualLineNumber = line;
            super.visitLineNumber(line, start);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            AccessType accessType = AccessType.forOpCode(opcode);
            LOG.debug("Found {} access to field {}.{}:{} in line {}", accessType, owner, name, desc, actualLineNumber);
            TargetInfo target = new FieldTargetInfo(owner, name, desc);
            fieldAccessRecordBuilders.add(filled(new RawAccessRecord.ForField.Builder(), target)
                    .withAccessType(accessType)
                    .build());

            super.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            LOG.debug("Found call of method {}.{}:{} in line {}", owner, name, desc, actualLineNumber);
            if (CONSTRUCTOR_NAME.equals(name)) {
                TargetInfo target = new ConstructorTargetInfo(owner, name, desc);
                constructorCallRecordBuilders.add(filled(new RawAccessRecord.Builder(), target).build());
            } else {
                TargetInfo target = new MethodTargetInfo(owner, name, desc);
                methodCallRecordBuilders.add(filled(new RawAccessRecord.Builder(), target).build());
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }

        private <BUILDER extends RawAccessRecord.BaseBuilder<BUILDER>> BUILDER filled(BUILDER builder, TargetInfo target) {
            return builder
                    .withCaller(currentCodeUnit)
                    .withTarget(target)
                    .withLineNumber(actualLineNumber);
        }

        @Override
        public void visitEnd() {
            for (RawAccessRecord.ForField fieldAccessRecord : fieldAccessRecordBuilders) {
                importRecord.registerFieldAccess(fieldAccessRecord);
            }
            fieldAccessRecordBuilders = new HashSet<>();
            for (RawAccessRecord methodCallRecord : methodCallRecordBuilders) {
                importRecord.registerMethodCall(methodCallRecord);
            }
            methodCallRecordBuilders = new HashSet<>();
            for (RawAccessRecord constructorCallRecord : constructorCallRecordBuilders) {
                importRecord.registerConstructorCall(constructorCallRecord);
            }
            constructorCallRecordBuilders = new HashSet<>();

            super.visitEnd();
        }
    }

    private static class CodeUnitRecorder extends ForwardingMap<CodeUnitIdentifier, CodeUnit>
            implements TypeAnalysisListener {

        private final Map<CodeUnitIdentifier, CodeUnit> identifierToCodeUnit = new HashMap<>();

        @SuppressWarnings("unchecked")
        CodeUnitRecorder(Class<?> type) {
            for (JavaClass clazzInHierarchy : getAllSuperClasses(type.getName())) {
                put(staticInitializerIdentifier(clazzInHierarchy.getName()), staticInitializerOf(clazzInHierarchy.reflect()));
            }
        }

        @Override
        public CodeUnit get(Object key) {
            return checkNotNull(super.get(key),
                    "No Method for %s was encountered, you probably have ambiguous classes on your classpath", key);
        }

        @Override
        public void onMethodFound(Method method) {
            identifierToCodeUnit.put(CodeUnitIdentifier.of(method), CodeUnit.of(method));
        }

        @Override
        public void onConstructorFound(Constructor<?> constructor) {
            identifierToCodeUnit.put(CodeUnitIdentifier.of(constructor), CodeUnit.of(constructor));
        }

        @Override
        protected Map<CodeUnitIdentifier, CodeUnit> delegate() {
            return identifierToCodeUnit;
        }
    }

    static class CodeUnitIdentifier {
        private final String declaringClassName;
        private final String name;
        private final Type type;

        CodeUnitIdentifier(String declaringClassName, String name, Type type) {
            this.declaringClassName = declaringClassName;
            this.name = name;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(declaringClassName, name, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final CodeUnitIdentifier other = (CodeUnitIdentifier) obj;
            return Objects.equals(this.declaringClassName, other.declaringClassName) &&
                    Objects.equals(this.name, other.name) &&
                    Objects.equals(this.type, other.type);
        }

        @Override
        public String toString() {
            return "{declaringClassName=" + declaringClassName + ", name='" + name + "', type=" + type + '}';
        }

        static CodeUnitIdentifier of(Method method) {
            return new CodeUnitIdentifier(method.getDeclaringClass().getName(), method.getName(), Type.getType(method));
        }

        public static CodeUnitIdentifier of(Constructor<?> constructor) {
            return new CodeUnitIdentifier(constructor.getDeclaringClass().getName(), CONSTRUCTOR_NAME, Type.getType(constructor));
        }

        public static CodeUnitIdentifier of(String name, String desc, String declaringClassName) {
            return new CodeUnitIdentifier(declaringClassName, name, Type.getMethodType(desc));
        }

        static CodeUnitIdentifier staticInitializerIdentifier(String declaringClassName) {
            return of(STATIC_INITIALIZER_NAME, "()V", declaringClassName);
        }
    }

}
