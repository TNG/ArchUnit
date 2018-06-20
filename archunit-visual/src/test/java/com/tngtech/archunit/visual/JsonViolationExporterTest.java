package com.tngtech.archunit.visual;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class JsonViolationExporterTest {

    private final JsonViolationExporter exporter = new JsonViolationExporter();

    @Test
    public void testExportProducesCorrectOutput() throws Exception {
        JavaClasses classes = new ClassFileImporter().importClasses(Accessor.class);
        ArchRule rule = ArchRuleDefinition.noClasses()
                .should().accessClassesThat().areAssignableTo(Target.class);
        EvaluationResult result = rule.evaluate(classes);

        String json = exporter.exportToJson(Arrays.asList(result));

        String expectedJson = jsonFromFile("access-violations.json");
        JSONAssert.assertEquals(expectedJson, json, false);
    }

    @Test
    public void exportViolationsOfDifferentRules() throws Exception {
        JavaClasses classes = new ClassFileImporter().importClasses(Accessor.class);
        ArchRule rule1 = ArchRuleDefinition.noClasses().should().accessField(Target.class, "field1");
        EvaluationResult result1 = rule1.evaluate(classes);

        ArchRule rule2 = ArchRuleDefinition.noClasses().should().accessField(Target.class, "field2");
        EvaluationResult result2 = rule2.evaluate(classes);

        String json = exporter.exportToJson(Arrays.asList(result1, result2));

        String expectedJson = jsonFromFile("access-violations-of-different-rules.json");
        JSONAssert.assertEquals(expectedJson, json, false);
    }

    private String jsonFromFile(String fileName) throws IOException {
        File jsonFile = ResourcesUtils.getResource("testjson/violation/" + fileName);
        return Files.toString(jsonFile, Charsets.UTF_8);
    }

    public static class Accessor {
        public void simpleMethodCall() {
            new Target().method();
        }

        public void complexMethodCall(String foo, Object bar) {
            new Target().complexMethod(foo, bar, this);
        }

        public void fieldAccess1(Target target) {
            target.field1 = "accessed";
        }

        public void fieldAccess2(Target target) {
            target.field2 = "accessed";
        }
    }

    public static class Target {
        public String field1 = "";
        public String field2 = "";

        public void method() {
        }

        public void complexMethod(String foo, Object bar, Accessor self) {
        }
    }

}