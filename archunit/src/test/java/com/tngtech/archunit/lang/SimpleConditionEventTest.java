package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.assertj.core.api.Assertions.assertThat;

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
}
