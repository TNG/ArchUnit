/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.modules.syntax;

import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ArchModules.DescriptorCreator;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * Serves the same purpose as {@link DescriptorCreator}, but carries along a {@link HasDescription#getDescription() description}
 * to be used by rule syntax elements.
 *
 * @param <DESCRIPTOR> The type of the {@link ArchModule.Descriptor} the respective {@link ArchModule}s will have
 */
@PublicAPI(usage = INHERITANCE)
public interface DescriptorFunction<DESCRIPTOR extends ArchModule.Descriptor> extends HasDescription {

    /**
     * @see DescriptorCreator#create(ArchModule.Identifier, Set)
     */
    DESCRIPTOR apply(ArchModule.Identifier identifier, Set<JavaClass> containedClasses);

    /**
     * Convenience method to create a {@link DescriptorFunction} from a {@link DescriptorCreator} and a textual {@code description}.
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    static <D extends ArchModule.Descriptor> DescriptorFunction<D> describe(String description, DescriptorCreator<D> descriptorCreator) {
        return new DescriptorFunction<D>() {
            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public D apply(ArchModule.Identifier identifier, Set<JavaClass> containedClasses) {
                return descriptorCreator.create(identifier, containedClasses);
            }
        };
    }
}
