/*
 * Copyright 2014-2024 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.junit.engine_api;

import java.lang.reflect.Field;
import java.util.Objects;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import org.junit.platform.engine.TestSource;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class FieldSource implements TestSource {
    private final Class<?> javaClass;
    private final String fieldName;

    private FieldSource(Field field) {
        javaClass = field.getDeclaringClass();
        fieldName = field.getName();
    }

    @PublicAPI(usage = ACCESS)
    public String getClassName() {
        return javaClass.getName();
    }

    @PublicAPI(usage = ACCESS)
    public Class<?> getJavaClass() {
        return javaClass;
    }

    @PublicAPI(usage = ACCESS)
    public String getFieldName() {
        return fieldName;
    }

	@Override
	public int hashCode() {
        return Objects.hash(javaClass, fieldName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
        FieldSource other = (FieldSource) obj;
        return Objects.equals(this.javaClass, other.javaClass)
                && Objects.equals(this.fieldName, other.fieldName);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + getClassName() + '.' + fieldName + '}';
    }

    @Internal
    public static FieldSource from(Field field) {
        return new FieldSource(field);
    }
}
