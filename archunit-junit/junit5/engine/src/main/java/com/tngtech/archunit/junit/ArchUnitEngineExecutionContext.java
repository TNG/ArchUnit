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
package com.tngtech.archunit.junit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.Extension;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;

class ArchUnitEngineExecutionContext implements EngineExecutionContext {
    private final EngineExecutionListener engineExecutionListener;
    private final ConfigurationParameters configurationParameters;

    private final Map<Class<? extends Extension>, Extension> extensions = new HashMap<>();

    public ArchUnitEngineExecutionContext(EngineExecutionListener engineExecutionListener, ConfigurationParameters configurationParameters) {
        this.engineExecutionListener = engineExecutionListener;
        this.configurationParameters = configurationParameters;
    }

    public EngineExecutionListener getEngineExecutionListener() {
        return engineExecutionListener;
    }

    public ConfigurationParameters getConfigurationParameters() {
        return configurationParameters;
    }

    public Collection<Extension> getExtensions() {
        return Collections.unmodifiableCollection(extensions.values());
    }

    public <T extends Extension> Collection<T> getExtensions(Class<T> type) {
        return getExtensions().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toSet());
    }

    public ArchUnitEngineExecutionContext registerExtension(final Extension extension) {
        extensions.put(extension.getClass(), extension);
        return this;
    }
}
