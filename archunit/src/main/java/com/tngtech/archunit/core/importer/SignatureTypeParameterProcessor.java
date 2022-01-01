/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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

import java.util.ArrayList;
import java.util.List;

import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeParameterBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaWildcardTypeBuilder;
import com.tngtech.archunit.core.importer.SignatureTypeArgumentProcessor.NewJavaTypeCreationProcess;
import com.tngtech.archunit.core.importer.SignatureTypeArgumentProcessor.ReferenceCreationProcess;
import org.objectweb.asm.signature.SignatureVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.importer.ClassFileProcessor.ASM_API_VERSION;

class SignatureTypeParameterProcessor<OWNER extends HasDescription> extends SignatureVisitor {
    private static final Logger log = LoggerFactory.getLogger(SignatureTypeParameterProcessor.class);

    private final List<JavaTypeParameterBuilder<OWNER>> typeParameterBuilders = new ArrayList<>();

    private JavaTypeParameterBuilder<OWNER> currentType;
    private JavaParameterizedTypeBuilder<OWNER> currentBound;

    SignatureTypeParameterProcessor() {
        super(ASM_API_VERSION);
    }

    List<JavaTypeParameterBuilder<OWNER>> getTypeParameterBuilders() {
        return typeParameterBuilders;
    }

    void addTypeParameter(String typeName) {
        currentType = new JavaTypeParameterBuilder<>(typeName);
        currentBound = null;
        typeParameterBuilders.add(currentType);
    }

    @Override
    public void visitClassType(String internalObjectName) {
        JavaClassDescriptor type = JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName);
        log.trace("Encountered upper bound for {}: Class type {}", currentType.getName(), type.getFullyQualifiedClassName());
        currentBound = new JavaParameterizedTypeBuilder<>(type);
    }

    @Override
    public void visitTypeArgument() {
        log.trace("Encountered wildcard for {}", currentBound.getTypeName());
        currentBound.addTypeArgument(new NewJavaTypeCreationProcess<>(new JavaWildcardTypeBuilder<OWNER>()));
    }

    @Override
    public void visitTypeVariable(String name) {
        log.trace("Encountered upper bound for {}: Type variable {}", currentType.getName(), name);
        currentType.addBound(new ReferenceCreationProcess<OWNER>(name));
    }

    @Override
    public SignatureVisitor visitTypeArgument(char wildcard) {
        return SignatureTypeArgumentProcessor.create(wildcard, currentBound);
    }

    @Override
    public void visitInnerClassType(String name) {
        currentBound = currentBound.forInnerClass(name);
    }

    @Override
    public void visitEnd() {
        currentType.addBound(new NewJavaTypeCreationProcess<>(currentBound));
    }
}
