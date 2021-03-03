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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

import static com.google.common.io.ByteStreams.toByteArray;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;

public interface DiagramSpec {
    String diagramTemplate();

    String renderComponent(String identifier, List<String> description);

    String renderDependency(String originIdentifier, String targetIdentifier);

    String renderLegend(String text);

    class Factory {
        public static DiagramSpec plantUml() {
            return new DiagramSpec() {
                @Override
                public String diagramTemplate() {
                    return readResource("component-diagram.puml.template");
                }

                @Override
                public String renderComponent(String identifier, List<String> description) {
                    List<String> result = new ArrayList<>();
                    result.add("component " + identifier + " [");
                    result.addAll(description);
                    result.add("]");
                    return Joiner.on(lineSeparator()).join(result);
                }

                @Override
                public String renderDependency(String originIdentifier, String targetIdentifier) {
                    return originIdentifier + " --> " + targetIdentifier;
                }

                @Override
                public String renderLegend(String text) {
                    return "legend" + lineSeparator() +
                            text + lineSeparator() +
                            "endlegend";
                }
            };
        }

        public static DiagramSpec dot() {
            return new DiagramSpec() {
                @Override
                public String diagramTemplate() {
                    return readResource("component-graph.dot.html.template");
                }

                @Override
                public String renderComponent(String identifier, List<String> description) {
                    return identifier + "[label=\"" + Joiner.on("\\n").join(description) + "\"]";
                }

                @Override
                public String renderDependency(String originIdentifier, String targetIdentifier) {
                    return originIdentifier + " -> " + targetIdentifier;
                }

                @Override
                public String renderLegend(String text) {
                    return Joiner.on(lineSeparator()).join(
                            "subgraph { ",
                            "  legend [label=\"" + text + "\" shape=rectangle] ;",
                            "}");
                }
            };
        }

        private static String readResource(String path) {
            try {
                byte[] bytes = toByteArray(Diagram.class.getResourceAsStream(path));
                return new String(bytes, UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
