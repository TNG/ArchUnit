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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeParameterBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaWildcardTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.TypeParametersBuilder;
import com.tngtech.archunit.core.importer.JavaClassProcessor.DeclarationHandler;
import com.tngtech.archunit.core.importer.SignatureTypeArgumentProcessor.NewJavaTypeCreationProcess;
import com.tngtech.archunit.core.importer.SignatureTypeArgumentProcessor.ReferenceCreationProcess;
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
        declarationHandler.onDeclaredTypeParameters(new TypeParametersBuilder(signatureProcessor.getTypeParameterBuilders()));

        Optional<JavaParameterizedTypeBuilder<JavaClass>> genericSuperclass = signatureProcessor.getGenericSuperclass();
        if (genericSuperclass.isPresent()) {
            declarationHandler.onGenericSuperclass(genericSuperclass.get());
        }

        declarationHandler.onGenericInterfaces(signatureProcessor.getGenericInterfaces());
    }

    private static class SignatureProcessor extends SignatureVisitor {
        private final BoundProcessor boundProcessor = new BoundProcessor();
        private final GenericSuperclassProcessor superclassProcessor = new GenericSuperclassProcessor();
        private final GenericInterfacesProcessor interfacesProcessor = new GenericInterfacesProcessor();

        SignatureProcessor() {
            super(ASM_API_VERSION);
        }

        List<JavaTypeParameterBuilder<JavaClass>> getTypeParameterBuilders() {
            return boundProcessor.typeParameterBuilders;
        }

        Optional<JavaParameterizedTypeBuilder<JavaClass>> getGenericSuperclass() {
            return Optional.fromNullable(superclassProcessor.superclass);
        }

        Set<JavaParameterizedTypeBuilder<JavaClass>> getGenericInterfaces() {
            return interfacesProcessor.interfaces;
        }

        @Override
        public void visitFormalTypeParameter(String name) {
            log.trace("Encountered type parameter {}", name);
            boundProcessor.addTypeParameter(name);
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
            return boundProcessor;
        }

        @Override
        public SignatureVisitor visitInterfaceBound() {
            return boundProcessor;
        }

        private static class BoundProcessor extends SignatureVisitor {
            private final List<JavaTypeParameterBuilder<JavaClass>> typeParameterBuilders = new ArrayList<>();

            private JavaTypeParameterBuilder<JavaClass> currentType;
            private JavaParameterizedTypeBuilder<JavaClass> currentBound;

            BoundProcessor() {
                super(ASM_API_VERSION);
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
                this.currentBound = new JavaParameterizedTypeBuilder<>(type);
                currentType.addBound(new NewJavaTypeCreationProcess<>(this.currentBound));
            }

            @Override
            public void visitTypeArgument() {
                log.trace("Encountered wildcard for {}", currentBound.getTypeName());
                currentBound.addTypeArgument(new NewJavaTypeCreationProcess<>(new JavaWildcardTypeBuilder<JavaClass>()));
            }

            @Override
            public void visitTypeVariable(String name) {
                log.trace("Encountered upper bound for {}: Type variable {}", currentType.getName(), name);
                currentType.addBound(new ReferenceCreationProcess<JavaClass>(name));
            }

            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                return SignatureTypeArgumentProcessor.create(wildcard, currentBound);
            }
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
            private final Set<JavaParameterizedTypeBuilder<JavaClass>> interfaces = new HashSet<>();
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
