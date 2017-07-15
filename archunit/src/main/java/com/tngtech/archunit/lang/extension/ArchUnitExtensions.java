/*
 * Copyright 2017 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Internal
public class ArchUnitExtensions {
    private static final Logger LOG = LoggerFactory.getLogger(ArchUnitExtensions.class);

    private final ArchUnitExtensionLoader extensionLoader;

    public ArchUnitExtensions() {
        this(new ArchUnitExtensionLoader());
    }

    // Used for testing
    private ArchUnitExtensions(ArchUnitExtensionLoader extensionLoader) {
        this.extensionLoader = extensionLoader;
    }

    public void dispatch(EvaluatedRule evaluatedRule) {
        for (ArchUnitExtension extension : extensionLoader.getAll()) {
            dispatch(evaluatedRule, extension);
        }
    }

    private void dispatch(EvaluatedRule evaluatedRule, ArchUnitExtension extension) {
        ArchConfiguration configuration = ArchConfiguration.get();
        try {
            extension.configure(configuration.getExtensionProperties(extension.getUniqueIdentifier()));
            extension.handle(evaluatedRule);
        } catch (RuntimeException e) {
            LOG.warn(String.format("Error in extension '%s'", extension.getUniqueIdentifier()), e);
        }
    }
}
