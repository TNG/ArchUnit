package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.visual.testjson.structure.complexinherit.ComplexClass1;
import com.tngtech.archunit.visual.testjson.structure.complexinherit.ComplexClass2;
import com.tngtech.archunit.visual.testjson.structure.simpleinherit.SimpleClass1;
import org.assertj.core.api.Condition;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class VisualExtensionTest {

    private static final JavaClasses ALL_TEST_CLASSES = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure");
    private static final JavaClasses COMPLEX_INHERIT_CLASSES = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure.complexinherit");
    private static final JavaClasses SIMPLE_INHERIT_CLASSES = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure.simpleinherit");
    private static final File OUTPUT_DIR = new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile().getParentFile(), "visual-report-test");
    private static final File CLASSES_FILE = new File(OUTPUT_DIR, "classes.json");
    private static final File VIOLATION_FILE = new File(OUTPUT_DIR, "violations.json");
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

        assertThat(CLASSES_FILE).exists();
        assertThat(VIOLATION_FILE).exists();
        assertThat(REPORT_FILE).exists();
        assertThat(JsonTestUtils.jsonToMap(CLASSES_FILE))
                .as("created package structure")
                .containsValue("default");
        assertThat(JsonTestUtils.jsonToMapArray(VIOLATION_FILE))
                .as("violations")
                .areAtLeastOne(new Condition<Map<Object, Object>>() {
                    @Override
                    public boolean matches(Map<Object, Object> value) {
                        return value.containsValue("no classes should call method ComplexClass2.sayHelloAndBye()");
                    }
                });
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

        assertThat(CLASSES_FILE).exists();
        assertThat(VIOLATION_FILE).exists();
        assertThat(REPORT_FILE).exists();
        assertThat(JsonTestUtils.jsonToMap(CLASSES_FILE))
                .as("created package structure")
                .containsValue("default");
        assertThat(JsonTestUtils.jsonToMapArray(VIOLATION_FILE))
                .as("violations")
                .areAtLeastOne(new Condition<Map<Object, Object>>() {
                    @Override
                    public boolean matches(Map<Object, Object> value) {
                        return value.containsValue("no classes should call method SimpleClass1.sayHi()");
                    }
                })
                .areAtLeastOne(new Condition<Map<Object, Object>>() {
                    @Override
                    public boolean matches(Map<Object, Object> value) {
                        return value.containsValue("no classes should call method ComplexClass1.sayHello()");
                    }
                })
                .areAtLeastOne(new Condition<Map<Object, Object>>() {
                    @Override
                    public boolean matches(Map<Object, Object> value) {
                        return value.containsValue("no classes should call method ComplexClass2.sayHelloAndBye()");
                    }
                });
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

        assertThat(CLASSES_FILE).exists();
        assertThat(VIOLATION_FILE).exists();
        assertThat(REPORT_FILE).exists();
        assertThat(JsonTestUtils.jsonToMap(CLASSES_FILE))
                .as("created package structure")
                .containsValue("default");
        assertThat(JsonTestUtils.jsonToMapArray(VIOLATION_FILE))
                .as("violations")
                .areAtLeastOne(new Condition<Map<Object, Object>>() {
                    @Override
                    public boolean matches(Map<Object, Object> value) {
                        return value.containsValue("no classes should call method ComplexClass2.sayHelloAndBye()");
                    }
                })
                .areNot(new Condition<Map<Object, Object>>() {
                    @Override
                    public boolean matches(Map<Object, Object> value) {
                        return value.containsValue("no classes should call method SimpleClass1.sayHi()");
                    }
                });
    }
}