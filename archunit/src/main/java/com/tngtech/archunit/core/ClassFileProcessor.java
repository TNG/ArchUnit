package com.tngtech.archunit.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.ArchUnitException.ReflectionException;
import com.tngtech.archunit.core.JavaClassProcessor.AccessHandler;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.RawAccessRecord.MethodTargetInfo;
import com.tngtech.archunit.core.RawAccessRecord.TargetInfo;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.ReflectionUtils.classForName;
import static com.tngtech.archunit.core.ReflectionUtils.getAllSuperTypes;
import static org.objectweb.asm.Opcodes.ASM5;

class ClassFileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ClassFileProcessor.class);

    static final int ASM_API_VERSION = ASM5;

    JavaClasses process(ClassFileSource source) {
        ClassFileImportRecord importRecord = new ClassFileImportRecord();
        RecordAccessHandler accessHandler = new RecordAccessHandler(importRecord);
        for (Supplier<InputStream> stream : source) {
            try (InputStream s = stream.get()) {
                JavaClassProcessor javaClassProcessor = new JavaClassProcessor(accessHandler);
                new ClassReader(s).accept(javaClassProcessor, 0);
                importRecord.addAll(javaClassProcessor.createJavaClass().asSet());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new ClassGraphCreator(importRecord, getClassResolver()).complete();
    }

    private static class RecordAccessHandler implements AccessHandler {
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

    private ClassResolver getClassResolver() {
        return new ClassResolverFromClassPath();
    }

    static class ClassResolverFromClassPath implements ClassResolver {
        @Override
        public JavaClass resolve(String typeName) {
            Class<?> type = classForName(typeName);
            String typeFile = "/" + type.getName().replace(".", "/") + ".class";

            try (InputStream inputStream = type.getResourceAsStream(typeFile)) {
                JavaClassProcessor classProcessor = new JavaClassProcessor();
                new ClassReader(inputStream).accept(classProcessor, 0);
                return classProcessor.createJavaClass().get();
            } catch (IOException e) {
                return new JavaClass.Builder().withType(TypeDetails.of(type)).build();
            }
        }

        @Override
        public Set<JavaClass> getAllSuperClasses(String className) {
            try {
                return tryGetAllSuperClasses(className);
            } catch (NoClassDefFoundError e) {
                LOG.warn("Can't analyse related type of '{}' because of missing dependency '{}'",
                        className, e.getMessage());
            } catch (ReflectionException e) {
                LOG.warn("Can't analyse related type of '{}' because of missing dependency. Error was: '{}'",
                        className, e.getMessage());
            }
            return Collections.emptySet();
        }

        private Set<JavaClass> tryGetAllSuperClasses(String typeName) {
            ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
            for (Class<?> type : getAllSuperTypes(classForName(typeName))) {
                result.add(resolve(type.getName()));
            }
            return result.build();
        }
    }
}
