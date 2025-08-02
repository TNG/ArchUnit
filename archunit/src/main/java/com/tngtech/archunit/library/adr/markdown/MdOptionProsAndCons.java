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

import com.tngtech.archunit.library.adr.OptionProsAndCons;

import java.util.List;
import java.util.Optional;

public final class MdOptionProsAndCons implements OptionProsAndCons {
    private final String title;
    private String description;
    private String example;
    private final List<String> prosAndCons;

    public MdOptionProsAndCons(final String title, final List<String> prosAndCons) {
        this.title = title;
        this.prosAndCons = prosAndCons;
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(this.description);
    }

    @Override
    public OptionProsAndCons withDescription(final String description) {
        this.description = description;
        return this;
    }

    @Override
    public Optional<String> example() {
        return Optional.ofNullable(this.example);
    }

    @Override
    public OptionProsAndCons withExample(final String example) {
        this.example = example;
        return this;
    }

    @Override
    public List<String> prosAndCons() {
        return this.prosAndCons;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("### ").append(title()).append("\n\n");
        description().ifPresent(d -> sb.append(d).append("\n\n"));
        example().ifPresent(e -> sb.append(e).append("\n\n"));
        prosAndCons().forEach(pc ->
                sb.append("* ").append(pc).append("\n")
        );
        return sb.toString();
    }
}
