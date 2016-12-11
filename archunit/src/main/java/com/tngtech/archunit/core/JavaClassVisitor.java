package com.tngtech.archunit.core;

import com.tngtech.archunit.core.RawAccessRecord.CodeUnit;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.ClassFileProcessor.ASM_API_VERSION;

class JavaClassVisitor extends ClassVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(JavaClassVisitor.class);
    private static final AccessHandler NO_OP = new AccessHandler.NoOp();

    private JavaClass.Builder javaClassBuilder;
    private final AccessHandler accessHandler;
    private boolean canImportCurrentClass;
    private String className;

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
        javaClassBuilder = init(name);
        this.className = name.replace("/", ".");
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

    JavaClass.Builder init(String classDescriptor) {
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
}
