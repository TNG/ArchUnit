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
import java.util.List;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassTypeParametersBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeParameterBuilder;
import com.tngtech.archunit.core.importer.JavaClassProcessor.DeclarationHandler;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.importer.ClassFileProcessor.ASM_API_VERSION;

class JavaClassSignatureImporter {
    private static final Logger log = LoggerFactory.getLogger(JavaClassSignatureImporter.class);

    static void parseAsmTypeSignature(String signature, DeclarationHandler declarationHandler) {
        if (signature == null) {
            return;
        }

        log.trace("Analyzing signature: {}", signature);

        SignatureProcessor signatureProcessor = new SignatureProcessor();
        new SignatureReader(signature).accept(signatureProcessor);
        declarationHandler.onDeclaredTypeParameters(new JavaClassTypeParametersBuilder(signatureProcessor.getTypeParameterBuilders()));

        Optional<JavaParameterizedTypeBuilder<JavaClass>> genericSuperclass = signatureProcessor.getGenericSuperclass();
        if (genericSuperclass.isPresent()) {
            declarationHandler.onGenericSuperclass(genericSuperclass.get());
        }

        declarationHandler.onGenericInterfaces(signatureProcessor.getGenericInterfaces());
    }

    private static class SignatureProcessor extends SignatureVisitor {
        private final SignatureTypeParameterProcessor<JavaClass> typeParameterProcessor = new SignatureTypeParameterProcessor<>();
        private final GenericSuperclassProcessor superclassProcessor = new GenericSuperclassProcessor();
        private final GenericInterfacesProcessor interfacesProcessor = new GenericInterfacesProcessor();

        SignatureProcessor() {
            super(ASM_API_VERSION);
        }

        List<JavaTypeParameterBuilder<JavaClass>> getTypeParameterBuilders() {
            return typeParameterProcessor.getTypeParameterBuilders();
        }

        Optional<JavaParameterizedTypeBuilder<JavaClass>> getGenericSuperclass() {
            return Optional.ofNullable(superclassProcessor.superclass);
        }

        List<JavaParameterizedTypeBuilder<JavaClass>> getGenericInterfaces() {
            return interfacesProcessor.interfaces;
        }

        @Override
        public void visitFormalTypeParameter(String name) {
            log.trace("Encountered type parameter {}", name);
            typeParameterProcessor.addTypeParameter(name);
        }

        @Override
        public SignatureVisitor visitSuperclass() {
            return superclassProcessor;
        }

        @Override
        public SignatureVisitor visitInterface() {
            return interfacesProcessor;
        }

        @Override
        public SignatureVisitor visitClassBound() {
            return typeParameterProcessor;
        }

        @Override
        public SignatureVisitor visitInterfaceBound() {
            return typeParameterProcessor;
        }

        private static class GenericSuperclassProcessor extends SignatureVisitor {
            private JavaParameterizedTypeBuilder<JavaClass> superclass;

            GenericSuperclassProcessor() {
                super(ASM_API_VERSION);
            }

            @Override
            public void visitClassType(String internalObjectName) {
                superclass = new JavaParameterizedTypeBuilder<>(JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName));
            }

            @Override
            public void visitInnerClassType(String name) {
                superclass = superclass.forInnerClass(name);
            }

            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                return SignatureTypeArgumentProcessor.create(wildcard, superclass);
            }
        }

        private static class GenericInterfacesProcessor extends SignatureVisitor {
            private final List<JavaParameterizedTypeBuilder<JavaClass>> interfaces = new ArrayList<>();
            private JavaParameterizedTypeBuilder<JavaClass> currentInterface;

            GenericInterfacesProcessor() {
                super(ASM_API_VERSION);
            }

            @Override
            public void visitClassType(String internalObjectName) {
                currentInterface = new JavaParameterizedTypeBuilder<>(JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName));
                interfaces.add(currentInterface);
            }

            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                return SignatureTypeArgumentProcessor.create(wildcard, currentInterface);
            }
        }
    }
}
