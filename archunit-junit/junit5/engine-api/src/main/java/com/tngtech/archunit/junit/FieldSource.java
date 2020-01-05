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
package com.tngtech.archunit.junit;

import java.lang.reflect.Field;
import java.util.Objects;

import com.tngtech.archunit.PublicAPI;
import org.junit.platform.engine.TestSource;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class FieldSource implements TestSource {
    private final String className;
    private final String fieldName;

    private FieldSource(Field field) {
        className = field.getDeclaringClass().getName();
        fieldName = field.getName();
    }

    @PublicAPI(usage = ACCESS)
    public String getClassName() {
        return className;
    }

    @PublicAPI(usage = ACCESS)
    public String getFieldName() {
        return fieldName;
    }

	@Override
	public int hashCode() {
		return Objects.hash(className, fieldName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final FieldSource other = (FieldSource) obj;
		return Objects.equals(this.className, other.className)
				&& Objects.equals(this.fieldName, other.fieldName);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + className + '.' + fieldName + '}';
	}

    static FieldSource from(Field field) {
        return new FieldSource(field);
    }
}
