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

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess.JavaTypeFinisher;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaWildcardTypeBuilder;
import com.tngtech.archunit.core.importer.SignatureTypeArgumentProcessor.NewJavaTypeCreationProcess;
import com.tngtech.archunit.core.importer.SignatureTypeArgumentProcessor.ReferenceCreationProcess;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.importer.ClassFileProcessor.ASM_API_VERSION;
import static com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess.JavaTypeFinisher.ARRAY_CREATOR;

class JavaFieldTypeSignatureImporter {
    private static final Logger log = LoggerFactory.getLogger(JavaFieldTypeSignatureImporter.class);

    static Optional<JavaTypeCreationProcess<JavaField>> parseAsmFieldTypeSignature(String signature) {
        if (signature == null) {
            return Optional.absent();
        }

        log.trace("Analyzing field signature: {}", signature);

        SignatureProcessor signatureProcessor = new SignatureProcessor();
        new SignatureReader(signature).accept(signatureProcessor);
        return Optional.of(signatureProcessor.getFieldType());
    }

    private static class SignatureProcessor extends SignatureVisitor {
        private final GenericFieldTypeProcessor genericFieldTypeProcessor = new GenericFieldTypeProcessor();

        SignatureProcessor() {
            super(ASM_API_VERSION);
        }

        @Override
        public SignatureVisitor visitSuperclass() {
            return genericFieldTypeProcessor;
        }

        JavaTypeCreationProcess<JavaField> getFieldType() {
            return genericFieldTypeProcessor.getFieldType();
        }
    }

    private static class GenericFieldTypeProcessor extends SignatureVisitor {
        private JavaParameterizedTypeBuilder<JavaField> parameterizedFieldType;
        private JavaTypeCreationProcess<JavaField> fieldTypeCreationProcess;
        private JavaTypeFinisher typeFinisher = JavaTypeFinisher.IDENTITY;

        GenericFieldTypeProcessor() {
            super(ASM_API_VERSION);
        }

        JavaTypeCreationProcess<JavaField> getFieldType() {
            return fieldTypeCreationProcess;
        }

        @Override
        public void visitClassType(String internalObjectName) {
            updateFieldType(new JavaParameterizedTypeBuilder<JavaField>(JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName)));
        }

        @Override
        public void visitInnerClassType(String name) {
            updateFieldType(parameterizedFieldType.forInnerClass(name));
        }

        @Override
        public void visitTypeArgument() {
            parameterizedFieldType.addTypeArgument(new NewJavaTypeCreationProcess<>(new JavaWildcardTypeBuilder<JavaField>()));
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            return SignatureTypeArgumentProcessor.create(wildcard, parameterizedFieldType, JavaTypeFinisher.IDENTITY);
        }

        @Override
        public SignatureVisitor visitArrayType() {
            typeFinisher = typeFinisher.after(ARRAY_CREATOR);
            return this;
        }

        @Override
        public void visitTypeVariable(String name) {
            fieldTypeCreationProcess = new ReferenceCreationProcess<>(name);
        }

        private void updateFieldType(JavaParameterizedTypeBuilder<JavaField> fieldType) {
            this.parameterizedFieldType = fieldType;
            fieldTypeCreationProcess = new NewJavaTypeCreationProcess<>(this.parameterizedFieldType, typeFinisher);
        }
    }
}
