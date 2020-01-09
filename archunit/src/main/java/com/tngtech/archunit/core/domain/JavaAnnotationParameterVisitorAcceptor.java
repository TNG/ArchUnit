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
package com.tngtech.archunit.core.domain;

import java.util.Map;

/**
 * Sole purpose is to keep all the ugly boring fluff out of {@link JavaAnnotation}.
 * The order of the conditionals is based on an educated guess about the frequency of annotation parameter types.
 */
class JavaAnnotationParameterVisitorAcceptor {
    static void accept(Map<String, Object> properties, JavaAnnotation.ParameterVisitor parameterVisitor) {
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            if (property.getValue() instanceof Boolean) {
                parameterVisitor.visitBoolean(property.getKey(), (Boolean) property.getValue());
                continue;
            }
            if (property.getValue() instanceof Integer) {
                parameterVisitor.visitInteger(property.getKey(), (Integer) property.getValue());
                continue;
            }
            if (property.getValue() instanceof String) {
                parameterVisitor.visitString(property.getKey(), (String) property.getValue());
                continue;
            }
            if (property.getValue() instanceof JavaClass) {
                parameterVisitor.visitClass(property.getKey(), (JavaClass) property.getValue());
                continue;
            }
            if (property.getValue() instanceof JavaEnumConstant) {
                parameterVisitor.visitEnumConstant(property.getKey(), (JavaEnumConstant) property.getValue());
                continue;
            }
            if (property.getValue() instanceof JavaAnnotation<?>) {
                parameterVisitor.visitAnnotation(property.getKey(), (JavaAnnotation<?>) property.getValue());
                continue;
            }
            if (property.getValue() instanceof String[]) {
                for (String value : ((String[]) property.getValue())) {
                    parameterVisitor.visitString(property.getKey(), value);
                }
                continue;
            }
            if (property.getValue() instanceof JavaClass[]) {
                for (JavaClass value : ((JavaClass[]) property.getValue())) {
                    parameterVisitor.visitClass(property.getKey(), value);
                }
                continue;
            }
            if (property.getValue() instanceof JavaEnumConstant[]) {
                for (JavaEnumConstant value : ((JavaEnumConstant[]) property.getValue())) {
                    parameterVisitor.visitEnumConstant(property.getKey(), value);
                }
                continue;
            }
            if (property.getValue() instanceof JavaAnnotation<?>[]) {
                for (JavaAnnotation<?> value : ((JavaAnnotation<?>[]) property.getValue())) {
                    parameterVisitor.visitAnnotation(property.getKey(), value);
                }
                continue;
            }
            if (property.getValue() instanceof Long) {
                parameterVisitor.visitLong(property.getKey(), (Long) property.getValue());
                continue;
            }
            if (property.getValue() instanceof int[]) {
                for (int value : ((int[]) property.getValue())) {
                    parameterVisitor.visitInteger(property.getKey(), value);
                }
                continue;
            }
            if (property.getValue() instanceof boolean[]) {
                for (boolean value : ((boolean[]) property.getValue())) {
                    parameterVisitor.visitBoolean(property.getKey(), value);
                }
                continue;
            }
            if (property.getValue() instanceof long[]) {
                for (long value : ((long[]) property.getValue())) {
                    parameterVisitor.visitLong(property.getKey(), value);
                }
                continue;
            }
            if (property.getValue() instanceof Byte) {
                parameterVisitor.visitByte(property.getKey(), (Byte) property.getValue());
                continue;
            }
            if (property.getValue() instanceof byte[]) {
                for (byte value : ((byte[]) property.getValue())) {
                    parameterVisitor.visitByte(property.getKey(), value);
                }
                continue;
            }
            if (property.getValue() instanceof Character) {
                parameterVisitor.visitCharacter(property.getKey(), (Character) property.getValue());
                continue;
            }
            if (property.getValue() instanceof char[]) {
                for (char value : ((char[]) property.getValue())) {
                    parameterVisitor.visitCharacter(property.getKey(), value);
                }
                continue;
            }
            if (property.getValue() instanceof Double) {
                parameterVisitor.visitDouble(property.getKey(), (Double) property.getValue());
                continue;
            }
            if (property.getValue() instanceof double[]) {
                for (double value : ((double[]) property.getValue())) {
                    parameterVisitor.visitDouble(property.getKey(), value);
                }
                continue;
            }
            if (property.getValue() instanceof Float) {
                parameterVisitor.visitFloat(property.getKey(), (Float) property.getValue());
                continue;
            }
            if (property.getValue() instanceof float[]) {
                for (float value : ((float[]) property.getValue())) {
                    parameterVisitor.visitFloat(property.getKey(), value);
                }
                continue;
            }
            if (property.getValue() instanceof Short) {
                parameterVisitor.visitShort(property.getKey(), (Short) property.getValue());
                continue;
            }
            if (property.getValue() instanceof short[]) {
                for (short value : ((short[]) property.getValue())) {
                    parameterVisitor.visitShort(property.getKey(), value);
                }
                // The danger of missing the continue when resorting is bigger than the "nuisance" of the obsolete continue
                //noinspection UnnecessaryContinue
                continue;
            }
        }
    }
}
