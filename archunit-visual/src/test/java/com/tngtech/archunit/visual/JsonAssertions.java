package com.tngtech.archunit.visual;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.junit.Assert;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

class JsonAssertions {

    static JsonAssertion assertThatJsonIn(File jsonFile) {
        return new JsonAssertion(jsonFile);
    }

    static class JsonAssertion {
        private final Map<Object, Object> actualJson;

        JsonAssertion(File jsonFile) {
            this.actualJson = JsonTestUtils.jsonToMap(jsonFile);
        }

        void isEquivalentToJsonIn(File jsonFile) {
            Map<Object, Object> expectedJson = JsonTestUtils.jsonToMap(jsonFile);
            Set<JsonDifference> differences = differences(actualJson, expectedJson);
            if (!differences.isEmpty()) {
                Assert.fail(String.format(
                        "Exported Json differs from expected :%n%s%n%nActual Json:%n%s%n%nExpected Json:%n%s",
                        Joiner.on(lineSeparator()).join(differences),
                        actualJson, expectedJson));
            }

            // Make sure, that we haven't overlooked anything when checking for JsonDifferences
            assertThat(actualJson).as("Complete Json values").isEqualTo(expectedJson);
        }
    }

    private static Set<JsonDifference> differences(Map<Object, Object> actual, Map<Object, Object> expected) {
        return mapDifferences("", actual, expected);
    }

    private static Set<JsonDifference> mapDifferences(String path, Map<Object, Object> actual, Map<Object, Object> expected) {
        Set<JsonDifference> differences = new HashSet<>();
        for (Map.Entry<Object, Object> actualEntry : actual.entrySet()) {
            differences.addAll(entryDifference(path, actualEntry, expected));
        }
        for (Object onlyInExpected : Sets.difference(expected.keySet(), actual.keySet())) {
            differences.add(new OnlyExpected(subPath(path, onlyInExpected), expected.get(onlyInExpected)));
        }
        return differences;
    }

    private static Set<? extends JsonDifference> entryDifference(String path, Map.Entry<Object, Object> actualEntry, Map<Object, Object> expectedValues) {
        if (!expectedValues.containsKey(actualEntry.getKey())) {
            return singleton(new OnlyActual(path, actualEntry.getValue()));
        } else {
            String subPath = subPath(path, actualEntry.getKey());
            Object actualValue = actualEntry.getValue();
            Object expectedValue = expectedValues.get(actualEntry.getKey());
            return objectDifferences(subPath, actualValue, expectedValue);
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<JsonDifference> objectDifferences(String path, Object valueActual, Object valueExpected) {
        if (Map.class.isInstance(valueActual) && Map.class.isInstance(valueExpected)) {
            return mapDifferences(path, (Map) valueActual, (Map) valueExpected);
        } else if (Set.class.isInstance(valueActual) && Set.class.isInstance(valueExpected)) {
            return setDifferences(path, (Set) valueActual, (Set) valueExpected);
        } else {
            if (!Objects.equals(valueActual, valueExpected)) {
                return singleton(new JsonDifference(path, valueActual, valueExpected));
            }
        }
        return emptySet();
    }

    private static Set<JsonDifference> setDifferences(String path, Set<Object> valueActual, Set<Object> valueExpected) {
        if (valueActual.equals(valueExpected)) {
            return emptySet();
        }

        JsonSet actual = JsonSet.from(valueActual);
        JsonSet expected = JsonSet.from(valueExpected);

        Set<JsonDifference> result = new HashSet<>();
        for (Object actualElement : actual) {
            if (!expected.contains(actualElement)) {
                result.add(new OnlyActual(path, actualElement));
            } else {
                String namedPath = path + tryFindName(actualElement);
                result.addAll(objectDifferences(namedPath, actualElement, expected.get(actualElement)));
            }
        }
        for (Object value : expected.subtract(actual)) {
            result.add(new OnlyExpected(path, value));
        }
        return result;
    }

    private static String tryFindName(Object element) {
        return element instanceof Map && ((Map) element).containsKey("name") ?
                String.format("[%s]", ((Map) element).get("name")) :
                "";
    }

    private static String subPath(String path, Object newPathElement) {
        return !path.isEmpty() ? Joiner.on(".").join(path, newPathElement) : newPathElement.toString();
    }

    private static class JsonDifference {
        final String path;
        final Object actual;
        final Object expected;

        private JsonDifference(String path, Object actual, Object expected) {
            this.path = path;
            this.actual = actual;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return String.format("ACTUAL[%s=%s] != EXPECTED[%s=%s]",
                    path, actual, path, expected);
        }
    }

    private static class OnlyActual extends JsonDifference {
        private OnlyActual(String path, Object actual) {
            super(path, actual, null);
        }

        @Override
        public String toString() {
            return String.format("Only present in actual value: %s=%s", path, actual);
        }
    }

    private static class OnlyExpected extends JsonDifference {
        private OnlyExpected(String path, Object right) {
            super(path, null, right);
        }

        @Override
        public String toString() {
            return String.format("Only present in expected value: %s=%s", path, expected);
        }
    }

    private static class JsonSet implements Iterable<Object> {
        private final Map<Object, Object> elementsByIdentifyingKey = new HashMap<>();

        private JsonSet(Set<Object> set) {
            for (Object element : set) {
                elementsByIdentifyingKey.put(keyOf(element), element);
            }
        }

        private Object keyOf(Object element) {
            if (element instanceof Map && ((Map) element).containsKey("fullname")) {
                return ((Map) element).get("fullname");
            } else {
                return element;
            }
        }

        static JsonSet from(Set<Object> set) {
            return new JsonSet(set);
        }

        @Override
        public Iterator<Object> iterator() {
            return elementsByIdentifyingKey.values().iterator();
        }

        boolean contains(Object value) {
            return elementsByIdentifyingKey.containsKey(keyOf(value));
        }

        Object get(Object value) {
            return elementsByIdentifyingKey.get(keyOf(value));
        }

        JsonSet subtract(JsonSet other) {
            Set<Object> rest = new HashSet<>();
            for (Object key : Sets.difference(getKeys(), other.getKeys())) {
                rest.add(elementsByIdentifyingKey.get(key));
            }
            return JsonSet.from(rest);
        }

        private Set<Object> getKeys() {
            return elementsByIdentifyingKey.keySet();
        }
    }
}
