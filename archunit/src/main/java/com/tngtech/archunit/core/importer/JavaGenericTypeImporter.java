/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeParameterBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaWildcardTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.TypeParametersBuilder;
import com.tngtech.archunit.core.importer.JavaClassProcessor.DeclarationHandler;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.importer.ClassFileProcessor.ASM_API_VERSION;

class JavaGenericTypeImporter {
    private static final Logger log = LoggerFactory.getLogger(JavaGenericTypeImporter.class);

    static void parseAsmTypeSignature(String signature, DeclarationHandler declarationHandler) {
        if (signature == null) {
            return;
        }

        log.trace("Analyzing signature: {}", signature);

        JavaTypeVariableProcessor typeVariableProcessor = new JavaTypeVariableProcessor();
        new SignatureReader(signature).accept(typeVariableProcessor);
        declarationHandler.onDeclaredTypeParameters(new TypeParametersBuilder(typeVariableProcessor.typeParameterBuilders));
    }

    private static class JavaTypeVariableProcessor extends SignatureVisitor {
        private static final BoundProcessor boundProcessor = new BoundProcessor();

        private final List<JavaTypeParameterBuilder> typeParameterBuilders = new ArrayList<>();

        JavaTypeVariableProcessor() {
            super(ASM_API_VERSION);
        }

        @Override
        public void visitFormalTypeParameter(String name) {
            log.trace("Encountered type parameter {}", name);
            JavaTypeParameterBuilder type = new JavaTypeParameterBuilder(name);
            boundProcessor.setCurrentType(type);
            typeParameterBuilders.add(type);
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
            private JavaTypeParameterBuilder currentType;
            private JavaParameterizedTypeBuilder currentBound;

            BoundProcessor() {
                super(ASM_API_VERSION);
            }

            void setCurrentType(JavaTypeParameterBuilder type) {
                this.currentType = type;
            }

            @Override
            public void visitClassType(String internalObjectName) {
                JavaClassDescriptor type = JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName);
                log.trace("Encountered upper bound for {}: Class type {}", currentType.getName(), type.getFullyQualifiedClassName());
                this.currentBound = new JavaParameterizedTypeBuilder(type);
                currentType.addBound(new NewJavaTypeCreationProcess(this.currentBound));
            }

            @Override
            public void visitTypeArgument() {
                log.trace("Encountered wildcard for {}", currentBound.getTypeName());
                currentBound.addTypeArgument(new NewJavaTypeCreationProcess(new JavaWildcardTypeBuilder()));
            }

            @Override
            public void visitTypeVariable(String name) {
                log.trace("Encountered upper bound for {}: Type variable {}", currentType.getName(), name);
                currentType.addBound(new ReferenceCreationProcess(name));
            }

            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                return TypeArgumentProcessor.create(wildcard, currentBound);
            }
        }
    }

    private static class NewJavaTypeCreationProcess implements JavaTypeCreationProcess {
        private final JavaTypeBuilder builder;

        NewJavaTypeCreationProcess(JavaTypeBuilder builder) {
            this.builder = builder;
        }

        @Override
        public JavaType finish(Iterable<JavaTypeVariable> allTypeParametersInContext, ClassesByTypeName classes) {
            return builder.build(allTypeParametersInContext, classes);
        }
    }

    private static class ReferenceCreationProcess implements JavaTypeCreationProcess {
        private final String typeVariableName;

        ReferenceCreationProcess(String typeVariableName) {
            this.typeVariableName = typeVariableName;
        }

        @Override
        public JavaType finish(Iterable<JavaTypeVariable> allTypeParametersInContext, ClassesByTypeName classes) {
            for (JavaTypeVariable existingTypeVariable : allTypeParametersInContext) {
                if (existingTypeVariable.getName().equals(typeVariableName)) {
                    return existingTypeVariable;
                }
            }
            // type variables can be missing from the import context -> create a simple unbound type variable since we have no more information
            return new JavaTypeParameterBuilder(typeVariableName).build(classes);
        }
    }

    private static class TypeArgumentProcessor extends SignatureVisitor {
        private final TypeArgumentType typeArgumentType;
        private final JavaParameterizedTypeBuilder parameterizedType;

        private JavaParameterizedTypeBuilder currentTypeArgument;

        TypeArgumentProcessor(TypeArgumentType typeArgumentType, JavaParameterizedTypeBuilder parameterizedType) {
            super(ASM_API_VERSION);
            this.typeArgumentType = typeArgumentType;
            this.parameterizedType = parameterizedType;
        }

        @Override
        public void visitClassType(String internalObjectName) {
            JavaClassDescriptor type = JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName);
            log.trace("Encountered {} for {}: Class type {}", typeArgumentType.description, parameterizedType.getTypeName(), type.getFullyQualifiedClassName());
            currentTypeArgument = new JavaParameterizedTypeBuilder(type);
            typeArgumentType.addTypeArgumentToBuilder(parameterizedType, new NewJavaTypeCreationProcess(this.currentTypeArgument));
        }

        @Override
        public void visitTypeArgument() {
            log.trace("Encountered wildcard for {}", currentTypeArgument.getTypeName());
            currentTypeArgument.addTypeArgument(new NewJavaTypeCreationProcess(new JavaWildcardTypeBuilder()));
        }

        @Override
        public void visitTypeVariable(String name) {
            log.trace("Encountered {} for {}: Type variable {}", typeArgumentType.description, parameterizedType.getTypeName(), name);
            typeArgumentType.addTypeArgumentToBuilder(parameterizedType, new ReferenceCreationProcess(name));
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            return TypeArgumentProcessor.create(wildcard, currentTypeArgument);
        }

        static TypeArgumentProcessor create(char identifier, JavaParameterizedTypeBuilder parameterizedType) {
            switch (identifier) {
                case '=':
                    return new TypeArgumentProcessor(PARAMETERIZED_TYPE, parameterizedType);
                case '+':
                    return new TypeArgumentProcessor(WILDCARD_WITH_UPPER_BOUND, parameterizedType);
                case '-':
                    return new TypeArgumentProcessor(WILDCARD_WITH_LOWER_BOUND, parameterizedType);
                default:
                    throw new IllegalStateException(String.format("Cannot handle asm type argument identifier '%s'", identifier));
            }
        }
    }

    private abstract static class TypeArgumentType {
        private final String description;

        TypeArgumentType(String description) {
            this.description = description;
        }

        abstract void addTypeArgumentToBuilder(JavaParameterizedTypeBuilder parameterizedType, JavaTypeCreationProcess creationProcess);
    }

    private static final TypeArgumentType PARAMETERIZED_TYPE = new TypeArgumentType("type argument") {
        @Override
        void addTypeArgumentToBuilder(JavaParameterizedTypeBuilder parameterizedType, JavaTypeCreationProcess typeCreationProcess) {
            parameterizedType.addTypeArgument(typeCreationProcess);
        }
    };

    private static final TypeArgumentType WILDCARD_WITH_UPPER_BOUND = new TypeArgumentType("wildcard with upper bound") {
        @Override
        void addTypeArgumentToBuilder(JavaParameterizedTypeBuilder parameterizedType, JavaTypeCreationProcess typeCreationProcess) {
            parameterizedType.addTypeArgument(new NewJavaTypeCreationProcess(new JavaWildcardTypeBuilder().addUpperBound(typeCreationProcess)));
        }
    };

    private static final TypeArgumentType WILDCARD_WITH_LOWER_BOUND = new TypeArgumentType("wildcard with lower bound") {
        @Override
        void addTypeArgumentToBuilder(JavaParameterizedTypeBuilder parameterizedType, JavaTypeCreationProcess typeCreationProcess) {
            parameterizedType.addTypeArgument(new NewJavaTypeCreationProcess(new JavaWildcardTypeBuilder().addLowerBound(typeCreationProcess)));
        }
    };
}
