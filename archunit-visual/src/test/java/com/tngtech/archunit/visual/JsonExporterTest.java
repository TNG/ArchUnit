package com.tngtech.archunit.visual;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.visual.testjson.structure.EmptyClass;
import com.tngtech.archunit.visual.testjson.structure.simpleinherit.SimpleClass1;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import some.other.OtherClass;

import static com.tngtech.archunit.visual.JsonAssertions.assertThatJsonIn;

public class JsonExporterTest {
    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private final JsonExporter jsonExporter = new JsonExporter();

    @Test
    public void exports_empty_class() throws Exception {
        JavaClasses classes = new ClassFileImporter().importClasses(EmptyClass.class, OtherClass.class);
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, new VisualizationContext.Builder()
                .includeOnly("com.tngtech.archunit.visual.testjson.structure").build());

        File expectedJson = expectJson("empty-class.json");
        assertThatJsonIn(target).isEquivalentToJsonIn(expectedJson);
    }

    @Test
    public void exports_empty_class_including_everything() throws Exception {
        JavaClasses classes = new ClassFileImporter().importClasses(EmptyClass.class, OtherClass.class);
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, new VisualizationContext.Builder()
                .includeEverything().build());

        File expectedJson = expectJson("empty-class-everything.json");
        assertThatJsonIn(target).isEquivalentToJsonIn(expectedJson);
    }

    @Test
    public void simple_inheritance_structure_ignoring_access_to_super_constructor() throws Exception {
        JavaClasses classes = importClassesThatAreInPackagesOf(EmptyClass.class, SimpleClass1.class);
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, new VisualizationContext.Builder()
                .ignoreAccessToSuperConstructor()
                .includeOnly("com.tngtech.archunit.visual.testjson.structure").build());

        File expectedJson = expectJson("simpleinheritstructure1.json");
        assertThatJsonIn(target).isEquivalentToJsonIn(expectedJson);
    }

    @Test
    public void exports_simple_inherit_structure() throws Exception {
        JavaClasses classes = importClassesThatAreInPackagesOf(EmptyClass.class, SimpleClass1.class);
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, new VisualizationContext.Builder()
                .includeOnly("com.tngtech.archunit.visual.testjson").build());

        File expectedJson = expectJson("simpleinheritstructure2.json");
        assertThatJsonIn(target).isEquivalentToJsonIn(expectedJson);
    }

    @Test
    public void exports_complex_inherit_structure() throws Exception {
        JavaClasses classes = new ClassFileImporter().importPackages(EmptyClass.class.getPackage().getName());
        File target = tmpDir.newFile("test.json");

        jsonExporter.export(classes, target, new VisualizationContext.Builder()
                .includeOnly("com.tngtech.archunit.visual.testjson")
                .ignoreAccessToSuperConstructor().build());

        File expectedJson = expectJson("complexinheritstructure.json");
        assertThatJsonIn(target).isEquivalentToJsonIn(expectedJson);
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

    private File expectJson(String fileName) {
        return JsonTestUtils.getJsonFile("testjson/structure/" + fileName);
    }
}