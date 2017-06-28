package com.tngtech.archunit.visual;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import static com.google.common.base.Preconditions.checkNotNull;

final class JsonTestUtils {

    static String getJsonStringOf(JsonElement element) {
        final GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();
        return gson.toJson(element);
    }

    static Map<Object, Object> jsonToMap(File file) {
        JsonReader reader;
        try {
            reader = new JsonReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Map<Object, Object> imported = importJsonFromReader(reader);

        return ensureOrderIgnored(imported);
    }

    private static Map<Object, Object> importJsonFromReader(JsonReader reader) {
        return checkNotNull(new Gson().<Map<Object, Object>>fromJson(reader, mapStringObjectType()));
    }

    private static Type mapStringObjectType() {
        return new TypeToken<Map<String, Object>>() {
        }.getType();
    }

    static Map<Object, Object> jsonToMap(String json) {
        Map<Object, Object> imported = new Gson().fromJson(json, mapStringObjectType());

        return ensureOrderIgnored(imported);
    }

    @SuppressWarnings("unchecked")
    private static <T> T ensureOrderIgnored(T imported) {
        if (imported instanceof Map) {
            return (T) makeHashMap((Map<?, ?>) imported);
        } else if (imported instanceof List) {
            return (T) makeSet((List) imported);
        }
        return imported;
    }

    private static Map<?, ?> makeHashMap(Map<?, ?> map) {
        Map<Object, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(entry.getKey(), ensureOrderIgnored(entry.getValue()));
        }
        return result;
    }

    private static Set<Object> makeSet(List<?> list) {
        Set<Object> set = new HashSet<>();
        for (Object elem : list) {
            set.add(ensureOrderIgnored(elem));
        }
        return set;
    }

    static File getJsonFile(String name) {
        return new File(JsonTestUtils.class.getResource(name).getFile());
    }
}
