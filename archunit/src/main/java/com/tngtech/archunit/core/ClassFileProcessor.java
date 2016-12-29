package com.tngtech.archunit.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import com.google.common.base.Supplier;
import com.tngtech.archunit.core.JavaClassProcessor.AccessHandler;
import com.tngtech.archunit.core.JavaClassProcessor.DeclarationHandler;
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

    JavaClasses process(ClassFileSource source) {
        ClassFileImportRecord importRecord = new ClassFileImportRecord();
        RecordAccessHandler accessHandler = new RecordAccessHandler(importRecord);
        ClassDetailsRecorder classDetailsRecorder = new ClassDetailsRecorder(importRecord);
        for (Supplier<InputStream> stream : source) {
            try (InputStream s = stream.get()) {
                JavaClassProcessor javaClassProcessor = new JavaClassProcessor(classDetailsRecorder, accessHandler);
                new ClassReader(s).accept(javaClassProcessor, 0);
                importRecord.addAll(javaClassProcessor.createJavaClass().asSet());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new ClassGraphCreator(importRecord, getClassResolver(classDetailsRecorder)).complete();
    }

    private static class ClassDetailsRecorder implements DeclarationHandler {
        private final ClassFileImportRecord importRecord;
        private String ownerName;

        private ClassDetailsRecorder(ClassFileImportRecord importRecord) {
            this.importRecord = importRecord;
        }

        @Override
        public boolean isNew(String className) {
            return !importRecord.getClasses().containsKey(className);
        }

        @Override
        public void onNewClass(String className, Optional<String> superClassName, Set<String> interfaceNames) {
            ownerName = className;
            if (superClassName.isPresent()) {
                importRecord.setSuperClass(ownerName, superClassName.get());
            }
            importRecord.addInterfaces(ownerName, interfaceNames);
        }

        @Override
        public void onDeclaredField(JavaField.Builder fieldBuilder) {
            importRecord.addField(ownerName, fieldBuilder);
        }

        @Override
        public void onDeclaredConstructor(JavaConstructor.Builder builder) {
            importRecord.addConstructor(ownerName, builder);
        }

        @Override
        public void onDeclaredMethod(JavaMethod.Builder builder) {
            importRecord.addMethod(ownerName, builder);
        }

        @Override
        public void onDeclaredStaticInitializer(JavaStaticInitializer.Builder builder) {
            importRecord.setStaticInitializer(ownerName, builder);
        }

        @Override
        public void onDeclaredAnnotations(Set<JavaAnnotation.Builder> annotations) {
            importRecord.addAnnotations(ownerName, annotations);
        }

        @Override
        public void registerEnclosingClass(String ownerName, String enclosingClassName) {
            importRecord.setEnclosingClass(ownerName, enclosingClassName);
        }
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

    private ClassResolverFromClassPath getClassResolver(ClassDetailsRecorder classDetailsRecorder) {
        return new ClassResolverFromClassPath(classDetailsRecorder);
    }

    @MayResolveTypesViaReflection(reason = "This is a dedicated option to resolve further dependencies from the classpath")
    static class ClassResolverFromClassPath implements ClassResolver {
        private final DeclarationHandler declarationHandler;

        ClassResolverFromClassPath(DeclarationHandler declarationHandler) {
            this.declarationHandler = declarationHandler;
        }

        @Override
        public Optional<JavaClass> tryResolve(String typeName, ImportedClasses.ByTypeName importedClasses) {
            if (importedClasses.contain(typeName)) {
                return Optional.of(importedClasses.get(typeName));
            }

            return tryResolve(typeName);
        }

        private Optional<JavaClass> tryResolve(String typeName) {
            String typeFile = "/" + typeName.replace(".", "/") + ".class";

            try (InputStream inputStream = getClass().getResourceAsStream(typeFile)) {
                JavaClassProcessor classProcessor = new JavaClassProcessor(declarationHandler);
                new ClassReader(inputStream).accept(classProcessor, 0);
                return classProcessor.createJavaClass();
            } catch (Exception e) {
                return Optional.absent();
            }
        }
    }
}
