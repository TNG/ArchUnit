package com.tngtech.archunit.htmlvisualization;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getLast;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

class JsonSerializableAssertion extends AbstractObjectAssert<JsonSerializableAssertion, JsonSerializable> {
    private JsonSerializableAssertion(JsonSerializable jsonSerializable) {
        super(jsonSerializable, JsonSerializableAssertion.class);
    }

    static JsonSerializableAssertion assertThatJsonSerializable(JsonSerializable jsonSerializable) {
        return new JsonSerializableAssertion(jsonSerializable);
    }

    JsonSerializableAssertion isRoot() {
        assertThat(actual.type).isEqualTo("package");
        assertThat(actual.name).isEqualTo("default");
        assertThat(actual.fullName).isEqualTo("default");
        return myself;
    }

    JsonSerializableAssertion containsPath(ExpectedJsonNode... expectedNodes) {
        for (ExpectedJsonNode expectedNode : expectedNodes) {
            expectedNode.assertMatchWith(findChild(actual, expectedNode.name));
        }
        return myself;
    }

    JsonSerializableAssertion doesNotContainNodeWithNameThat(Predicate<? super String> predicate) {
        Set<JsonSerializable> nodesMatching = findNodesWithMatchingNames(singleton(actual), predicate);
        assertThat(nodesMatching).as("nodes with names matching predicate").isEmpty();
        return myself;
    }

    private Set<JsonSerializable> findNodesWithMatchingNames(Set<JsonSerializable> nodes, Predicate<? super String> predicate) {
        Set<JsonSerializable> result = new HashSet<>();
        for (JsonSerializable child : nodes) {
            if (predicate.apply(child.name)) {
                result.add(child);
            }
            result.addAll(findNodesWithMatchingNames(child.children, predicate));
        }
        return result;
    }

    private static JsonSerializable findChild(JsonSerializable jsonNode, String childName) {
        for (JsonSerializable child : jsonNode.children) {
            if (child.name.equals(childName)) {
                return child;
            }
        }
        throw new AssertionError(jsonNode + " has no child named '" + childName + "'");
    }

    static class ExpectedJsonNode {
        private final String type;
        private final String name;
        private final String fullName;
        private final Set<ExpectedJsonNode> children;

        ExpectedJsonNode(String type, String name, String fullName, Set<ExpectedJsonNode> children) {
            this.type = checkNotNull(type);
            this.name = checkNotNull(name);
            this.fullName = checkNotNull(fullName);
            this.children = checkNotNull(children);
        }

        ExpectedJsonNode containing(ExpectedJsonNode... expectedJsonNode) {
            return new ExpectedJsonNode(type, name, fullName, FluentIterable.from(children).append(expectedJsonNode).toSet());
        }

        static ExpectedJsonNode flattenedPackageNode(String packageName) {
            return new ExpectedJsonNode("package", packageName, packageName, Collections.<ExpectedJsonNode>emptySet());
        }

        static ExpectedJsonNode relativePackageNode(String fullPackageName) {
            String relativePackageName = getLast(Splitter.on(".").split(fullPackageName));
            return new ExpectedJsonNode("package", relativePackageName, fullPackageName, Collections.<ExpectedJsonNode>emptySet());
        }

        static ExpectedJsonNode classNode(Class<?> clazz) {
            checkArgument(!clazz.isArray(), "We don't serialize array types at the moment");
            checkArgument(!clazz.isAnonymousClass(), "For anonymous classes use `anonymousClassNode(..)` instead");

            String type = clazz.isInterface() ? "interface" : "class";
            return new ExpectedJsonNode(type, clazz.getSimpleName(), clazz.getName(), Collections.<ExpectedJsonNode>emptySet());
        }

        static ExpectedJsonNode anonymousClassNode(Class<?> clazz, int number) {
            checkArgument(!clazz.isArray());
            checkArgument(number > 0, "Anonymous class numbered names are always greater than 0");

            String expectedName = "<<Anonymous[" + number + "]>>";
            String expectedFullName = clazz.getName() + "$" + number;
            return new ExpectedJsonNode("class", expectedName, expectedFullName, Collections.<ExpectedJsonNode>emptySet());
        }

        public void assertMatchWith(JsonSerializable actual) {
            assertThat(actual.type).as("type of " + actual).isEqualTo(type);
            assertThat(actual.name).as("full name of " + actual).isEqualTo(name);
            assertThat(actual.fullName).as("full name of " + actual).isEqualTo(fullName);

            for (ExpectedJsonNode expectedChild : children) {
                JsonSerializable actualChild = findChild(actual, expectedChild.name);
                expectedChild.assertMatchWith(actualChild);
            }
        }
    }
}
