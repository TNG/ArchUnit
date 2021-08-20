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
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.importer.ClassFileProcessor.ASM_API_VERSION;

class JavaFieldTypeSignatureImporter {
    private static final Logger log = LoggerFactory.getLogger(JavaFieldTypeSignatureImporter.class);

    static Optional<JavaTypeCreationProcess<JavaField>> parseAsmFieldTypeSignature(String signature) {
        if (signature == null) {
            return Optional.empty();
        }

        log.trace("Analyzing field signature: {}", signature);

        SignatureProcessor signatureProcessor = new SignatureProcessor();
        new SignatureReader(signature).accept(signatureProcessor);
        return signatureProcessor.getFieldType();
    }

    private static class SignatureProcessor extends SignatureVisitor {
        private final GenericMemberTypeProcessor<JavaField> genericFieldTypeProcessor = new GenericMemberTypeProcessor<>();

        SignatureProcessor() {
            super(ASM_API_VERSION);
        }

        @Override
        public SignatureVisitor visitSuperclass() {
            return genericFieldTypeProcessor;
        }

        Optional<JavaTypeCreationProcess<JavaField>> getFieldType() {
            return genericFieldTypeProcessor.getType();
        }
    }
}
