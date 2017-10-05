package com.tngtech.archunit.visual;

import java.io.File;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.visual.testclasses.SomeClass;
import com.tngtech.archunit.visual.testclasses.subpkg.SubPkgClass;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static org.assertj.core.api.Assertions.assertThat;

public class VisualizationContextTest {
    @Test
    public void default_VisualizationContext_includes_everything() {
        JavaClasses classes = importClasses(SomeClass.class, SubPkgClass.class, File.class);

        VisualizationContext defaultContext = new VisualizationContext.Builder().build();

        assertThat(defaultContext.isElementIncluded(classes.get(SomeClass.class)))
                .as(SomeClass.class.getSimpleName() + " is included").isTrue();
        assertThat(defaultContext.isElementIncluded(classes.get(File.class)))
                .as(SomeClass.class.getSimpleName() + " is included").isTrue();
        assertThat(defaultContext.filterIncluded(classes)).containsOnlyElementsOf(classes);
    }

    @Test
    public void configured_VisualizationContext_includes_only_specific_packages() {
        JavaClasses classes = importClasses(SomeClass.class, SubPkgClass.class, File.class);

        VisualizationContext defaultContext = new VisualizationContext.Builder()
                .includeOnly(SubPkgClass.class.getPackage().getName(), File.class.getPackage().getName())
                .build();

        assertThat(defaultContext.isElementIncluded(classes.get(SomeClass.class)))
                .as(SomeClass.class.getSimpleName() + " is included").isFalse();
        assertThat(defaultContext.isElementIncluded(classes.get(File.class)))
                .as(SomeClass.class.getSimpleName() + " is included").isTrue();
        assertThat(defaultContext.filterIncluded(classes))
                .containsOnly(classes.get(SubPkgClass.class), classes.get(File.class));
    }

    @Test
    public void included_packages_must_be_a_real_match() {
        JavaClasses classes = importClasses(SomeClass.class);

        String rightPackage = SomeClass.class.getPackage().getName();
        String incompleteSubString = rightPackage.substring(0, rightPackage.length() - 1);
        VisualizationContext defaultContext = new VisualizationContext.Builder()
                .includeOnly(incompleteSubString)
                .build();

        assertThat(defaultContext.isElementIncluded(classes.get(SomeClass.class)))
                .as(SomeClass.class.getSimpleName() + " is included").isFalse();
    }
}