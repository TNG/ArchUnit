package com.tngtech.archunit.visual;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
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

        jsonExporter.export(classes, target, new VisualizationContextBuilder()
                .includeOnly("com.tngtech.archunit.visual.testjson").build());

        File expectedJson = JsonConverter.getJsonFile("./testjson/empty-class.json");
        assertThat(JsonConverter.jsonToMap(target)).as("exported json")
                .isEqualTo(JsonConverter.jsonToMap(expectedJson));
    }

    @Test
    public void exports_empty_class_including_everything() throws Exception {
        JavaClasses classes = importClasses(EmptyClass.class);
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, new VisualizationContextBuilder()
                .includeEverything().build());

        File expectedJson = JsonConverter.getJsonFile("./testjson/empty-class-everything.json");
        assertThat(JsonConverter.jsonToMap(target)).as("exported json")
                .isEqualTo(JsonConverter.jsonToMap(expectedJson));
    }

    @Test
    public void exports_simple_inherit_structure_ignoring_access_to_super_constructor() throws Exception {
        JavaClasses classes = importClasses(Class1.class, Class2.class, Class3.class, Interface1.class,
                EmptyClass.class);
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, new VisualizationContextBuilder()
                .ignoreAccessToSuperConstructor()
                .includeOnly("com.tngtech.archunit.visual.testjson").build());

        File expectedJson = JsonConverter.getJsonFile("./testjson/simpleinheritstructure1.json");
        assertThat(JsonConverter.jsonToMap(target)).as("exported json")
                .isEqualTo(JsonConverter.jsonToMap(expectedJson));
    }

    @Test
    public void exports_simple_inherit_structure() throws Exception {
        JavaClasses classes = importClasses(Class1.class, Class2.class, Class3.class, Interface1.class,
                EmptyClass.class);
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, new VisualizationContextBuilder()
                .includeOnly("com.tngtech.archunit.visual.testjson").build());

        File expectedJson = JsonConverter.getJsonFile("./testjson/simpleinheritstructure2.json");
        assertThat(JsonConverter.jsonToMap(target)).as("exported json")
                .isEqualTo(JsonConverter.jsonToMap(expectedJson));
    }

    @Test
    public void exports_complex_inherit_structure() throws Exception {
        JavaClasses classes = new ClassFileImporter().importUrl(new File(getClass().getResource(
                "/" + EmptyClass.class.getName().replace('.', '/') + ".class")
                .getFile()).getParentFile().toPath().toUri().toURL());
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, new VisualizationContextBuilder()
                .includeOnly("com.tngtech.archunit.visual.testjson")
                .ignoreAccessToSuperConstructor().build());

        File expectedJson = new File(getClass().getResource("./testjson/complexinheritstructure.json").getFile());
        assertThat(JsonConverter.jsonToMap(target)).as("exported json").isEqualTo(JsonConverter.jsonToMap(expectedJson));
    }

    private JavaClasses importClasses(Class<?>... classes) {
        List<URL> urls = new ArrayList<>();
        for (Class<?> c : classes) {
            urls.add(c.getResource("/" + c.getName().replace(".", "/") + ".class"));
        }
        return new ClassFileImporter().importUrls(urls);
    }
}