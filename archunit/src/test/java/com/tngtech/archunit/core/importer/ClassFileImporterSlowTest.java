package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;

import com.tngtech.archunit.Slow;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.testutil.TransientCopyRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.core.SourceTest.urlOf;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

@Category(Slow.class)
public class ClassFileImporterSlowTest {
    @Rule
    public final TransientCopyRule copyRule = new TransientCopyRule();

    @Test
    public void importing_the_classpath() {
        JavaClasses classes = new ClassFileImporter().importClasspath(new ImportOptions());

        assertThat(classes.contain(Object.class));
    }

    @Test
    public void imports_duplicate_classes() throws IOException {
        String existingClass = urlOf(JavaClass.class).getFile();
        copyRule.copy(
                new File(existingClass),
                new File(getClass().getResource(".").getFile()));

        JavaClasses classes = new ClassFileImporter().importPackages(getClass().getPackage().getName());

        assertThat(classes.get(JavaClass.class)).isNotNull();
    }
}
