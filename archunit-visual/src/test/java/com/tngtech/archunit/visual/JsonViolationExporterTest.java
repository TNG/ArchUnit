package com.tngtech.archunit.visual;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

public class JsonViolationExporterTest {

    private final JsonViolationExporter exporter = new JsonViolationExporter();

    private final StringWriter writer = new StringWriter();

    @Test
    public void testExportProducesCorrectOutput() throws Exception {
        JavaClasses classes = new ClassFileImporter().importClasses(Accessor.class);
        EvaluationResult result = ArchRuleDefinition.noClasses()
                .should().accessClassesThat().areAssignableTo(Target.class)
                .evaluate(classes);

        exporter.export(result, writer);

        String expectedJson = jsonFromFile("access-violations.json");
        JSONAssert.assertEquals(expectedJson, writer.toString(), false);
    }

    private String jsonFromFile(String fileName) throws IOException {
        File jsonFile = JsonTestUtils.getJsonFile("testjson/violation/" + fileName);
        return Files.toString(jsonFile, Charsets.UTF_8);
    }

    public static class Accessor {
        public void simpleMethodCall() {
            new Target().method();
        }

        public void complexMethodCall(String foo, Object bar) {
            new Target().complexMethod(foo, bar, this);
        }

        public void fieldAccess(Target target) {
            target.field = "accessed";
        }
    }

    public static class Target {
        public String field = "";

        public void method() {
        }

        public void complexMethod(String foo, Object bar, Accessor self) {
        }
    }

}