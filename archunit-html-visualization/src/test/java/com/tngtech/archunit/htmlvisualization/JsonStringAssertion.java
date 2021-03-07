package com.tngtech.archunit.htmlvisualization;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.FluentIterable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.htmlvisualization.ResourcesUtils.getResourceText;
import static org.assertj.core.api.Assertions.assertThat;

class JsonStringAssertion extends AbstractObjectAssert<JsonStringAssertion, String> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonStringAssertion(String jsonString) {
        super(jsonString, JsonStringAssertion.class);
    }

    static JsonStringAssertion assertThatJson(String jsonString) {
        return new JsonStringAssertion(jsonString);
    }

    JsonStringAssertion contains(String jsonString) {
        assertThat(actual).contains(jsonString);
        return myself;
    }

    JsonStringAssertion matches(String jsonString) {
        assertThat(sortJson(actual)).isEqualTo(sortJson(jsonString));
        return myself;
    }

    JsonStringAssertion matchesResource(Class<?> resourceRoot, String relativeResourcePath) {
        checkArgument(!relativeResourcePath.startsWith("/"), "Resource path %s is not relative", relativeResourcePath);
        return matches(getResourceText(resourceRoot, relativeResourcePath));
    }

    private String sortJson(String actualJson) {
        JsonElement parsed = GSON.fromJson(actualJson, JsonElement.class);
        return GSON.toJson(sortRecursively(parsed));
    }

    private JsonElement sortRecursively(JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            return sortJsonObject(jsonElement.getAsJsonObject());
        } else if (jsonElement.isJsonArray()) {
            return sortJsonArray(jsonElement.getAsJsonArray());
        }
        return jsonElement;
    }

    private JsonElement sortJsonObject(JsonObject jsonObject) {
        JsonObject object = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : sortKeys(jsonObject.entrySet())) {
            object.add(entry.getKey(), sortRecursively(entry.getValue()));
        }
        return object;
    }

    private JsonElement sortJsonArray(JsonArray jsonArray) {
        JsonArray array = new JsonArray();
        for (JsonElement element : FluentIterable.from(jsonArray).toSortedList(jsonArraySorter())) {
            array.add(sortRecursively(element));
        }
        return array;
    }

    private Comparator<JsonElement> jsonArraySorter() {
        return new Comparator<JsonElement>() {
            @Override
            public int compare(JsonElement o1, JsonElement o2) {
                if (o1.isJsonPrimitive()) {
                    return String.valueOf(o1).compareTo(String.valueOf(o2));
                }
                return o1.getAsJsonObject().get("fullName").getAsString().compareTo(o2.getAsJsonObject().get("fullName").getAsString());
            }
        };
    }

    private Iterable<? extends Map.Entry<String, JsonElement>> sortKeys(Set<Map.Entry<String, JsonElement>> entrySet) {
        TreeMap<String, JsonElement> result = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result.entrySet();
    }
}
