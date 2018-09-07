package com.tngtech.archunit.library.dependencies;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.tngtech.archunit.core.Convertible;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.library.dependencies.GraphTest.randomNode;
import static com.tngtech.archunit.library.dependencies.GraphTest.stringEdge;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class CycleTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void rejects_invalid_edges() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Edges are not connected");

        new Cycle<>(asList(stringEdge(randomNode(), randomNode()), stringEdge(randomNode(), randomNode())));
    }

    @Test
    public void rejects_single_edge() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("do not form a cycle");

        new Cycle<>(singletonList(stringEdge(randomNode(), randomNode())));
    }

    @Test
    public void minimal_nontrivial_cycle() {
        String nodeA = "Node-A";
        String nodeB = "Node-B";
        Cycle<String, ?> cycle = new Cycle<>(asList(stringEdge(nodeA, nodeB), stringEdge(nodeB, nodeA)));

        assertThat(cycle.getEdges()).hasSize(2);
    }

    @Test
    public void converts_attachments() {
        String first = randomNode();
        String second = randomNode();
        List<Edge<String, Attachment>> edgesWithAttachments = Lists.newArrayList(
                new Edge<>(first, second, ImmutableList.of(new Attachment("one"), new Attachment("two"))),
                new Edge<>(second, first, ImmutableList.of(new Attachment("three"), new Attachment("four"))));

        Set<Attachment> convertedToIdentity = new Cycle<>(edgesWithAttachments).convertTo(Attachment.class);

        assertThat(convertedToIdentity).containsOnly(
                new Attachment("one"),
                new Attachment("two"),
                new Attachment("three"),
                new Attachment("four"));

        Set<ConversionTarget> converted = new Cycle<>(edgesWithAttachments).convertTo(ConversionTarget.class);

        assertThat(converted).containsOnly(
                new ConversionTarget("one"),
                new ConversionTarget("two"),
                new ConversionTarget("three"),
                new ConversionTarget("four"));
    }

    private static class Attachment implements Convertible {
        private final String message;

        private Attachment(String message) {
            this.message = message;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Set<T> convertTo(Class<T> type) {
            if (type == ConversionTarget.class) {
                return (Set<T>) Collections.singleton(new ConversionTarget(message));
            }
            return Collections.emptySet();
        }

        @Override
        public int hashCode() {
            return Objects.hash(message);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Attachment other = (Attachment) obj;
            return Objects.equals(this.message, other.message);
        }
    }

    private static class ConversionTarget {
        private final String message;

        private ConversionTarget(String message) {
            this.message = message;
        }

        @Override
        public int hashCode() {
            return Objects.hash(message);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final ConversionTarget other = (ConversionTarget) obj;
            return Objects.equals(this.message, other.message);
        }
    }
}
