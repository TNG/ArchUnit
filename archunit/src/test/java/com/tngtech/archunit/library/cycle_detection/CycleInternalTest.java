package com.tngtech.archunit.library.cycle_detection;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.tngtech.archunit.core.Convertible;
import org.junit.Test;

import static com.tngtech.archunit.library.cycle_detection.GraphTest.randomNode;
import static com.tngtech.archunit.library.cycle_detection.GraphTest.stringEdge;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CycleInternalTest {

    @Test
    public void rejects_invalid_edges() {
        List<Edge<String>> edges = asList(stringEdge(randomNode(), randomNode()), stringEdge(randomNode(), randomNode()));
        assertThatThrownBy(
                () -> new CycleInternal<>(edges)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Edges are not connected");
    }

    @Test
    public void rejects_single_edge() {
        List<Edge<String>> edges = singletonList(stringEdge(randomNode(), randomNode()));
        assertThatThrownBy(
                () -> new CycleInternal<>(edges)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not form a cycle");
    }

    @Test
    public void minimal_nontrivial_cycle() {
        String nodeA = "Node-A";
        String nodeB = "Node-B";
        CycleInternal<Edge<String>> cycle = new CycleInternal<>(asList(stringEdge(nodeA, nodeB), stringEdge(nodeB, nodeA)));

        assertThat(cycle.getEdges()).hasSize(2);
    }

    @Test
    public void converts_edges() {
        CycleInternal<Edge<String>> cycle = new CycleInternal<>(asList(
                new OriginalEdge("a", "b"),
                new OriginalEdge("b", "a"))
        );

        assertThat(cycle.convertTo(ConvertedEdge.class)).containsOnly(
                new ConvertedEdge("a", "b"),
                new ConvertedEdge("b", "a")
        );
    }

    private static class OriginalEdge implements Edge<String>, Convertible {
        private final String origin;
        private final String target;

        private OriginalEdge(String origin, String target) {
            this.origin = origin;
            this.target = target;
        }

        @Override
        public String getOrigin() {
            return origin;
        }

        @Override
        public String getTarget() {
            return target;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Set<T> convertTo(Class<T> type) {
            return ConvertedEdge.class.isAssignableFrom(type)
                    ? (Set<T>) singleton(new ConvertedEdge(origin, target))
                    : emptySet();
        }
    }

    private static class ConvertedEdge {
        private final String value;

        public ConvertedEdge(String origin, String target) {
            value = origin + "/" + target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ConvertedEdge that = (ConvertedEdge) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}
