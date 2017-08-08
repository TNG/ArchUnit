package com.tngtech.archunit.visual;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.visual.testjson.structure.EmptyClass;
import com.tngtech.archunit.visual.testjson.structure.simpleinherit.SimpleClass1;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import some.other.OtherClass;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class JsonExporterTest {
    private final JsonExporter jsonExporter = new JsonExporter();

    private final StringWriter writer = new StringWriter();

    @Test
    public void exports_empty_class() throws Exception {
        JavaClasses classes = new ClassFileImporter().importClasses(EmptyClass.class, OtherClass.class);

        jsonExporter.export(classes, writer, new VisualizationContext.Builder()
                .includeOnly("com.tngtech.archunit.visual.testjson.structure").build());

        assertWriterContainsJsonEqualToFile("empty-class.json");
    }

    @Test
    public void exports_empty_class_including_everything() throws Exception {
        JavaClasses classes = new ClassFileImporter().importClasses(EmptyClass.class, OtherClass.class);

        jsonExporter.export(classes, writer, new VisualizationContext.Builder().build());

        assertWriterContainsJsonEqualToFile("empty-class-everything.json");
    }

    @Test
    public void exports_simple_inherit_structure() throws Exception {
        JavaClasses classes = importClassesThatAreInPackagesOf(EmptyClass.class, SimpleClass1.class);

        jsonExporter.export(classes, writer, new VisualizationContext.Builder()
                .includeOnly("com.tngtech.archunit.visual.testjson").build());

        assertWriterContainsJsonEqualToFile("simpleinheritstructure.json");
    }

    @Test
    public void exports_simple_inherit_structure_including_two_packages() throws Exception {
        JavaClasses classes = importClassesThatAreInPackagesOf(EmptyClass.class, SimpleClass1.class);

        jsonExporter.export(classes, writer, new VisualizationContext.Builder().includeOnly("com.tngtech.archunit.visual.testjson", "java.io").build());

        assertWriterContainsJsonEqualToFile("simpleinheritstructure_includetwopackages.json");
    }

    @Test
    public void exports_complex_inherit_structure() throws Exception {
        JavaClasses classes = new ClassFileImporter().importPackages(EmptyClass.class.getPackage().getName());

        jsonExporter.export(classes, writer, new VisualizationContext.Builder()
                .includeOnly("com.tngtech.archunit.visual.testjson").build());

        assertWriterContainsJsonEqualToFile("complexinheritstructure.json");
    }

    private JavaClasses importClassesThatAreInPackagesOf(Class... classes) {
        return new ClassFileImporter().importPackages(getClass().getPackage().getName() + ".testjson")
                .that(areInPackagesOf(classes));
    }

    private DescribedPredicate<JavaClass> areInPackagesOf(final Class... packagesOf) {
        final List<String> chosenPackages = new ArrayList<>();
        for (Class c : packagesOf) {
            chosenPackages.add(c.getPackage().getName());
        }
        return new DescribedPredicate<JavaClass>("are in packages of " + JavaClass.namesOf(packagesOf)) {
            @Override
            public boolean apply(JavaClass input) {
                return chosenPackages.contains(input.getPackage());
            }
        };
    }

    private void assertWriterContainsJsonEqualToFile(String fileName) throws IOException, JSONException {
        File jsonFile = JsonTestUtils.getJsonFile("testjson/structure/" + fileName);
        String expectedJson = Files.toString(jsonFile, Charsets.UTF_8);
        JSONAssert.assertEquals(expectedJson, writer.toString(), false);
    }
}