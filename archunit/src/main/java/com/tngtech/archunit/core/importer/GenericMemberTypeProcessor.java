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
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess.JavaTypeFinisher;
import com.tngtech.archunit.core.importer.SignatureTypeArgumentProcessor.NewJavaTypeCreationProcess;
import com.tngtech.archunit.core.importer.SignatureTypeArgumentProcessor.ReferenceCreationProcess;
import org.objectweb.asm.signature.SignatureVisitor;

import static com.tngtech.archunit.core.importer.ClassFileProcessor.ASM_API_VERSION;
import static com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess.JavaTypeFinisher.ARRAY_CREATOR;

class GenericMemberTypeProcessor<T extends HasDescription> extends SignatureVisitor {
    private JavaParameterizedTypeBuilder<T> parameterizedType;
    private JavaTypeCreationProcess<T> typeCreationProcess;
    private JavaTypeFinisher typeFinisher = JavaTypeFinisher.IDENTITY;

    GenericMemberTypeProcessor() {
        super(ASM_API_VERSION);
    }

    Optional<JavaTypeCreationProcess<T>> getType() {
        return Optional.ofNullable(typeCreationProcess);
    }

    @Override
    public void visitClassType(String internalObjectName) {
        updateType(new JavaParameterizedTypeBuilder<T>(JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName)));
    }

    @Override
    public void visitBaseType(char descriptor) {
        visitClassType(String.valueOf(descriptor));
    }

    @Override
    public void visitInnerClassType(String name) {
        updateType(parameterizedType.forInnerClass(name));
    }

    @Override
    public void visitTypeArgument() {
        parameterizedType.addTypeArgument(new NewJavaTypeCreationProcess<>(new DomainBuilders.JavaWildcardTypeBuilder<T>()));
    }

    @Override
    public SignatureVisitor visitTypeArgument(char wildcard) {
        return SignatureTypeArgumentProcessor.create(wildcard, parameterizedType, JavaTypeFinisher.IDENTITY);
    }

    @Override
    public SignatureVisitor visitArrayType() {
        typeFinisher = typeFinisher.after(ARRAY_CREATOR);
        return this;
    }

    @Override
    public void visitTypeVariable(String name) {
        typeCreationProcess = new ReferenceCreationProcess<>(name, typeFinisher);
    }

    private void updateType(JavaParameterizedTypeBuilder<T> type) {
        this.parameterizedType = type;
        typeCreationProcess = new NewJavaTypeCreationProcess<>(this.parameterizedType, typeFinisher);
    }
}
