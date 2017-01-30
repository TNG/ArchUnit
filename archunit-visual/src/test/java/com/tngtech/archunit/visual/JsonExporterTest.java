package com.tngtech.archunit.visual;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.visual.testjson.EmptyClass;
import com.tngtech.archunit.visual.testjson.simpleInheritStructure.Class1;
import com.tngtech.archunit.visual.testjson.simpleInheritStructure.Class2;
import com.tngtech.archunit.visual.testjson.simpleInheritStructure.Class3;
import com.tngtech.archunit.visual.testjson.simpleInheritStructure.Interface1;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonExporterTest {
    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private final JsonExporter jsonExporter = new JsonExporter();

    @Test
    public void exports_empty_class() throws Exception {
        JavaClasses classes = importClasses(EmptyClass.class);
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, "com.tngtech.archunit.visual.testjson");

        File expectedJson = new File(getClass().getResource("./testjson/empty-class.json").getFile());
        assertThat(importJson(target)).as("exported json").isEqualTo(importJson(expectedJson));
    }

    @Test
    public void exports_simple_inherit_structure() throws Exception {
        JavaClasses classes = importClasses(Class1.class, Class2.class, Class3.class, Interface1.class, EmptyClass.class);
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, "com.tngtech.archunit.visual.testjson");

        File expectedJson = new File(getClass().getResource("./testjson/simpleinheritstructure.json").getFile());
        assertThat(importJson(target)).as("exported json").isEqualTo(importJson(expectedJson));
    }

    @Test
    public void exports_complex_inherit_structure() throws Exception {
        JavaClasses classes = importClasses(Class1.class, Class2.class, Class3.class, Interface1.class,
                EmptyClass.class, com.tngtech.archunit.visual.testjson.complexInheritStructure.Class1.class,
                com.tngtech.archunit.visual.testjson.complexInheritStructure.Class2.class,
                com.tngtech.archunit.visual.testjson.complexInheritStructure.Class3.class,
                com.tngtech.archunit.visual.testjson.complexInheritStructure.Interface1.class,
                com.tngtech.archunit.visual.testjson.complexInheritStructure.Interface2.class);
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, "com.tngtech.archunit.visual.testjson");

        File expectedJson = new File(getClass().getResource("./testjson/complexinheritstructure.json").getFile());
        assertThat(importJson(target)).as("exported json").isEqualTo(importJson(expectedJson));
    }

    private Map<Object, Object> importJson(File file) {
        JsonReader reader;
        try {
            reader = new JsonReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Map<Object, Object> imported = new Gson().fromJson(reader, new TypeToken<Map<String, Object>>() {
        }.getType());

        return ensureOrderIgnored(imported);
    }

    @SuppressWarnings("unchecked")
    private <T> T ensureOrderIgnored(T imported) {
        if (imported instanceof Map) {
            return (T) makeHashMap((Map<?, ?>) imported);
        } else if (imported instanceof List) {
            return (T) makeSet((List) imported);
        }
        return imported;
    }

    private Map<?, ?> makeHashMap(Map<?, ?> map) {
        Map<Object, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(entry.getKey(), ensureOrderIgnored(entry.getValue()));
        }
        return result;
    }

    private Set<Object> makeSet(List<?> list) {
        Set<Object> set = new HashSet<>();
        for (Object elem : list) {
            set.add(ensureOrderIgnored(elem));
        }
        return set;
    }

    private JavaClasses importClasses(Class<?>... classes) {
        List<URL> urls = new ArrayList<>();
        for (Class<?> c : classes) {
            urls.add(c.getResource("/" + c.getName().replace(".", "/") + ".class"));
        }
        return new ClassFileImporter().importUrls(urls);
    }
}