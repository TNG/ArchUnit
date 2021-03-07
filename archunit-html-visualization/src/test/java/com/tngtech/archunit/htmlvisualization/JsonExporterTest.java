package com.tngtech.archunit.htmlvisualization;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.htmlvisualization.testjson.EmptyClassInPackageRoot;
import com.tngtech.archunit.htmlvisualization.testjson.simpleinherit.SimpleInterface;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Rule;
import org.junit.Test;
import some.other.ClassInDifferentPackageRoot;

import static com.tngtech.archunit.htmlvisualization.JsonStringAssertion.assertThatJson;

public class JsonExporterTest {

    @Rule
    public final ArchConfigurationRule rule = new ArchConfigurationRule().resolveAdditionalDependenciesFromClassPath(false);

    private final JsonExporter jsonExporter = new JsonExporter();

    @Test
    public void exports_simple_classes_without_inheritance() {
        JavaClasses classes = new ClassFileImporter().importClasses(EmptyClassInPackageRoot.class, ClassInDifferentPackageRoot.class);

        String result = jsonExporter.exportToJson(classes);

        assertThatJson(result).matchesResource(getClass(), "testjson/two-classes-in-different-roots.json");
    }

    @Test
    public void exports_inheritance_structure_with_interfaces_and_inner_classes() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(SimpleInterface.class);

        String result = jsonExporter.exportToJson(classes);

        assertThatJson(result).matchesResource(getClass(), "testjson/simple-case-with-interface-and-inner-class.json");
    }

    @Test
    public void exports_complex_inheritances_and_inner_classes() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(EmptyClassInPackageRoot.class);

        String result = jsonExporter.exportToJson(classes);

        assertThatJson(result).matchesResource(getClass(), "testjson/complex-case-with-multiple-inheritances-and-dependencies.json");
    }
}
