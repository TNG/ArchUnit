package com.tngtech.archunit.htmlvisualization;

import java.io.File;
import java.io.IOException;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.htmlvisualization.testjson.structure.EmptyClass;
import com.tngtech.archunit.htmlvisualization.testjson.structure.complexinherit.ComplexClass1;
import com.tngtech.archunit.htmlvisualization.testjson.structure.complexinherit.ComplexClass2;
import com.tngtech.archunit.htmlvisualization.testjson.structure.simpleinherit.SimpleClass1;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.testutil.SystemPropertiesRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

public class VisualExtensionTest {

    @Rule
    public final SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File visualizationOutput;

    @Before
    public void configureOutputFile() throws IOException {
        visualizationOutput = configureTemporaryVisualizationOutput();
    }

    @Test
    public void evaluation_of_a_simple_rule() {
        JavaClasses classes = importSimpleAndComplexInheritance();
        checkRule(noClasses().should().callMethod(ComplexClass2.class, "sayHelloAndBye"), classes);

        VisualExtension.createVisualization(classes);

        String visualizationHtml = ResourcesUtils.getTextOfFile(visualizationOutput);
        assertThat(visualizationHtml).contains("\"default\"");
        assertThat(visualizationHtml).contains("\"no classes should call method ComplexClass2.sayHelloAndBye()\"");
    }

    @Test
    public void evaluation_of_several_rules_on_same_set_of_classes_contains_all_rule_visualizations() {
        JavaClasses classes = importSimpleAndComplexInheritance();
        checkRule(noClasses().should().callMethod(ComplexClass2.class, "sayHelloAndBye"), classes);
        checkRule(noClasses().should().callMethod(ComplexClass1.class, "sayHello"), classes);
        checkRule(noClasses().should().callMethod(SimpleClass1.class, "sayHi"), classes);

        VisualExtension.createVisualization(classes);

        String visualizationHtml = ResourcesUtils.getTextOfFile(visualizationOutput);
        assertThat(visualizationHtml).contains("\"default\"");
        assertThat(visualizationHtml).contains("\"no classes should call method SimpleClass1.sayHi()\"");
        assertThat(visualizationHtml).contains("\"no classes should call method ComplexClass1.sayHello()\"");
        assertThat(visualizationHtml).contains("\"no classes should call method ComplexClass2.sayHelloAndBye()\"");
    }

    @Test
    public void evaluation_of_several_rules_on_different_set_of_classes_includes_only_visualization_of_passed_classes() {
        JavaClasses complexStructure = importPackagesOf(ComplexClass2.class);
        checkRule(noClasses().should().callMethod(ComplexClass2.class, "sayHelloAndBye"), complexStructure);
        checkRule(noClasses().should().callMethod(SimpleClass1.class, "sayHi"), importPackagesOf(SimpleClass1.class));

        VisualExtension.createVisualization(complexStructure);

        String visualizationHtml = ResourcesUtils.getTextOfFile(visualizationOutput);
        assertThat(visualizationHtml).contains("\"default\"");
        assertThat(visualizationHtml).contains("\"no classes should call method ComplexClass2.sayHelloAndBye()\"");
        assertThat(visualizationHtml).doesNotContain("no classes should call method SimpleClass1.sayHi()");
    }

    private File configureTemporaryVisualizationOutput() throws IOException {
        File visualizationOutput = temporaryFolder.newFile("visualization.html");
        System.setProperty("archunit.htmlvisualization.targetfile", visualizationOutput.getAbsolutePath());
        return visualizationOutput;
    }

    private static JavaClasses importSimpleAndComplexInheritance() {
        return new ClassFileImporter().importPackagesOf(EmptyClass.class);
    }

    private static JavaClasses importPackagesOf(Class<?> clazz) {
        return new ClassFileImporter().importPackagesOf(clazz);
    }

    private void checkRule(ArchRule rule, JavaClasses allTestClasses) {
        try {
            rule.check(allTestClasses);
        } catch (AssertionError ignored) {
        }
    }
}