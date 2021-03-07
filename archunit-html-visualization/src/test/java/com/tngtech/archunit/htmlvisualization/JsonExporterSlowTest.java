package com.tngtech.archunit.htmlvisualization;

import java.io.File;

import com.tngtech.archunit.Slow;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.ImportOptions;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.htmlvisualization.JsonStringAssertion.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

@Category(Slow.class)
public class JsonExporterSlowTest {

    @Rule
    public final ArchConfigurationRule rule = new ArchConfigurationRule().resolveAdditionalDependenciesFromClassPath(true);

    private final JsonExporter jsonExporter = new JsonExporter();

    @Test
    public void exports_all_classes_from_the_classpath() {
        JavaClasses classes = new ClassFileImporter().importClasspath(new ImportOptions().with(new ImportOption() {
            @Override
            public boolean includes(Location location) {
                return location.contains("/java.base/") || location.contains("/rt.jar") || location.contains("/archunit/");
            }
        }));
        assertThat(classes.contain(File.class)).as(File.class.getName() + " was imported").isTrue();
        assertThat(classes.contain(ClassFileImporter.class)).as(ClassFileImporter.class.getName() + " was imported").isTrue();
        assertThat(classes.size()).as("Number of classes imported from the classpath").isGreaterThan(5000);

        String result = jsonExporter.exportToJson(classes);

        assertThatJson(result).contains(File.class.getName());
        assertThatJson(result).contains(ClassFileImporter.class.getName());
    }
}
