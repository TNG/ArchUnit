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

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.Description;

class ArchRuleExecution extends ArchTestExecution {
    private final Field ruleField;

    ArchRuleExecution(Class<?> testClass, Field ruleField, boolean ignore) {
        super(testClass, ignore);

        ArchTestInitializationException.check(ArchRule.class.isAssignableFrom(ruleField.getType()),
                "Rule field %s.%s to check must be of type %s",
                testClass.getSimpleName(), ruleField.getName(), ArchRule.class.getSimpleName());

        this.ruleField = ruleField;
    }

    @Override
    Result evaluateOn(JavaClasses classes) {
        ArchRule rule = getValue(ruleField, testClass);
        try {
            rule.check(classes);
        } catch (Exception | AssertionError e) {
            return new NegativeResult(describeSelf(), e);
        }
        return new PositiveResult();
    }

    @Override
    Description describeSelf() {
        return Description.createTestDescription(testClass, ruleField.getName());
    }

    @Override
    String getName() {
        return ruleField.getName();
    }
}
