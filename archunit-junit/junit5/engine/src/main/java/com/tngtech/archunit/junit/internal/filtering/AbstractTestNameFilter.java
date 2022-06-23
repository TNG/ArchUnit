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
package com.tngtech.archunit.junit.internal.filtering;/*
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;

public abstract class AbstractTestNameFilter implements TestSourceFilter {
    private static final Collection<TestSelectorFactory> SELECTOR_FACTORIES = Arrays.asList(
            new MethodSelectorFactory(),
            new FieldSelectorFactory()
    );
    private final String discoveryFilterClassName;

    public AbstractTestNameFilter(EngineDiscoveryRequest request, String discoveryFilterClassName) throws Exception {
        this.discoveryFilterClassName = discoveryFilterClassName;
        PostDiscoveryFilter postDiscoveryFilter = findPostDiscoveryFilter(request);
        PostDiscoveryFilter replacement = initialize(postDiscoveryFilter);
        if (replacement != postDiscoveryFilter) {
            replaceFilter(request, postDiscoveryFilter, replacement);
        }
    }

    private void replaceFilter(
            EngineDiscoveryRequest discoveryRequest,
            PostDiscoveryFilter postDiscoveryFilter,
            PostDiscoveryFilter replacement) throws ReflectiveOperationException {
        LauncherDiscoveryRequest request = (LauncherDiscoveryRequest) discoveryRequest;
        List<PostDiscoveryFilter> filters = new ArrayList<>(request.getPostDiscoveryFilters());
        filters.remove(postDiscoveryFilter);
        filters.add(replacement);
        ReflectionUtils.setField(discoveryRequest, "postDiscoveryFilters", filters);
    }

    public boolean shouldRun(TestSource source) {
        return resolveFactory(source)
                .map(factory -> factory.createSelector(source))
                .map(this::shouldRunAccordingToTestingTool)
                .orElse(true);
    }

    public static boolean checkApplicability(EngineDiscoveryRequest discoveryRequest, String discoveryFilterClassName) {
        if (!(discoveryRequest instanceof LauncherDiscoveryRequest)) {
            return false;
        }
        LauncherDiscoveryRequest request = (LauncherDiscoveryRequest) discoveryRequest;
        return request.getPostDiscoveryFilters().stream()
                .anyMatch(filter -> filter.getClass().getName().equals(discoveryFilterClassName));
    }

    private PostDiscoveryFilter findPostDiscoveryFilter(EngineDiscoveryRequest request) {
        return ((LauncherDiscoveryRequest) request).getPostDiscoveryFilters().stream()
                .filter(this::matches)
                .findAny().get();
    }

    boolean matches(PostDiscoveryFilter filter) {
        return discoveryFilterClassName.equals(filter.getClass().getName());
    }

    protected abstract PostDiscoveryFilter initialize(PostDiscoveryFilter filter) throws Exception;

    protected abstract boolean shouldRunAccordingToTestingTool(TestSelectorFactory.TestSelector selector);

    private Optional<TestSelectorFactory> resolveFactory(TestSource source) {
        return SELECTOR_FACTORIES.stream()
                .filter(factory -> factory.supports(source))
                .findAny();
    }
}
