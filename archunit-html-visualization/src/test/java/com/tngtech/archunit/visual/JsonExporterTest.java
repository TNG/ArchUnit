package com.tngtech.archunit.visual;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.visual.testjson.structure.EmptyClass;
import com.tngtech.archunit.visual.testjson.structure.simpleinherit.SimpleClass1;
import com.tngtech.archunit.visual.testjson.violations.Accessor;
import com.tngtech.archunit.visual.testjson.violations.Target;
import org.junit.Test;
import some.other.OtherClass;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.visual.ResourcesUtils.getResourceText;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonExporterTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final JsonExporter jsonExporter = new JsonExporter();

    @Test
    public void exports_empty_class() {
        JavaClasses classes = new ClassFileImporter().importClasses(EmptyClass.class, OtherClass.class);

        String result = jsonExporter.exportToJson(classes);

        assertClassesStructureJsonEqualToFile(result, "empty-class.json");
    }

    @Test
    public void exports_simple_inherit_structure() {
        JavaClasses classes = importClassesThatAreInPackagesOf(EmptyClass.class, SimpleClass1.class);

        String result = jsonExporter.exportToJson(classes);

        assertClassesStructureJsonEqualToFile(result, "simpleinheritstructure.json");
    }

    @Test
    public void exports_complex_inherit_structure() {
        JavaClasses classes = new ClassFileImporter().importPackages(EmptyClass.class.getPackage().getName());

        String result = jsonExporter.exportToJson(classes);

        assertClassesStructureJsonEqualToFile(result, "complexinheritstructure.json");
    }

    @Test
    public void exports_violations_of_a_simple_rule() {
        JavaClasses classes = new ClassFileImporter().importClasses(Accessor.class);
        ArchRule rule = noClasses().should().accessClassesThat().areAssignableTo(Target.class);
        EvaluationResult result = rule.evaluate(classes);

        String json = jsonExporter.exportToJson(singletonList(result));

        assertViolationsJsonEqualToFile(json, "access-violations.json");
    }

    @Test
    public void exports_violations_of_two_different_rules() {
        JavaClasses classes = new ClassFileImporter().importClasses(Accessor.class);
        ArchRule rule1 = noClasses().should().accessField(Target.class, "field1");
        EvaluationResult result1 = rule1.evaluate(classes);

        ArchRule rule2 = noClasses().should().accessField(Target.class, "field2");
        EvaluationResult result2 = rule2.evaluate(classes);

        String json = jsonExporter.exportToJson(Arrays.asList(result1, result2));

        assertViolationsJsonEqualToFile(json, "access-violations-of-different-rules.json");
    }

    @Test
    public void exports_non_existing_violation() {
        JavaClasses classes = new ClassFileImporter().importClasses(Accessor.class);
        ArchRule rule = noClasses().should().bePublic();
        EvaluationResult result = rule.evaluate(classes);

        String json = jsonExporter.exportToJson(singletonList(result));

        assertThat(json).isEqualTo("[]");
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
                return chosenPackages.contains(input.getPackageName());
            }
        };
    }

    private void assertClassesStructureJsonEqualToFile(String actualJson, String expectedJsonFileName) {
        String expectedJson = getResourceText(getClass(), "testjson/structure/" + expectedJsonFileName);
        assertThat(sortJson(actualJson)).isEqualTo(sortJson(expectedJson));
    }

    private void assertViolationsJsonEqualToFile(String actualJson, String expectedJsonFileName) {
        String expectedJson = getResourceText(getClass(), "testjson/violations/" + expectedJsonFileName);
        assertThat(sortJson(actualJson)).isEqualTo(sortJson(expectedJson));
    }

    private String sortJson(String actualJson) {
        JsonElement parsed = GSON.fromJson(actualJson, JsonElement.class);
        return GSON.toJson(sortRecursively(parsed));
    }

    private JsonElement sortRecursively(JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            return sortJsonObject(jsonElement.getAsJsonObject());
        } else if (jsonElement.isJsonArray()) {
            return sortJsonArray(jsonElement.getAsJsonArray());
        }
        return jsonElement;
    }

    private JsonElement sortJsonObject(JsonObject jsonObject) {
        JsonObject object = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : sortKeys(jsonObject.entrySet())) {
            object.add(entry.getKey(), sortRecursively(entry.getValue()));
        }
        return object;
    }

    private JsonElement sortJsonArray(JsonArray jsonArray) {
        JsonArray array = new JsonArray();
        for (JsonElement element : FluentIterable.from(jsonArray).toSortedList(jsonArraySorter())) {
            array.add(sortRecursively(element));
        }
        return array;
    }

    private Comparator<JsonElement> jsonArraySorter() {
        return new Comparator<JsonElement>() {
            @Override
            public int compare(JsonElement o1, JsonElement o2) {
                if (o1.isJsonPrimitive()) {
                    return String.valueOf(o1).compareTo(String.valueOf(o2));
                }

                return ComparisonChain.start()
                        .compare(o1, o2, compareOptionalProperty("fullName"))
                        .compare(o1, o2, compareOptionalProperty("description"))
                        .compare(o1, o2, compareOptionalProperty("startCodeUnit"))
                        .compare(o1, o2, compareOptionalProperty("target"))
                        .compare(o1, o2, compareOptionalProperty("targetCodeElement"))
                        .result();
            }
        };
    }

    private Comparator<JsonElement> compareOptionalProperty(final String propertyName) {
        return new Comparator<JsonElement>() {
            @Override
            public int compare(JsonElement o1, JsonElement o2) {
                return memberAsString(o1, propertyName).compareTo(memberAsString(o2, propertyName));
            }
        };
    }

    private static String memberAsString(JsonElement jsonObject, String memberName) {
        JsonElement member = jsonObject.getAsJsonObject().get(memberName);
        return member != null ? member.getAsString() : "";
    }

    private Iterable<? extends Map.Entry<String, JsonElement>> sortKeys(Set<Map.Entry<String, JsonElement>> entrySet) {
        TreeMap<String, JsonElement> result = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result.entrySet();
    }

}