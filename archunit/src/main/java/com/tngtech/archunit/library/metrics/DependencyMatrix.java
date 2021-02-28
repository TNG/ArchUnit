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
package com.tngtech.archunit.library.metrics;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.tngtech.archunit.library.metrics.components.MetricsComponent;
import com.tngtech.archunit.library.metrics.components.MetricsComponents;
import com.tngtech.archunit.library.metrics.rendering.AsciiDocTable;
import com.tngtech.archunit.library.metrics.rendering.AsciiDocTable.RowCreator;

public class DependencyMatrix {
    private final MetricsComponents<?> components;

    public DependencyMatrix(MetricsComponents<?> components) {
        this.components = components;
    }

    public AsciiDocTable toAsciiDocTable() {
        return toAsciiDocTable(components);
    }

    private static <T> AsciiDocTable toAsciiDocTable(MetricsComponents<T> metricsComponents) {
        List<MetricsComponent<T>> components = sortedByName(metricsComponents);

        RowCreator header = AsciiDocTable.header().addColumnValue("");
        for (MetricsComponent<?> component : components) {
            header.addColumnValue(component.getName());
        }

        AsciiDocTable.Creator tableCreator = header.end();
        for (int i = 0; i < components.size(); i++) {
            RowCreator row = tableCreator.row().addColumnValue(components.get(i).getName());
            for (int j = 0; j < components.size(); j++) {
                if (i == j) {
                    row.addColumnValue("-");
                } else {
                    row.addColumnValue(String.valueOf(components.get(i).getElementDependenciesTo(components.get(j)).size()));
                }
            }
            tableCreator = row.end();
        }

        return tableCreator.create();
    }

    private static <T> ImmutableList<MetricsComponent<T>> sortedByName(MetricsComponents<T> metricsComponents) {
        return FluentIterable.from(metricsComponents)
                .toSortedList(Ordering.natural().onResultOf(new Function<MetricsComponent<T>, Comparable<String>>() {
                    @Override
                    public Comparable<String> apply(MetricsComponent<T> input) {
                        return input.getName();
                    }
                }));
    }

    public static DependencyMatrix of(MetricsComponents<?> components) {
        return new DependencyMatrix(components);
    }
}
