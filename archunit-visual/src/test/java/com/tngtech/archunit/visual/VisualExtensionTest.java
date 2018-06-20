package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.visual.testjson.structure.complexinherit.ComplexClass1;
import com.tngtech.archunit.visual.testjson.structure.complexinherit.ComplexClass2;
import com.tngtech.archunit.visual.testjson.structure.simpleinherit.SimpleClass1;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class VisualExtensionTest {

    private static final JavaClasses ALL_TEST_CLASSES = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure");
    private static final JavaClasses COMPLEX_INHERIT_CLASSES = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure.complexinherit");
    private static final JavaClasses SIMPLE_INHERIT_CLASSES = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure.simpleinherit");
    private static final File OUTPUT_DIR = new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile().getParentFile(), "visual-report-test");
    private static final File REPORT_FILE = new File(OUTPUT_DIR, "report.html");

    private static final ArchRule NO_CLASSES_SHOULD_CALL_METHOD_1 = ArchRuleDefinition.noClasses().should().callMethod(ComplexClass2.class, "sayHelloAndBye");
    private static final ArchRule NO_CLASSES_SHOULD_CALL_METHOD_2 = ArchRuleDefinition.noClasses().should().callMethod(ComplexClass1.class, "sayHello");
    private static final ArchRule NO_CLASSES_SHOULD_CALL_METHOD_3 = ArchRuleDefinition.noClasses().should().callMethod(SimpleClass1.class, "sayHi");

    @BeforeClass
    public static void setupClass() {
        System.setProperty("archunit.visual.report.dir", OUTPUT_DIR.getAbsolutePath());
    }

    @Test
    public void testEvaluateSingleRule() {
        try {
            NO_CLASSES_SHOULD_CALL_METHOD_1.check(ALL_TEST_CLASSES);
        } catch (AssertionError e) {
        }
        VisualExtension.createVisualization(ALL_TEST_CLASSES);

        assertThat(REPORT_FILE).exists();
        String reportContent = ResourcesUtils.getStringOfFile(REPORT_FILE);
        assertThat(reportContent).contains("\"default\"");
        assertThat(reportContent).contains("\"no classes should call method ComplexClass2.sayHelloAndBye()\"");
    }

    @Test
    public void testEvaluateSeveralRulesOnSameClasses() {
        try {
            NO_CLASSES_SHOULD_CALL_METHOD_1.check(ALL_TEST_CLASSES);
        } catch (AssertionError e) {
        }
        try {
            NO_CLASSES_SHOULD_CALL_METHOD_2.check(ALL_TEST_CLASSES);
        } catch (AssertionError e) {
        }
        try {
            NO_CLASSES_SHOULD_CALL_METHOD_3.check(ALL_TEST_CLASSES);
        } catch (AssertionError e) {
        }
        VisualExtension.createVisualization(ALL_TEST_CLASSES);

        String reportContent = ResourcesUtils.getStringOfFile(REPORT_FILE);
        assertThat(REPORT_FILE).exists();
        assertThat(reportContent).contains("\"default\"");
        assertThat(reportContent).contains("\"no classes should call method SimpleClass1.sayHi()\"");
        assertThat(reportContent).contains("\"no classes should call method ComplexClass1.sayHello()\"");
        assertThat(reportContent).contains("\"no classes should call method ComplexClass2.sayHelloAndBye()\"");
    }

    @Test
    public void testEvaluateTwoRulesOnDifferentClasses() {
        try {
            NO_CLASSES_SHOULD_CALL_METHOD_1.check(COMPLEX_INHERIT_CLASSES);
        } catch (AssertionError e) {
        }
        try {
            NO_CLASSES_SHOULD_CALL_METHOD_3.check(SIMPLE_INHERIT_CLASSES);
        } catch (AssertionError e) {
        }
        VisualExtension.createVisualization(COMPLEX_INHERIT_CLASSES);

        String reportContent = ResourcesUtils.getStringOfFile(REPORT_FILE);
        assertThat(REPORT_FILE).exists();
        assertThat(reportContent).contains("\"default\"");
        assertThat(reportContent).contains("\"no classes should call method ComplexClass2.sayHelloAndBye()\"");
        assertThat(reportContent).doesNotContain("\"\"no classes should call method SimpleClass1.sayHi()\"\"");
    }
}