package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.assertj.core.api.Assertions.assertThat;

public class SimpleConditionEventTest {
    @Test
    public void passes_corresponding_object_as_single_element_collection_with_message() {
        final List<String> messages = new ArrayList<>();
        ConditionEvent.Handler handler = new ConditionEvent.Handler() {
            @Override
            public void handle(Collection<?> correspondingObjects, String message) {
                messages.add(getOnlyElement(correspondingObjects) + ": " + message);
            }
        };

        SimpleConditionEvent.satisfied(77, "satisfied").handleWith(handler);
        assertThat(messages).containsExactly("77: satisfied");

        messages.clear();
        SimpleConditionEvent.violated(88, "violated").handleWith(handler);
        assertThat(messages).containsExactly("88: violated");
    }
}