/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.core.importer;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassTypeParametersBuilder;
import com.tngtech.archunit.core.importer.JavaClassProcessor.AccessHandler;
import com.tngtech.archunit.core.importer.JavaClassProcessor.DeclarationHandler;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.importer.RawAccessRecord.TargetInfo;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver.ClassUriImporter;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static org.objectweb.asm.Opcodes.ASM9;

class ClassFileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ClassFileProcessor.class);

    static final int ASM_API_VERSION = ASM9;

    private final boolean md5InClassSourcesEnabled = ArchConfiguration.get().md5InClassSourcesEnabled();
    private final ClassResolver.Factory classResolverFactory = new ClassResolver.Factory();

    JavaClasses process(ClassFileSource source) {
        ClassFileImportRecord importRecord = new ClassFileImportRecord();
        RecordAccessHandler accessHandler = new RecordAccessHandler(importRecord);
        ClassDetailsRecorder classDetailsRecorder = new ClassDetailsRecorder(importRecord);
        for (ClassFileLocation location : source) {
            try (InputStream s = location.openStream()) {
                JavaClassProcessor javaClassProcessor =
                        new JavaClassProcessor(new SourceDescriptor(location.getUri(), md5InClassSourcesEnabled), classDetailsRecorder, accessHandler);
                new ClassReader(s).accept(javaClassProcessor, 0);
                importRecord.addAll(javaClassProcessor.createJavaClass().asSet());
            } catch (Exception e) {
                LOG.warn(String.format("Couldn't import class from %s", location.getUri()), e);
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
        public void onNewClass(String className, Optional<String> superclassName, List<String> interfaceNames) {
            ownerName = className;
            if (superclassName.isPresent()) {
                importRecord.setSuperclass(ownerName, superclassName.get());
            }
            importRecord.addInterfaces(ownerName, interfaceNames);
        }

        @Override
        public void onDeclaredTypeParameters(JavaClassTypeParametersBuilder typeParametersBuilder) {
            importRecord.addTypeParameters(ownerName, typeParametersBuilder);
        }

        @Override
        public void onGenericSuperclass(DomainBuilders.JavaParameterizedTypeBuilder<JavaClass> genericSuperclassBuilder) {
            importRecord.addGenericSuperclass(ownerName, genericSuperclassBuilder);
        }

        @Override
        public void onGenericInterfaces(List<DomainBuilders.JavaParameterizedTypeBuilder<JavaClass>> genericInterfaceBuilders) {
            importRecord.addGenericInterfaces(ownerName, genericInterfaceBuilders);
        }

        @Override
        public void onDeclaredField(DomainBuilders.JavaFieldBuilder fieldBuilder) {
            importRecord.addField(ownerName, fieldBuilder);
        }

        @Override
        public void onDeclaredConstructor(DomainBuilders.JavaConstructorBuilder constructorBuilder) {
            importRecord.addConstructor(ownerName, constructorBuilder);
        }

        @Override
        public void onDeclaredMethod(DomainBuilders.JavaMethodBuilder methodBuilder) {
            importRecord.addMethod(ownerName, methodBuilder);
        }

        @Override
        public void onDeclaredStaticInitializer(DomainBuilders.JavaStaticInitializerBuilder staticInitializerBuilder) {
            importRecord.setStaticInitializer(ownerName, staticInitializerBuilder);
        }

        @Override
        public void onDeclaredClassAnnotations(Set<DomainBuilders.JavaAnnotationBuilder> annotationBuilders) {
            importRecord.addClassAnnotations(ownerName, annotationBuilders);
        }

        @Override
        public void onDeclaredMemberAnnotations(String memberName, String descriptor, Set<DomainBuilders.JavaAnnotationBuilder> annotations) {
            importRecord.addMemberAnnotations(ownerName, memberName, descriptor, annotations);
        }

        @Override
        public void onDeclaredAnnotationDefaultValue(String methodName, String methodDescriptor, DomainBuilders.JavaAnnotationBuilder.ValueBuilder valueBuilder) {
            importRecord.addAnnotationDefaultValue(ownerName, methodName, methodDescriptor, valueBuilder);
        }

        @Override
        public void registerEnclosingClass(String ownerName, String enclosingClassName) {
            importRecord.setEnclosingClass(ownerName, enclosingClassName);
        }

        @Override
        public void registerEnclosingCodeUnit(String ownerName, CodeUnit enclosingCodeUnit) {
            importRecord.setEnclosingCodeUnit(ownerName, enclosingCodeUnit);
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
            LOG.trace("Found {} access to field {}.{}:{} in line {}", accessType, owner, name, desc, lineNumber);
            TargetInfo target = new TargetInfo(owner, name, desc);
            importRecord.registerFieldAccess(filled(new RawAccessRecord.ForField.Builder(), target)
                    .withAccessType(accessType)
                    .build());
        }

        @Override
        public void handleMethodInstruction(String owner, String name, String desc) {
            LOG.trace("Found call of method {}.{}:{} in line {}", owner, name, desc, lineNumber);
            if (CONSTRUCTOR_NAME.equals(name)) {
                TargetInfo target = new TargetInfo(owner, name, desc);
                importRecord.registerConstructorCall(filled(new RawAccessRecord.Builder(), target).build());
            } else {
                TargetInfo target = new TargetInfo(owner, name, desc);
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

    private ClassResolver getClassResolver(ClassDetailsRecorder classDetailsRecorder) {
        ClassResolver classResolver = classResolverFactory.create();
        classResolver.setClassUriImporter(new UriImporterOfProcessor(classDetailsRecorder, md5InClassSourcesEnabled));
        return classResolver;
    }

    private static class UriImporterOfProcessor implements ClassUriImporter {
        private final DeclarationHandler declarationHandler;
        private final boolean md5InClassSourcesEnabled;

        UriImporterOfProcessor(DeclarationHandler declarationHandler, boolean md5InClassSourcesEnabled) {
            this.declarationHandler = declarationHandler;
            this.md5InClassSourcesEnabled = md5InClassSourcesEnabled;
        }

        @Override
        public Optional<JavaClass> tryImport(URI uri) {
            try (InputStream inputStream = uri.toURL().openStream()) {
                JavaClassProcessor classProcessor = new JavaClassProcessor(new SourceDescriptor(uri, md5InClassSourcesEnabled), declarationHandler);
                new ClassReader(inputStream).accept(classProcessor, 0);
                return classProcessor.createJavaClass();
            } catch (Exception e) {
                LOG.warn(String.format("Error during import from %s, falling back to simple import", uri), e);
                return Optional.empty();
            }
        }
    }

}
