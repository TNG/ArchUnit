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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
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

import static com.google.common.base.Functions.compose;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createGenericArrayType;
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
                currentType.addBound(new ReferenceCreationProcess<JavaClass>(name, ReferenceCreationProcess.JavaTypeVariableFinisher.IDENTITY));
            }

            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                return TypeArgumentProcessor.create(wildcard, currentBound);
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
                return TypeArgumentProcessor.create(wildcard, superclass);
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
                return TypeArgumentProcessor.create(wildcard, currentInterface);
            }
        }
    }

    private static class NewJavaTypeCreationProcess<OWNER extends HasDescription> implements JavaTypeCreationProcess<OWNER> {
        private final JavaTypeBuilder<OWNER> builder;

        NewJavaTypeCreationProcess(JavaTypeBuilder<OWNER> builder) {
            this.builder = builder;
        }

        @Override
        public JavaType finish(OWNER owner, Iterable<JavaTypeVariable<?>> allTypeParametersInContext, ClassesByTypeName classes) {
            return builder.build(owner, allTypeParametersInContext, classes);
        }
    }

    private static class ReferenceCreationProcess<OWNER extends HasDescription> implements JavaTypeCreationProcess<OWNER> {
        private final String typeVariableName;
        private final JavaTypeVariableFinisher finisher;

        ReferenceCreationProcess(String typeVariableName, JavaTypeVariableFinisher finisher) {
            this.typeVariableName = typeVariableName;
            this.finisher = finisher;
        }

        @Override
        public JavaType finish(OWNER owner, Iterable<JavaTypeVariable<?>> allTypeParametersInContext, ClassesByTypeName classes) {
            return finisher.finish(createTypeVariable(owner, allTypeParametersInContext, classes), classes);
        }

        private JavaType createTypeVariable(OWNER owner, Iterable<JavaTypeVariable<?>> allTypeParametersInContext, ClassesByTypeName classes) {
            for (JavaTypeVariable<?> existingTypeVariable : allTypeParametersInContext) {
                if (existingTypeVariable.getName().equals(typeVariableName)) {
                    return existingTypeVariable;
                }
            }
            // type variables can be missing from the import context -> create a simple unbound type variable since we have no more information
            return new JavaTypeParameterBuilder<>(typeVariableName).build(owner, classes);
        }

        abstract static class JavaTypeVariableFinisher {
            abstract JavaType finish(JavaType input, ClassesByTypeName classes);

            abstract String getFinishedName(String name);

            JavaTypeVariableFinisher after(final JavaTypeVariableFinisher other) {
                return new JavaTypeVariableFinisher() {
                    @Override
                    JavaType finish(JavaType input, ClassesByTypeName classes) {
                        return JavaTypeVariableFinisher.this.finish(other.finish(input, classes), classes);
                    }

                    @Override
                    String getFinishedName(String name) {
                        return JavaTypeVariableFinisher.this.getFinishedName(other.getFinishedName(name));
                    }
                };
            }

            static JavaTypeVariableFinisher IDENTITY = new JavaTypeVariableFinisher() {
                @Override
                JavaType finish(JavaType input, ClassesByTypeName classes) {
                    return input;
                }

                @Override
                String getFinishedName(String name) {
                    return name;
                }
            };
        }
    }

    private static class TypeArgumentProcessor extends SignatureVisitor {
        private static final Function<JavaClassDescriptor, JavaClassDescriptor> TO_ARRAY_TYPE = new Function<JavaClassDescriptor, JavaClassDescriptor>() {
            @Override
            @SuppressWarnings("ConstantConditions") // we never return null by convention
            public JavaClassDescriptor apply(JavaClassDescriptor input) {
                return input.toArrayDescriptor();
            }
        };
        private static final ReferenceCreationProcess.JavaTypeVariableFinisher GENERIC_ARRAY_CREATOR = new ReferenceCreationProcess.JavaTypeVariableFinisher() {
            @Override
            public JavaType finish(JavaType componentType, ClassesByTypeName classes) {
                JavaClassDescriptor erasureType = JavaClassDescriptor.From.javaClass(componentType.toErasure()).toArrayDescriptor();
                JavaClass erasure = classes.get(erasureType.getFullyQualifiedClassName());
                return createGenericArrayType(componentType, erasure);
            }

            @Override
            String getFinishedName(String name) {
                return name + "[]";
            }
        };

        private final TypeArgumentType typeArgumentType;
        private final JavaParameterizedTypeBuilder<JavaClass> parameterizedType;
        private final Function<JavaClassDescriptor, JavaClassDescriptor> typeMapping;
        private final ReferenceCreationProcess.JavaTypeVariableFinisher typeVariableFinisher;

        private JavaParameterizedTypeBuilder<JavaClass> currentTypeArgument;

        TypeArgumentProcessor(
                TypeArgumentType typeArgumentType,
                JavaParameterizedTypeBuilder<JavaClass> parameterizedType,
                Function<JavaClassDescriptor, JavaClassDescriptor> typeMapping,
                ReferenceCreationProcess.JavaTypeVariableFinisher typeVariableFinisher) {
            super(ASM_API_VERSION);
            this.typeArgumentType = typeArgumentType;
            this.parameterizedType = parameterizedType;
            this.typeMapping = typeMapping;
            this.typeVariableFinisher = typeVariableFinisher;
        }

        @Override
        public void visitClassType(String internalObjectName) {
            JavaClassDescriptor type = typeMapping.apply(JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName));
            log.trace("Encountered {} for {}: Class type {}", typeArgumentType.description, parameterizedType.getTypeName(), type.getFullyQualifiedClassName());
            currentTypeArgument = new JavaParameterizedTypeBuilder<>(type);
            typeArgumentType.addTypeArgumentToBuilder(parameterizedType, new NewJavaTypeCreationProcess<>(this.currentTypeArgument));
        }

        @Override
        public void visitBaseType(char descriptor) {
            visitClassType(String.valueOf(descriptor));
        }

        @Override
        public void visitTypeArgument() {
            log.trace("Encountered wildcard for {}", currentTypeArgument.getTypeName());
            currentTypeArgument.addTypeArgument(new NewJavaTypeCreationProcess<>(new JavaWildcardTypeBuilder<JavaClass>()));
        }

        @Override
        public void visitTypeVariable(String name) {
            if (log.isTraceEnabled()) {
                log.trace("Encountered {} for {}: Type variable {}", typeArgumentType.description, parameterizedType.getTypeName(), typeVariableFinisher.getFinishedName(name));
            }
            typeArgumentType.addTypeArgumentToBuilder(parameterizedType, new ReferenceCreationProcess<JavaClass>(name, typeVariableFinisher));
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            return TypeArgumentProcessor.create(wildcard, currentTypeArgument, typeMapping, typeVariableFinisher);
        }

        @Override
        public SignatureVisitor visitArrayType() {
            return new TypeArgumentProcessor(typeArgumentType, parameterizedType, compose(typeMapping, TO_ARRAY_TYPE), typeVariableFinisher.after(GENERIC_ARRAY_CREATOR));
        }

        static TypeArgumentProcessor create(char identifier, JavaParameterizedTypeBuilder<JavaClass> parameterizedType) {
            return create(identifier, parameterizedType, Functions.<JavaClassDescriptor>identity(), ReferenceCreationProcess.JavaTypeVariableFinisher.IDENTITY);
        }

        static TypeArgumentProcessor create(
                char identifier,
                JavaParameterizedTypeBuilder<JavaClass> parameterizedType,
                Function<JavaClassDescriptor, JavaClassDescriptor> typeMapping,
                ReferenceCreationProcess.JavaTypeVariableFinisher typeVariableFinisher) {

            switch (identifier) {
                case INSTANCEOF:
                    return new TypeArgumentProcessor(PARAMETERIZED_TYPE, parameterizedType, typeMapping, typeVariableFinisher);
                case EXTENDS:
                    return new TypeArgumentProcessor(WILDCARD_WITH_UPPER_BOUND, parameterizedType, typeMapping, typeVariableFinisher);
                case SUPER:
                    return new TypeArgumentProcessor(WILDCARD_WITH_LOWER_BOUND, parameterizedType, typeMapping, typeVariableFinisher);
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

        abstract void addTypeArgumentToBuilder(JavaParameterizedTypeBuilder<JavaClass> parameterizedType, JavaTypeCreationProcess<JavaClass> creationProcess);
    }

    private static final TypeArgumentType PARAMETERIZED_TYPE = new TypeArgumentType("type argument") {
        @Override
        void addTypeArgumentToBuilder(JavaParameterizedTypeBuilder<JavaClass> parameterizedType, JavaTypeCreationProcess<JavaClass> typeCreationProcess) {
            parameterizedType.addTypeArgument(typeCreationProcess);
        }
    };

    private static final TypeArgumentType WILDCARD_WITH_UPPER_BOUND = new TypeArgumentType("wildcard with upper bound") {
        @Override
        void addTypeArgumentToBuilder(JavaParameterizedTypeBuilder<JavaClass> parameterizedType, JavaTypeCreationProcess<JavaClass> typeCreationProcess) {
            parameterizedType.addTypeArgument(new NewJavaTypeCreationProcess<>(new JavaWildcardTypeBuilder<JavaClass>().addUpperBound(typeCreationProcess)));
        }
    };

    private static final TypeArgumentType WILDCARD_WITH_LOWER_BOUND = new TypeArgumentType("wildcard with lower bound") {
        @Override
        void addTypeArgumentToBuilder(JavaParameterizedTypeBuilder<JavaClass> parameterizedType, JavaTypeCreationProcess<JavaClass> typeCreationProcess) {
            parameterizedType.addTypeArgument(new NewJavaTypeCreationProcess<>(new JavaWildcardTypeBuilder<JavaClass>().addLowerBound(typeCreationProcess)));
        }
    };
}
