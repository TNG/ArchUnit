/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.metrics.rendering;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.System.lineSeparator;

public class AsciiDocTable {
    private final List<String> header;
    private final List<List<String>> body;

    private AsciiDocTable(List<String> header, List<List<String>> body) {
        this.header = header;
        this.body = body;
    }

    public String render() {
        List<String> rows = newArrayList("|===");
        rows.add("| " + Joiner.on(" | ").join(header));
        rows.add("");
        for (List<String> row : body) {
            rows.add("| " + Joiner.on(" | ").join(row));
        }
        rows.add("|===");
        return Joiner.on(lineSeparator()).join(rows);
    }

    public static RowCreator header() {
        return new Creator().row();
    }

    public static class Creator {
        private List<String> header;
        private final List<List<String>> table = new ArrayList<>();

        private Creator() {
        }

        private Creator addRow(List<String> row) {
            if (header == null) {
                header = row;
            } else {
                table.add(row);
            }
            return this;
        }

        public RowCreator row() {
            return new RowCreator(this);
        }

        public AsciiDocTable create() {
            return new AsciiDocTable(header, table);
        }
    }

    public static class RowCreator {
        private final Creator creator;
        private final List<String> row = new ArrayList<>();

        private RowCreator(Creator creator) {
            this.creator = creator;
        }

        public RowCreator addColumnValue(String value) {
            row.add(value);
            return this;
        }

        public Creator end() {
            return creator.addRow(row);
        }
    }
}
