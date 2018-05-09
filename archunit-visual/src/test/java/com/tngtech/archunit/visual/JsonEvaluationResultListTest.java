package com.tngtech.archunit.visual;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.assertj.core.util.Lists;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class JsonEvaluationResultListTest {
    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
    
    @Test
    public void testInsertNewEvaluationResultToEmptyList() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, JSONException {
        JsonEvaluationResultList jsonEvaluationResultList = new JsonEvaluationResultList(Lists.<JsonEvaluationResult>newArrayList());
        JsonEvaluationResult jsonEvaluationResult = new JsonEvaluationResult("Rule1");
        jsonEvaluationResult.addViolation(createJsonViolation("OriginClass1", "TargetClass1"));
        jsonEvaluationResult.addViolation(createJsonViolation("OriginClass2", "TargetClass2"));
        jsonEvaluationResultList.insertEvaluationResult(jsonEvaluationResult);
        String act = gson.toJson(jsonEvaluationResultList.getJsonEvaluationResultList());
        assertStringContainsJsonEqualToFile(act, "two-violations-of-one-rule.json");
    }

    @Test
    public void testInsertNewEvaluationResultOfNewRuleToNonEmptyList() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, JSONException {
        JsonEvaluationResult existingJsonEvaluationResult = new JsonEvaluationResult("Rule1");
        existingJsonEvaluationResult.addViolation(createJsonViolation("OriginClass1", "TargetClass1"));
        existingJsonEvaluationResult.addViolation(createJsonViolation("OriginClass2", "TargetClass2"));
        JsonEvaluationResultList jsonEvaluationResultList = new JsonEvaluationResultList(Lists.newArrayList(existingJsonEvaluationResult));

        JsonEvaluationResult newJsonEvaluationResult = new JsonEvaluationResult("Rule2");
        newJsonEvaluationResult.addViolation(createJsonViolation("OriginClass1", "TargetClass1"));
        newJsonEvaluationResult.addViolation(createJsonViolation("OriginClass2", "TargetClass2"));

        jsonEvaluationResultList.insertEvaluationResult(newJsonEvaluationResult);

        String act = gson.toJson(jsonEvaluationResultList.getJsonEvaluationResultList());
        assertStringContainsJsonEqualToFile(act, "two-violations-of-two-rules.json");
    }

    @Test
    public void testInsertNewEvaluationResultOfExistingRuleToNonEmptyList() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, JSONException {
        JsonEvaluationResult existingJsonEvaluationResult = new JsonEvaluationResult("Rule1");
        existingJsonEvaluationResult.addViolation(createJsonViolation("OriginClass1", "TargetClass1"));
        existingJsonEvaluationResult.addViolation(createJsonViolation("OriginClass2", "TargetClass2"));
        JsonEvaluationResultList jsonEvaluationResultList = new JsonEvaluationResultList(Lists.newArrayList(existingJsonEvaluationResult));

        JsonEvaluationResult newJsonEvaluationResult = new JsonEvaluationResult("Rule1");
        newJsonEvaluationResult.addViolation(createJsonViolation("OriginClass3", "TargetClass3"));
        newJsonEvaluationResult.addViolation(createJsonViolation("OriginClass4", "TargetClass4"));

        jsonEvaluationResultList.insertEvaluationResult(newJsonEvaluationResult);

        String act = gson.toJson(jsonEvaluationResultList.getJsonEvaluationResultList());
        assertStringContainsJsonEqualToFile(act, "four-violations-of-one-rule.json");
    }

    @Test
    public void testInsertExistingEvaluationResultOfExistingRuleToNonEmptyList() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, JSONException {
        JsonEvaluationResult existingJsonEvaluationResult = new JsonEvaluationResult("Rule1");
        existingJsonEvaluationResult.addViolation(createJsonViolation("OriginClass1", "TargetClass1"));
        existingJsonEvaluationResult.addViolation(createJsonViolation("OriginClass2", "TargetClass2"));
        JsonEvaluationResultList jsonEvaluationResultList = new JsonEvaluationResultList(Lists.newArrayList(existingJsonEvaluationResult));

        JsonEvaluationResult newJsonEvaluationResult = new JsonEvaluationResult("Rule1");
        newJsonEvaluationResult.addViolation(createJsonViolation("OriginClass1", "TargetClass1"));

        jsonEvaluationResultList.insertEvaluationResult(newJsonEvaluationResult);

        String act = gson.toJson(jsonEvaluationResultList.getJsonEvaluationResultList());
        assertStringContainsJsonEqualToFile(act, "two-violations-of-one-rule.json");
    }

    private JsonViolation createJsonViolation(String from, String to) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<JsonViolation> constructor = JsonViolation.class.getDeclaredConstructor(String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(from, to);
    }

    private void assertStringContainsJsonEqualToFile(String act, String fileName) throws IOException, JSONException {
        File jsonFile = ResourcesUtils.getResource("testjson/violation/" + fileName);
        String expectedJson = Files.toString(jsonFile, Charsets.UTF_8);
        JSONAssert.assertEquals(expectedJson, act, false);
    }
}
