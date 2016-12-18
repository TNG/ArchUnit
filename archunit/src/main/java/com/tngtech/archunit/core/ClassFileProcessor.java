package com.tngtech.archunit.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
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
import static com.tngtech.archunit.core.ReflectionUtils.getAllSuperTypes;
import static com.tngtech.archunit.core.ReflectionUtils.tryGetClassForName;
import static org.objectweb.asm.Opcodes.ASM5;

class ClassFileProcessor {
    static final int ASM_API_VERSION = ASM5;

    JavaClasses process(ClassFileSource source) {
        ClassFileImportRecord importRecord = new ClassFileImportRecord();
        RecordAccessHandler accessHandler = new RecordAccessHandler(importRecord);
        MembersRecorder membersRecorder = new MembersRecorder(importRecord);
        for (Supplier<InputStream> stream : source) {
            try (InputStream s = stream.get()) {
                JavaClassProcessor javaClassProcessor = new JavaClassProcessor(membersRecorder, accessHandler);
                new ClassReader(s).accept(javaClassProcessor, 0);
                importRecord.addAll(javaClassProcessor.createJavaClass().asSet());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new ClassGraphCreator(importRecord, getClassResolver(membersRecorder)).complete();
    }

    private static class MembersRecorder implements DeclarationHandler {
        private final ClassFileImportRecord importRecord;
        private String ownerName;

        private MembersRecorder(ClassFileImportRecord importRecord) {
            this.importRecord = importRecord;
        }

        @Override
        public void onNewClass(String className) {
            ownerName = className;
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

    private ClassResolverFromClassPath getClassResolver(MembersRecorder membersRecorder) {
        return new ClassResolverFromClassPath(membersRecorder);
    }

    static class ClassResolverFromClassPath implements ClassResolver {
        private final DeclarationHandler declarationHandler;

        ClassResolverFromClassPath(DeclarationHandler declarationHandler) {
            this.declarationHandler = declarationHandler;
        }

        @Override
        public JavaClass resolve(String typeName) {
            Optional<Class<?>> type = tryGetClassForName(typeName);
            return type.isPresent() ?
                    resolve(type.get()) :
                    new JavaClass.Builder().withType(TypeDetails.of(typeName)).build();
        }

        private JavaClass resolve(Class<?> type) {
            String typeFile = "/" + type.getName().replace(".", "/") + ".class";

            try (InputStream inputStream = type.getResourceAsStream(typeFile)) {
                JavaClassProcessor classProcessor = new JavaClassProcessor(declarationHandler);
                new ClassReader(inputStream).accept(classProcessor, 0);
                return classProcessor.createJavaClass().get();
            } catch (IOException e) {
                return new JavaClass.Builder().withType(TypeDetails.of(type)).build();
            }
        }

        @Override
        public Set<JavaClass> getAllSuperClasses(String className, Map<String, JavaClass> importedClasses) {
            Optional<Class<?>> type = tryGetClassForName(className);
            return type.isPresent() ?
                    tryGetAllSuperClasses(type.get(), importedClasses) :
                    Collections.<JavaClass>emptySet();
        }

        private Set<JavaClass> tryGetAllSuperClasses(Class<?> type, Map<String, JavaClass> importedClasses) {
            ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
            for (Class<?> superClass : getAllSuperTypes(type)) {
                result.add(importedClasses.containsKey(superClass.getName()) ?
                        importedClasses.get(superClass.getName()) :
                        resolve(superClass));
            }
            return result.build();
        }
    }
}
