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

import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeParameterBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaWildcardTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.TypeParametersBuilder;
import com.tngtech.archunit.core.importer.JavaClassProcessor.DeclarationHandler;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import static com.tngtech.archunit.core.importer.ClassFileProcessor.ASM_API_VERSION;

class JavaGenericTypeImporter {

    static void parseAsmTypeSignature(String signature, DeclarationHandler declarationHandler) {
        if (signature == null) {
            return;
        }

        JavaTypeVariableProcessor typeVariableProcessor = new JavaTypeVariableProcessor();
        new SignatureReader(signature).accept(typeVariableProcessor);
        declarationHandler.onDeclaredTypeParameters(new TypeParametersBuilder(typeVariableProcessor.typeParameterBuilders));
    }

    private static class JavaTypeVariableProcessor extends SignatureVisitor {
        private final List<JavaTypeParameterBuilder> typeParameterBuilders = new ArrayList<>();
        private JavaTypeParameterBuilder currentlyVisiting;

        JavaTypeVariableProcessor() {
            super(ASM_API_VERSION);
        }

        @Override
        public void visitFormalTypeParameter(String name) {
            currentlyVisiting = new JavaTypeParameterBuilder(name);
            typeParameterBuilders.add(currentlyVisiting);
        }

        @Override
        public SignatureVisitor visitClassBound() {
            return visitBound();
        }

        @Override
        public SignatureVisitor visitInterfaceBound() {
            return visitBound();
        }

        private SignatureVisitor visitBound() {
            return new SignatureVisitor(ASM_API_VERSION) {
                private JavaParameterizedTypeBuilder bound;

                @Override
                public void visitClassType(String internalObjectName) {
                    JavaParameterizedTypeBuilder builder = new JavaParameterizedTypeBuilder(JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName));
                    this.bound = builder;
                    currentlyVisiting.addBound(new NewJavaTypeCreationProcess(builder));
                }

                @Override
                public void visitTypeArgument() {
                    addWildcardTypeArgument();
                }

                @Override
                public void visitTypeVariable(String name) {
                    currentlyVisiting.addBound(new ReferenceCreationProcess(name));
                }

                @Override
                public SignatureVisitor visitTypeArgument(char wildcard) {
                    if (wildcard == '+') {
                        final JavaWildcardTypeBuilder javaWildcardTypeBuilder = addWildcardTypeArgument();
                        return new SignatureVisitor(ASM_API_VERSION) {
                            @Override
                            public void visitClassType(String internalObjectName) {
                                javaWildcardTypeBuilder.addUpperBound(newParameterizedTypeCreationProcess(internalObjectName));
                            }

                            @Override
                            public void visitTypeVariable(String name) {
                                javaWildcardTypeBuilder.addUpperBound(new ReferenceCreationProcess(name));
                            }
                        };
                    }
                    if (wildcard == '-') {
                        final JavaWildcardTypeBuilder javaWildcardTypeBuilder = addWildcardTypeArgument();
                        return new SignatureVisitor(ASM_API_VERSION) {
                            @Override
                            public void visitClassType(String internalObjectName) {
                                javaWildcardTypeBuilder.addLowerBound(newParameterizedTypeCreationProcess(internalObjectName));
                            }

                            @Override
                            public void visitTypeVariable(String name) {
                                javaWildcardTypeBuilder.addLowerBound(new ReferenceCreationProcess(name));
                            }
                        };
                    }
                    return new SignatureVisitor(ASM_API_VERSION) {
                        @Override
                        public void visitClassType(String internalObjectName) {
                            bound.addTypeArgument(newParameterizedTypeCreationProcess(internalObjectName));
                        }
                    };
                }

                private NewJavaTypeCreationProcess newParameterizedTypeCreationProcess(String internalObjectName) {
                    return new NewJavaTypeCreationProcess(new JavaParameterizedTypeBuilder(JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName)));
                }

                private JavaWildcardTypeBuilder addWildcardTypeArgument() {
                    JavaWildcardTypeBuilder wildcardTypeBuilder = new JavaWildcardTypeBuilder();
                    bound.addTypeArgument(new NewJavaTypeCreationProcess(wildcardTypeBuilder));
                    return wildcardTypeBuilder;
                }
            };
        }
    }

    private static class NewJavaTypeCreationProcess implements DomainBuilders.JavaTypeCreationProcess {
        private final JavaTypeBuilder builder;

        NewJavaTypeCreationProcess(JavaTypeBuilder builder) {
            this.builder = builder;
        }

        @Override
        public JavaType finish(Iterable<JavaTypeVariable> allTypeParametersInContext, ClassesByTypeName classes) {
            return builder.build(allTypeParametersInContext, classes);
        }
    }

    private static class ReferenceCreationProcess implements DomainBuilders.JavaTypeCreationProcess {
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
}
