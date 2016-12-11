package com.tngtech.archunit.core;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.base.Supplier;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.RawAccessRecord.MethodTargetInfo;
import com.tngtech.archunit.core.RawAccessRecord.TargetInfo;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static org.objectweb.asm.Opcodes.ASM5;

class ClassFileProcessor {
    static final int ASM_API_VERSION = ASM5;

    private final ClassFileImportRecord importRecord = new ClassFileImportRecord();

    JavaClasses process(ClassFileSource source) {
        ClassFileImportRecord importRecord = new ClassFileImportRecord();
        RecordAccessHandler accessHandler = new RecordAccessHandler(importRecord);
        for (Supplier<InputStream> stream : source) {
            try (InputStream s = stream.get()) {
                JavaClassVisitor javaClassVisitor = new JavaClassVisitor(accessHandler);
                new ClassReader(s).accept(javaClassVisitor, 0);
                Optional<JavaClass> javaClass = javaClassVisitor.createJavaClass();
                if (javaClass.isPresent()) {
                    importRecord.add(javaClass.get());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new ClassGraphCreator(importRecord).complete();
    }

    private static class RecordAccessHandler implements JavaClassVisitor.AccessHandler {
        private static final Logger LOG = LoggerFactory.getLogger(RecordAccessHandler.class);

        private final ClassFileImportRecord importRecord;
        private CodeUnit codeUnit;
        private int lineNumber;

        private RecordAccessHandler(ClassFileImportRecord importRecord) {
            this.importRecord = importRecord;
        }

        @Override
        public void setContext(CodeUnit codeUnit) {
            this.codeUnit = codeUnit;
        }

        @Override
        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        @Override
        public void handleFieldInstruction(int opcode, String owner, String name, String desc) {
            AccessType accessType = AccessType.forOpCode(opcode);
            LOG.debug("Found {} access to field {}.{}:{} in line {}", accessType, owner, name, desc, lineNumber);
            TargetInfo target = new RawAccessRecord.FieldTargetInfo(owner, name, desc);
            importRecord.registerFieldAccess(filled(new RawAccessRecord.ForField.Builder(), target)
                    .withAccessType(accessType)
                    .build());
        }

        @Override
        public void handleMethodInstruction(int opcode, String owner, String name, String desc) {
            LOG.debug("Found call of method {}.{}:{} in line {}", owner, name, desc, lineNumber);
            if (CONSTRUCTOR_NAME.equals(name)) {
                TargetInfo target = new RawAccessRecord.ConstructorTargetInfo(owner, name, desc);
                importRecord.registerConstructorCall(filled(new RawAccessRecord.Builder(), target).build());
            } else {
                TargetInfo target = new MethodTargetInfo(owner, name, desc);
                importRecord.registerMethodCall(filled(new RawAccessRecord.Builder(), target).build());
            }
        }

        private <BUILDER extends RawAccessRecord.BaseBuilder<BUILDER>> BUILDER filled(BUILDER builder, TargetInfo target) {
            return builder
                    .withCaller(codeUnit)
                    .withTarget(target)
                    .withLineNumber(lineNumber);
        }
    }
}
