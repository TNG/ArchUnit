package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ForwardingMap;
import com.tngtech.archunit.core.JavaClass.TypeAnalysisListener;
import com.tngtech.archunit.core.RawAccessRecord.CodeUnit;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.ClassFileProcessor.ASM_API_VERSION;
import static com.tngtech.archunit.core.ImportWorkaround.getAllSuperClasses;
import static com.tngtech.archunit.core.JavaClassVisitor.CodeUnitIdentifier.staticInitializerIdentifier;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.JavaStaticInitializer.STATIC_INITIALIZER_NAME;
import static com.tngtech.archunit.core.RawAccessRecord.CodeUnit.staticInitializerOf;

class JavaClassVisitor extends ClassVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(JavaClassVisitor.class);
    private static final AccessHandler NO_OP = new AccessHandler.NoOp();

    private final ProcessingContext processingContext = new ProcessingContext();

    private JavaClass.Builder javaClassBuilder;
    private final AccessHandler accessHandler;

    JavaClassVisitor() {
        this(NO_OP);
    }

    JavaClassVisitor(AccessHandler accessHandler) {
        super(ASM_API_VERSION);
        this.accessHandler = accessHandler;
    }

    Optional<JavaClass> createJavaClass() {
        return javaClassBuilder != null ? Optional.of(javaClassBuilder.build()) : Optional.<JavaClass>absent();
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
        accessHandler.setContext(processingContext.getCodeUnit(name, desc));
        return new MethodProcessor(accessHandler, delegate);
    }

    @Override
    public void visitEnd() {
        if (processingContext.canImportCurrentClass) {
            LOG.debug("Done analysing {}", processingContext.getCurrentClassName());
            javaClassBuilder = processingContext.finish();
        }
        super.visitEnd();
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

        JavaClass.Builder finish() {
            JavaClass.Builder result = currentClassBuilder;
            currentClass = null;
            codeUnitRecorder = null;
            currentClassBuilder = null;
            return result;
        }
    }

    private static class MethodProcessor extends MethodVisitor {
        private final AccessHandler accessHandler;
        private int actualLineNumber;

        MethodProcessor(AccessHandler accessHandler, MethodVisitor mv) {
            super(ASM_API_VERSION, mv);
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
}
