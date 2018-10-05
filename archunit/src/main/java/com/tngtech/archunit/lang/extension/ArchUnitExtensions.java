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

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Internal
public class ArchUnitExtensions {
    private static final Logger LOG = LoggerFactory.getLogger(ArchUnitExtensions.class);

    private static final String ENABLED_PROPERTY = "enabled";

    private final ArchUnitExtensionLoader extensionLoader;

    public ArchUnitExtensions() {
        this(new ArchUnitExtensionLoader());
    }

    // Used for testing
    private ArchUnitExtensions(ArchUnitExtensionLoader extensionLoader) {
        this.extensionLoader = extensionLoader;
    }

    public void dispatch(final EvaluatedRule evaluatedRule) {
        callOnEnabledExtensions(new ExtensionCallable() {
            @Override
            public void call(ArchUnitExtension extension) {
                extension.handle(evaluatedRule);
            }
        });
    }

    public void finish(final JavaClasses classes) {
        callOnEnabledExtensions(new ExtensionCallable() {
            @Override
            public void call(ArchUnitExtension extension) {
                extension.onFinished(classes);
            }
        });
    }

    private void callOnEnabledExtensions(ExtensionCallable callable) {
        for (ArchUnitExtension extension : extensionLoader.getAll()) {
            callOnExtensionIfEnabled(extension, callable);
        }
    }

    private void callOnExtensionIfEnabled(ArchUnitExtension extension, ExtensionCallable callable) {
        ArchConfiguration configuration = ArchConfiguration.get();
        Properties extensionProperties = configuration.getExtensionProperties(extension.getUniqueIdentifier());
        if (isEnabled(extensionProperties)) {
            configureAndCall(extension, extensionProperties, callable);
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Extension '{}' is disabled, skipping... (to enable this extension, configure extension.{}.{}=true)",
                    extension.getUniqueIdentifier(), extension.getUniqueIdentifier(), ENABLED_PROPERTY);
        }
    }

    private void configureAndCall(ArchUnitExtension extension, Properties extensionProperties, ExtensionCallable callable) {
        try {
            extension.configure(extensionProperties);
            callable.call(extension);
        } catch (RuntimeException e) {
            LOG.warn(String.format("Error in extension '%s'", extension.getUniqueIdentifier()), e);
        }
    }

    private Boolean isEnabled(Properties extensionProperties) {
        return Boolean.valueOf(extensionProperties.getProperty(ENABLED_PROPERTY, "false"));
    }

    private interface ExtensionCallable {
        void call(ArchUnitExtension extension);
    }
}
