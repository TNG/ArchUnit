package com.tngtech.archunit.visual;

import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TmpJsonUtils {
    private static JsonArray toJsonArray(Iterable<com.google.gson.JsonElement> iterable) {
        JsonArray result = new JsonArray();
        for (com.google.gson.JsonElement e : iterable) {
            result.add(e);
        }
        return result;
    }

    static String orderDependencies(String jsonExport) {
        JsonArray jsonArray = new JsonParser().parse(jsonExport).getAsJsonObject().get("dependencies").getAsJsonArray();

        List<com.google.gson.JsonElement> ordered = Lists.newArrayList(jsonArray);
        Collections.sort(ordered, new Comparator<com.google.gson.JsonElement>() {
            @Override
            public int compare(com.google.gson.JsonElement o1, com.google.gson.JsonElement o2) {
                return o1.getAsJsonObject().get("description").getAsString().compareTo(o2.getAsJsonObject().get("description").getAsString());
            }
        });

        return new GsonBuilder().create().toJson(toJsonArray(ordered));
    }

    static String orderViolations(String jsonViolations) {
        JsonArray jsonArray = (JsonArray) new JsonParser().parse(jsonViolations);

        List<com.google.gson.JsonElement> ordered = Lists.newArrayList(jsonArray);
        Collections.sort(ordered, new Comparator<com.google.gson.JsonElement>() {
            @Override
            public int compare(com.google.gson.JsonElement o1, com.google.gson.JsonElement o2) {
                return o1.getAsJsonObject().get("rule").getAsString().compareTo(o2.getAsJsonObject().get("rule").getAsString());
            }
        });

        for (com.google.gson.JsonElement rule : ordered) {
            List<com.google.gson.JsonElement> orderedDescriptions = Lists.newArrayList(rule.getAsJsonObject().get("violations").getAsJsonArray());
            Collections.sort(orderedDescriptions, new Comparator<com.google.gson.JsonElement>() {
                @Override
                public int compare(com.google.gson.JsonElement o1, JsonElement o2) {
                    return o1.getAsString().compareTo(o2.getAsString());
                }
            });
            rule.getAsJsonObject().remove("violations");
            rule.getAsJsonObject().add("violations", toJsonArray(orderedDescriptions));
        }

        return new GsonBuilder().create().toJson(toJsonArray(ordered));
    }
}
