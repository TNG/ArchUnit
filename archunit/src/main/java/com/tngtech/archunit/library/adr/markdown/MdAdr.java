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
import com.tngtech.archunit.library.adr.Adr;
import com.tngtech.archunit.library.adr.envelopes.AdrEnvelope;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class MdAdr extends AdrEnvelope {

    @PublicAPI(usage = ACCESS)
    public MdAdr(final Adr delegate) {
        super(delegate);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        metadata().ifPresent(m -> sb.append(m).append("\n"));
        sb.append("\n# ").append(title()).append("\n");
        sb.append("\n## Context and Problem Statement\n");
        sb.append("\n").append(contextAndProblemStatement()).append("\n");
        decisionDrivers().ifPresent(d -> {
            sb.append("\n## Decision Drivers");
            sb.append("\n\n");
            d.forEach(s -> sb.append("* ").append(s).append("\n"));
        });
        sb.append("\n## Considered Options\n\n");
        consideredOptions().forEach(o -> sb.append("* ").append(o).append("\n"));
        sb.append("\n## Decision Outcome\n\n");
        sb.append(decisionOutcome()).append("\n");
        consequences().ifPresent(c -> {
            sb.append("\n### Consequences\n\n");
            c.forEach(s -> sb.append("* ").append(s).append("\n"));
        });
        confirmation().ifPresent(c -> sb.append("\n### Confirmation\n\n").append(c).append("\n"));
        optionProsAndCons().ifPresent(opc -> {
            sb.append("\n## Pros and Cons of the Options\n\n");
            opc.forEach(s -> sb.append(s.toString()).append("\n"));
        });
        moreInformation().ifPresent(mi -> sb.append("## More Information\n\n").append(mi));
        return sb.toString();
    }
}
