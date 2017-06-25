package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.domain.properties.HasDescription;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EvaluationResultTest {
    @Test
    public void properties_are_passed_to_FailureReport() {
        EvaluationResult result = new EvaluationResult(
                hasDescription("special description"),
                events("first bummer", "second bummer"),
                Priority.HIGH);

        assertThat(result.getFailureReport().getDetails()).containsExactly("first bummer", "second bummer");
        assertThat(result.getFailureReport().toString())
                .containsPattern("Priority.*HIGH")
                .contains("special description")
                .contains("first bummer")
                .contains("second bummer");
    }

    private ConditionEvents events(String... messages) {
        ConditionEvents result = new ConditionEvents();
        for (String message : messages) {
            result.add(new SimpleConditionEvent(new Object(), false, message));
        }
        return result;
    }

    private HasDescription hasDescription(final String description) {
        return new HasDescription() {
            @Override
            public String getDescription() {
                return description;
            }
        };
    }
}