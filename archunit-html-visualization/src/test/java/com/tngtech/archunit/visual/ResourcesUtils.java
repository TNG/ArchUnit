package com.tngtech.archunit.visual;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.tngtech.archunit.base.Optional;
import org.assertj.guava.api.Assertions;
import org.assertj.guava.api.OptionalAssert;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

final class ResourcesUtils {

    // FIXME: We have to make a shadow Jar of archunit-visual to use the test support, but for that we need to refactor the shrinking process within archunit-junit and make it reusable
    static <T> OptionalAssert<T> assertThatOptional(Optional<T> optional) {
        return Assertions.assertThat(com.google.common.base.Optional.fromNullable(optional.orNull()));
    }

    static String getStringOfFile(File file) {
        byte[] encodedContent;
        try {
            encodedContent = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(encodedContent, Charsets.UTF_8);
    }

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
        Map<Object, Object> imported = importJsonObjectFromReader(reader);

        return ensureOrderIgnored(imported);
    }

    private static Map<Object, Object> importJsonObjectFromReader(JsonReader reader) {
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

    static File getResource(String name) {
        return new File(ResourcesUtils.class.getResource(name).getFile());
    }

    static String getResourceText(Class<?> resourceRelativeClass, String resourceName) {
        try (InputStream inputStream = resourceRelativeClass.getResourceAsStream(resourceName)) {
            return new String(ByteStreams.toByteArray(inputStream), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
