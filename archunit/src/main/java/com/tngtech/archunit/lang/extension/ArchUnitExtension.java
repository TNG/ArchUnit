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
package com.tngtech.archunit.lang.extension;

import java.util.Properties;
import java.util.ServiceLoader;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * ArchUnit extensions need to implement this interface. To register the extension,
 * add a text file (UTF-8 encoded) to the folder <i>/META-INF/services</i>, named
 * <br><br>
 * <i>/META-INF/services/com.tngtech.archunit.lang.extension.ArchUnitExtension</i>
 * <br><br>
 * and add a line with the fully qualified class name(s) to it
 * <br><br>
 * <code>com.mycompany.MyArchUnitExtension</code>
 * <br><br>
 * For further details, check the documentation of {@link ServiceLoader}.<br>
 * Whenever a rule is evaluated, ArchUnit will dispatch the result to all extensions configured this way,
 * before reacting to the result (e.g. by failing the test, if violations exist).
 */
@PublicAPI(usage = INHERITANCE, state = EXPERIMENTAL)
public interface ArchUnitExtension {
    /**
     * A unique String, identifying this extension, so ArchUnit can associate configured properties. The
     * String must not contain '<b>.</b>' (dot). The return value MUST BE UNIQUE between all configured extensions,
     * or an exception will be thrown, thus it's good practice, to use some company specific namespace.
     *
     * @return A unique String identifier of this extension
     */
    String getUniqueIdentifier();

    /**
     * Before calling {@link #handle(EvaluatedRule)}, ArchUnit will call this method, to pass configured
     * properties to the extension. Properties for an extension
     * can be configured via <code>{@value com.tngtech.archunit.ArchConfiguration#ARCHUNIT_PROPERTIES_RESOURCE_NAME}</code>
     * (compare {@link ArchConfiguration}).
     * Extension properties are identified by prefixing each property to pass with
     * <pre><code>extension.${extension-id}</code></pre>
     * (where <code>${extension-id}</code> refers to the unique id of this extension configured via
     * {@link #getUniqueIdentifier()}). This way, configuration follows an uniform way.
     * <br><br>
     * Example:
     * <pre><code>
     * extension.my-extension.foo=bar
     * extension.my-extension.baz=quux
     * </code></pre>
     * will pass a {@link Properties} object to the extension with {@link #getUniqueIdentifier()} == "my-extension",
     * which contains the entries
     * <pre><code>
     * foo=bar
     * baz=quux
     * </code></pre>
     *
     * @param properties Object holding the configured properties of this extension
     */
    void configure(Properties properties);

    /**
     * ArchUnit will call this method after evaluating any rule against imported classes, but before any
     * {@link AssertionError} is thrown from possible violations of the rule.
     *
     * @param evaluatedRule Contains details about the evaluated rule, i.e. which rule was evaluated, the imported
     *                      classes and the result of the evaluation
     */
    void handle(EvaluatedRule evaluatedRule);
}
