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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeParameterBuilder;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.importer.ClassFileProcessor.ASM_API_VERSION;

class JavaCodeUnitSignatureImporter {
    private static final Logger log = LoggerFactory.getLogger(JavaCodeUnitSignatureImporter.class);

    public static JavaCodeUnitSignature parseAsmMethodSignature(String signature) {
        if (signature == null) {
            return JavaCodeUnitSignature.ABSENT;
        }

        log.trace("Analyzing method signature: {}", signature);

        SignatureProcessor signatureProcessor = new SignatureProcessor();
        new SignatureReader(signature).accept(signatureProcessor);
        return signatureProcessor.getParsedSignature();
    }

    private static class SignatureProcessor extends SignatureVisitor {
        private final SignatureTypeParameterProcessor<JavaCodeUnit> typeParameterProcessor = new SignatureTypeParameterProcessor<>();
        private final List<GenericMemberTypeProcessor<JavaCodeUnit>> genericMethodParameterTypeProcessors = new ArrayList<>();
        private final GenericMemberTypeProcessor<JavaCodeUnit> genericMethodReturnTypeProcessor = new GenericMemberTypeProcessor<>();

        SignatureProcessor() {
            super(ASM_API_VERSION);
        }

        @Override
        public void visitFormalTypeParameter(String name) {
            log.trace("Encountered type parameter {}", name);
            typeParameterProcessor.addTypeParameter(name);
        }

        @Override
        public SignatureVisitor visitClassBound() {
            return typeParameterProcessor;
        }

        @Override
        public SignatureVisitor visitInterfaceBound() {
            return typeParameterProcessor;
        }

        @Override
        public SignatureVisitor visitParameterType() {
            GenericMemberTypeProcessor<JavaCodeUnit> parameterTypeProcessor = new GenericMemberTypeProcessor<>();
            genericMethodParameterTypeProcessors.add(parameterTypeProcessor);
            return parameterTypeProcessor;
        }

        @Override
        public SignatureVisitor visitReturnType() {
            return genericMethodReturnTypeProcessor;
        }

        public JavaCodeUnitSignature getParsedSignature() {
            List<JavaTypeCreationProcess<JavaCodeUnit>> parameterTypes = new ArrayList<>();
            for (GenericMemberTypeProcessor<JavaCodeUnit> parameterTypeProcessor : genericMethodParameterTypeProcessors) {
                if (parameterTypeProcessor.getType().isPresent()) {
                    parameterTypes.add(parameterTypeProcessor.getType().get());
                }
            }

            return new JavaCodeUnitSignature(
                    typeParameterProcessor.getTypeParameterBuilders(),
                    parameterTypes,
                    genericMethodReturnTypeProcessor.getType());
        }
    }

    static class JavaCodeUnitSignature {
        static final JavaCodeUnitSignature ABSENT = new JavaCodeUnitSignature(
                Collections.<JavaTypeParameterBuilder<JavaCodeUnit>>emptyList(),
                Collections.<JavaTypeCreationProcess<JavaCodeUnit>>emptyList(),
                Optional.<JavaTypeCreationProcess<JavaCodeUnit>>empty()
        );

        private final List<JavaTypeParameterBuilder<JavaCodeUnit>> typeParameterBuilders;
        private final List<JavaTypeCreationProcess<JavaCodeUnit>> parameterTypes;
        private final Optional<JavaTypeCreationProcess<JavaCodeUnit>> returnType;

        private JavaCodeUnitSignature(
                List<JavaTypeParameterBuilder<JavaCodeUnit>> typeParameterBuilders,
                List<JavaTypeCreationProcess<JavaCodeUnit>> parameterTypes,
                Optional<JavaTypeCreationProcess<JavaCodeUnit>> returnType
        ) {
            this.typeParameterBuilders = checkNotNull(typeParameterBuilders);
            this.parameterTypes = parameterTypes;
            this.returnType = checkNotNull(returnType);
        }

        List<JavaTypeParameterBuilder<JavaCodeUnit>> getTypeParameterBuilders() {
            return typeParameterBuilders;
        }

        List<JavaTypeCreationProcess<JavaCodeUnit>> getParameterTypes() {
            return parameterTypes;
        }

        Optional<JavaTypeCreationProcess<JavaCodeUnit>> getReturnType() {
            return returnType;
        }
    }
}
