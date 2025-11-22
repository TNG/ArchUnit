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
package com.tngtech.archunit.library.adr.markdown;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.library.adr.Metadata;
import com.tngtech.archunit.library.adr.envelopes.MetadataEnvelope;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class MdMetadata extends MetadataEnvelope {

    @PublicAPI(usage = ACCESS)
    public MdMetadata(final Metadata delegate) {
        super(delegate);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("---\n");
        status().ifPresent(s -> sb.append("status: ").append(s).append("\n"));
        date().ifPresent(d -> sb.append("date: ").append(d).append("\n"));
        decisionMakers().ifPresent(d -> sb.append("decision-makers: ").append(String.join(", ", d)).append("\n"));
        consulted().ifPresent(c -> sb.append("consulted: ").append(String.join(", ", c)).append("\n"));
        informed().ifPresent(i -> sb.append("informed: ").append(String.join(", ", i)).append("\n"));
        sb.append("---");
        return sb.toString();
    }
}
