/*
 * Copyright 2018 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Includes all {@code @ArchTest} annotated members of another class into this ArchUnit test. For example
 * <pre><code>
 * class MyArchRuleSuite1 {
 *    {@literal @}ArchTest
 *     static final ArchRule suite1Rule1 = classes()...
 *
 *    {@literal @}ArchTest
 *     static void suite1Rule2(JavaClasses classes) {
 *         // ...
 *     }
 * }
 *
 * class MyArchRuleSuite2 {
 *    {@literal @}ArchTest
 *     static final ArchRule suite2Rule1 = classes()...
 * }
 *
 *{@literal @}AnalyzeClasses(..)
 * class MyArchitectureTest {
 *     // includes all{@literal @}ArchTest members from MyArchRuleSuite1
 *    {@literal @}ArchTest
 *     static final ArchTests includedRules1 = ArchTests.in(MyArchRuleSuite1.class);
 *
 *     // includes all{@literal @}ArchTest members from MyArchRuleSuite2
 *    {@literal @}ArchTest
 *     static final ArchTests includedRules2 = ArchTests.in(MyArchRuleSuite2.class);
 * }
 * </code></pre>
 */
public final class ArchTests {
    private final Class<?> definitionLocation;

    private ArchTests(Class<?> definitionLocation) {
        this.definitionLocation = definitionLocation;
    }

    /**
     * @param definitionLocation The class whose `@ArchTest` members should be included in this test
     * @return the {@link ArchTests} of the supplied class
     */
    @PublicAPI(usage = ACCESS)
    public static ArchTests in(Class<?> definitionLocation) {
        return new ArchTests(definitionLocation);
    }

    Class<?> getDefinitionLocation() {
        return definitionLocation;
    }
}
