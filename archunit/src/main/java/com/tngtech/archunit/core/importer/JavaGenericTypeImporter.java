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

import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeVariableBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaWildcardTypeBuilder;
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
        declarationHandler.onDeclaredTypeParameters(typeVariableProcessor.typeVariableBuilders);
    }

    private static class JavaTypeVariableProcessor extends SignatureVisitor {
        private final List<JavaTypeVariableBuilder> typeVariableBuilders = new ArrayList<>();
        private JavaTypeVariableBuilder currentlyVisiting;

        JavaTypeVariableProcessor() {
            super(ASM_API_VERSION);
        }

        @Override
        public void visitFormalTypeParameter(String name) {
            currentlyVisiting = new JavaTypeVariableBuilder(name);
            typeVariableBuilders.add(currentlyVisiting);
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
                    bound = new JavaParameterizedTypeBuilder(JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName));
                    currentlyVisiting.addBound(bound);
                }

                @Override
                public void visitTypeArgument() {
                    bound.addWildcardTypeParameter();
                }

                @Override
                public SignatureVisitor visitTypeArgument(char wildcard) {
                    if (wildcard == '+') {
                        final JavaWildcardTypeBuilder javaWildcardTypeBuilder = bound.addWildcardTypeParameter();
                        return new SignatureVisitor(ASM_API_VERSION) {
                            @Override
                            public void visitClassType(String internalObjectName) {
                                javaWildcardTypeBuilder.addUpperBound(new JavaParameterizedTypeBuilder(JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName)));
                            }
                        };
                    }
                    if (wildcard == '-') {
                        final JavaWildcardTypeBuilder javaWildcardTypeBuilder = bound.addWildcardTypeParameter();
                        return new SignatureVisitor(ASM_API_VERSION) {
                            @Override
                            public void visitClassType(String internalObjectName) {
                                javaWildcardTypeBuilder.addLowerBound(new JavaParameterizedTypeBuilder(JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName)));
                            }
                        };
                    }
                    return new SignatureVisitor(ASM_API_VERSION) {
                        @Override
                        public void visitClassType(String internalObjectName) {
                            bound.addTypeArgument(JavaClassDescriptorImporter.createFromAsmObjectTypeName(internalObjectName));
                        }
                    };
                }
            };
        }
    }
}
