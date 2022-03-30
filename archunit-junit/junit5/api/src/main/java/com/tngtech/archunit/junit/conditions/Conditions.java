/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.junit.conditions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.PublicAPI.State;
import com.tngtech.archunit.PublicAPI.Usage;
import com.tngtech.archunit.junit.internal.AdapterFor;
import com.tngtech.archunit.junit.ArchTest;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;


/**
 * A collection of wrappers for declaring {@code DisabledIfXxx} annotations on {@link ArchTest} annotated fields
 */
@Target({ })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditions {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable.class)
    @interface DisabledIfEnvironmentVariable {
        String named();

        /**
         * @see org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable#matches()
         */
        String matches();

        /**
         * @see org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable#disabledReason()
         */
        String disabledReason() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable.class)
    @interface EnabledIfEnvironmentVariable {
        /**
         * @see org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable#named()
         */
        String named();

        /**
         * @see org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable#matches()
         */
        String matches();

        /**
         * @see org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable#disabledReason()
         */
        String disabledReason() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.EnabledIfSystemProperty.class)
    @interface EnabledIfSystemProperty {

        /**
         * @see org.junit.jupiter.api.condition.EnabledIfSystemProperty#named()
         */
        String named();

        /**
         * @see org.junit.jupiter.api.condition.EnabledIfSystemProperty#matches()
         */
        String matches();

        /**
         * @see org.junit.jupiter.api.condition.EnabledIfSystemProperty#disabledReason()
         */
        String disabledReason() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.DisabledIfSystemProperty.class)
    @interface DisabledIfSystemProperty {

        /**
         * @see org.junit.jupiter.api.condition.DisabledIfSystemProperty#named()
         */
        String named();

        /**
         * @see org.junit.jupiter.api.condition.DisabledIfSystemProperty#matches()
         */
        String matches();

        /**
         * @see org.junit.jupiter.api.condition.DisabledIfSystemProperty#disabledReason()
         */
        String disabledReason() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.EnabledOnJre.class)
    @interface EnabledOnJre {

        /**
         * @see org.junit.jupiter.api.condition.EnabledOnJre#value()
         */
        JRE[] value();

        /**
         * @see org.junit.jupiter.api.condition.EnabledOnJre#disabledReason()
         */
        String disabledReason() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.DisabledOnJre.class)
    @interface DisabledOnJre {
        /**
         * @see org.junit.jupiter.api.condition.DisabledOnJre#value()
         */
        JRE[] value();

        /**
         * @see org.junit.jupiter.api.condition.DisabledOnJre#disabledReason()
         */
        String disabledReason() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.EnabledForJreRange.class)
    @interface EnabledForJreRange {
        /**
         * @see org.junit.jupiter.api.condition.EnabledForJreRange#min()
         */
        JRE min() default JRE.JAVA_8;

        /**
         * @see org.junit.jupiter.api.condition.EnabledForJreRange#max()
         */
        JRE max() default JRE.OTHER;

        /**
         * @see org.junit.jupiter.api.condition.EnabledForJreRange#disabledReason()
         */
        String disabledReason() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.DisabledForJreRange.class)
    @interface DisabledForJreRange {
        /**
         * @see org.junit.jupiter.api.condition.DisabledForJreRange#min()
         */
        JRE min() default JRE.JAVA_8;

        /**
         * @see org.junit.jupiter.api.condition.DisabledForJreRange#max()
         */
        JRE max() default JRE.OTHER;

        /**
         * @see org.junit.jupiter.api.condition.DisabledForJreRange#disabledReason()
         */
        String disabledReason() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.EnabledOnOs.class)
    @interface EnabledOnOs {
        /**
         * @see org.junit.jupiter.api.condition.EnabledOnOs#value()
         */
        OS[] value();

        /**
         * @see org.junit.jupiter.api.condition.EnabledOnOs#disabledReason()
         */
        String disabledReason() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.DisabledOnOs.class)
    @interface DisabledOnOs {
        /**
         * @see org.junit.jupiter.api.condition.DisabledOnOs#value()
         */
        OS[] value();

        /**
         * @see org.junit.jupiter.api.condition.DisabledOnOs#disabledReason()
         */
        String disabledReason() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.EnabledIf.class)
    @interface EnabledIf {
        /**
         * @see org.junit.jupiter.api.condition.EnabledIf#value()
         */
        String value();

        /**
         * @see org.junit.jupiter.api.condition.EnabledIf#disabledReason()
         */
        String disabledReason() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PublicAPI(usage = Usage.ACCESS, state = State.EXPERIMENTAL)
    @AdapterFor(org.junit.jupiter.api.condition.DisabledIf.class)
    @interface DisabledIf {
        /**
         * @see org.junit.jupiter.api.condition.DisabledIf#value()
         */
        String value();

        /**
         * @see org.junit.jupiter.api.condition.DisabledIf#disabledReason()
         */
        String disabledReason() default "";
    }
}
