package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SimpleConditionEventTest {
    @Test
    public void passes_corresponding_object_as_single_element_collection_with_message() {
        List<String> messages = new ArrayList<>();
        ConditionEvent.Handler handler = (correspondingObjects, message) ->
                messages.add(getOnlyElement(correspondingObjects) + ": " + message);

        SimpleConditionEvent.satisfied(77, "satisfied").handleWith(handler);
        assertThat(messages).containsExactly("77: satisfied");

        messages.clear();
        SimpleConditionEvent.violated(88, "violated").handleWith(handler);
        assertThat(messages).containsExactly("88: violated");
    }

    @Test
    public void passes_corresponding_object_as_single_element_collection_with_supplied_message() {
        List<String> messages = new ArrayList<>();
        ConditionEvent.Handler handler = (correspondingObjects, message) ->
                messages.add(getOnlyElement(correspondingObjects) + ": " + message);

        SimpleConditionEvent.satisfied(77, () -> "satisfied").handleWith(handler);
        assertThat(messages).containsExactly("77: satisfied");

        messages.clear();
        SimpleConditionEvent.violated(88, () -> "violated").handleWith(handler);
        assertThat(messages).containsExactly("88: violated");
    }

    @Test
    public void supplied_message_is_only_computed_when_requested() {
        AtomicInteger supplied = new AtomicInteger();
        ConditionEvent event = SimpleConditionEvent.violated("obj", () -> {
            supplied.incrementAndGet();
            return "message";
        });

        assertThat(event.isViolation()).isTrue();
        assertThat(event.invert().isViolation()).isFalse();
        assertThat(supplied).hasValue(0);

        assertThat(event.getDescriptionLines()).containsExactly("message");
        assertThat(supplied).hasValue(1);
    }

    @Test
    public void events_with_supplied_and_with_constant_message_behave_the_same() {
        for (boolean satisfied : new boolean[]{true, false}) {
            ConditionEvent constant = new SimpleConditionEvent("obj", satisfied, "message");
            ConditionEvent supplied = new SimpleConditionEvent("obj", satisfied, () -> "message");

            assertSameBehavior(supplied, constant);
            assertSameBehavior(supplied.invert(), constant.invert());
        }
    }

    private void assertSameBehavior(ConditionEvent actual, ConditionEvent expected) {
        assertThat(actual.isViolation()).isEqualTo(expected.isViolation());
        assertThat(actual.getDescriptionLines()).isEqualTo(expected.getDescriptionLines());
        assertThat(actual.toString()).isEqualTo(expected.toString());

        List<Object> actualHandled = new ArrayList<>();
        actual.handleWith((objects, message) -> actualHandled.add(objects + message));
        List<Object> expectedHandled = new ArrayList<>();
        expected.handleWith((objects, message) -> expectedHandled.add(objects + message));
        assertThat(actualHandled).isEqualTo(expectedHandled);
    }

    @Test
    public void rejects_empty_message_of_violation() {
        assertThatThrownBy(() -> SimpleConditionEvent.violated("obj", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message may not be empty for violation");
        assertThatThrownBy(() -> SimpleConditionEvent.satisfied("obj", " ").invert())
                .isInstanceOf(IllegalArgumentException.class);

        ConditionEvent suppliedViolation = SimpleConditionEvent.violated("obj", () -> " ");
        assertThatThrownBy(suppliedViolation::getDescriptionLines).isInstanceOf(IllegalArgumentException.class);
        ConditionEvent invertedSupplied = SimpleConditionEvent.satisfied("obj", () -> " ").invert();
        assertThatThrownBy(invertedSupplied::getDescriptionLines).isInstanceOf(IllegalArgumentException.class);

        assertThat(SimpleConditionEvent.satisfied("obj", () -> " ").getDescriptionLines()).containsExactly(" ");
    }
}
