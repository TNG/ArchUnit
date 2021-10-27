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

import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess.JavaTypeFinisher;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeParameterBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaWildcardTypeBuilder;
import org.objectweb.asm.signature.SignatureVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.importer.ClassFileProcessor.ASM_API_VERSION;
import static com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess.JavaTypeFinisher.ARRAY_CREATOR;

class SignatureTypeArgumentProcessor<TYPE extends HasDescription> extends SignatureVisitor {
    private static final Logger log = LoggerFactory.getLogger(SignatureTypeArgumentProcessor.class);

    private final TypeArgumentType typeArgumentType;
    private final JavaParameterizedTypeBuilder<TYPE> parameterizedType;
    private final JavaTypeFinisher typeFinisher;

    private JavaParameterizedTypeBuilder<TYPE> currentTypeArgument;

    SignatureTypeArgumentProcessor(
            TypeArgumentType typeArgumentType,
            JavaParameterizedTypeBuilder<TYPE> parameterizedType,
            JavaTypeFinisher typeFinisher) {
        super(ASM_API_VERSION);
        this.typeArgumentType = typeArgumentType;
        this.parameterizedType = parameterizedType;
        this.typeFinisher = typeFinisher;
    }

    @Override
    public void visitClassType(String internalObjectName) {
        JavaClassDescriptor type = JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName);
        log.trace("Encountered {} for {}: Class type {}", typeArgumentType.description, parameterizedType.getTypeName(), type.getFullyQualifiedClassName());
        currentTypeArgument = new JavaParameterizedTypeBuilder<>(type);
        typeArgumentType.addTypeArgumentToBuilder(parameterizedType, new NewJavaTypeCreationProcess<>(this.currentTypeArgument, typeFinisher));
    }

    @Override
    public void visitBaseType(char descriptor) {
        visitClassType(String.valueOf(descriptor));
    }

    @Override
    public void visitTypeArgument() {
        log.trace("Encountered wildcard for {}", currentTypeArgument.getTypeName());
        currentTypeArgument.addTypeArgument(new NewJavaTypeCreationProcess<>(new JavaWildcardTypeBuilder<TYPE>(), JavaTypeFinisher.IDENTITY));
    }

    @Override
    public void visitTypeVariable(String name) {
        if (log.isTraceEnabled()) {
            log.trace("Encountered {} for {}: Type variable {}", typeArgumentType.description, parameterizedType.getTypeName(), typeFinisher.getFinishedName(name));
        }
        typeArgumentType.addTypeArgumentToBuilder(parameterizedType, new ReferenceCreationProcess<TYPE>(name, typeFinisher));
    }

    @Override
    public SignatureVisitor visitTypeArgument(char wildcard) {
        return SignatureTypeArgumentProcessor.create(wildcard, currentTypeArgument, JavaTypeFinisher.IDENTITY);
    }

    @Override
    public SignatureVisitor visitArrayType() {
        return new SignatureTypeArgumentProcessor<>(typeArgumentType, parameterizedType, typeFinisher.after(ARRAY_CREATOR));
    }

    static <TYPE extends HasDescription> SignatureTypeArgumentProcessor<TYPE> create(char identifier, JavaParameterizedTypeBuilder<TYPE> parameterizedType) {
        return create(identifier, parameterizedType, JavaTypeFinisher.IDENTITY);
    }

    static <TYPE extends HasDescription> SignatureTypeArgumentProcessor<TYPE> create(
            char identifier,
            JavaParameterizedTypeBuilder<TYPE> parameterizedType,
            JavaTypeFinisher typeFinisher) {

        switch (identifier) {
            case INSTANCEOF:
                return new SignatureTypeArgumentProcessor<>(PARAMETERIZED_TYPE, parameterizedType, typeFinisher);
            case EXTENDS:
                return new SignatureTypeArgumentProcessor<>(WILDCARD_WITH_UPPER_BOUND, parameterizedType, typeFinisher);
            case SUPER:
                return new SignatureTypeArgumentProcessor<>(WILDCARD_WITH_LOWER_BOUND, parameterizedType, typeFinisher);
            default:
                throw new IllegalStateException(String.format("Cannot handle asm type argument identifier '%s'", identifier));
        }
    }

    private abstract static class TypeArgumentType {
        private final String description;

        TypeArgumentType(String description) {
            this.description = description;
        }

        abstract <TYPE extends HasDescription> void addTypeArgumentToBuilder(
                JavaParameterizedTypeBuilder<TYPE> parameterizedType, JavaTypeCreationProcess<TYPE> creationProcess);
    }

    private static final TypeArgumentType PARAMETERIZED_TYPE = new TypeArgumentType("type argument") {
        @Override
        <T extends HasDescription> void addTypeArgumentToBuilder(
                JavaParameterizedTypeBuilder<T> parameterizedType, JavaTypeCreationProcess<T> typeCreationProcess) {

            parameterizedType.addTypeArgument(typeCreationProcess);
        }
    };

    private static final TypeArgumentType WILDCARD_WITH_UPPER_BOUND = new TypeArgumentType("wildcard with upper bound") {
        @Override
        <T extends HasDescription> void addTypeArgumentToBuilder(
                JavaParameterizedTypeBuilder<T> parameterizedType, JavaTypeCreationProcess<T> typeCreationProcess) {

            parameterizedType.addTypeArgument(new NewJavaTypeCreationProcess<>(new JavaWildcardTypeBuilder<T>().addUpperBound(typeCreationProcess)));
        }
    };

    private static final TypeArgumentType WILDCARD_WITH_LOWER_BOUND = new TypeArgumentType("wildcard with lower bound") {
        @Override
        <T extends HasDescription> void addTypeArgumentToBuilder(
                JavaParameterizedTypeBuilder<T> parameterizedType, JavaTypeCreationProcess<T> typeCreationProcess) {

            parameterizedType.addTypeArgument(new NewJavaTypeCreationProcess<>(new JavaWildcardTypeBuilder<T>().addLowerBound(typeCreationProcess)));
        }
    };

    static class NewJavaTypeCreationProcess<OWNER extends HasDescription> implements JavaTypeCreationProcess<OWNER> {
        private final JavaTypeBuilder<OWNER> builder;
        private final JavaTypeFinisher typeFinisher;

        NewJavaTypeCreationProcess(JavaTypeBuilder<OWNER> builder) {
            this(builder, JavaTypeFinisher.IDENTITY);
        }

        NewJavaTypeCreationProcess(JavaTypeBuilder<OWNER> builder, JavaTypeFinisher typeFinisher) {
            this.builder = builder;
            this.typeFinisher = typeFinisher;
        }

        @Override
        public JavaType finish(OWNER owner, Iterable<JavaTypeVariable<?>> allTypeParametersInContext, ImportedClasses classes) {
            JavaType type = builder.build(owner, allTypeParametersInContext, classes);
            return typeFinisher.finish(type, classes);
        }
    }

    static class ReferenceCreationProcess<OWNER extends HasDescription> implements JavaTypeCreationProcess<OWNER> {
        private final String typeVariableName;
        private final JavaTypeFinisher finisher;

        ReferenceCreationProcess(String typeVariableName) {
            this(typeVariableName, JavaTypeFinisher.IDENTITY);
        }

        ReferenceCreationProcess(String typeVariableName, JavaTypeFinisher finisher) {
            this.typeVariableName = typeVariableName;
            this.finisher = finisher;
        }

        @Override
        public JavaType finish(OWNER owner, Iterable<JavaTypeVariable<?>> allTypeParametersInContext, ImportedClasses classes) {
            return finisher.finish(createTypeVariable(owner, allTypeParametersInContext, classes), classes);
        }

        private JavaType createTypeVariable(OWNER owner, Iterable<JavaTypeVariable<?>> allTypeParametersInContext, ImportedClasses classes) {
            for (JavaTypeVariable<?> existingTypeVariable : allTypeParametersInContext) {
                if (existingTypeVariable.getName().equals(typeVariableName)) {
                    return existingTypeVariable;
                }
            }
            // type variables can be missing from the import context -> create a simple unbound type variable since we have no more information
            return new JavaTypeParameterBuilder<>(typeVariableName).build(owner, classes);
        }
    }
}
