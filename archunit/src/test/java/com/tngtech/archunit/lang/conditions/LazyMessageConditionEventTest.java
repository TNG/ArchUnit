package com.tngtech.archunit.lang.conditions;

import java.util.concurrent.atomic.AtomicInteger;

import com.tngtech.archunit.lang.ConditionEvent;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class LazyMessageConditionEventTest {
    @Test
    public void does_not_render_message_until_requested() {
        AtomicInteger renderings = new AtomicInteger();
        ConditionEvent event = new LazyMessageConditionEvent("obj", false, () -> {
            renderings.incrementAndGet();
            return "message";
        });

        assertThat(event.isViolation()).isTrue();
        assertThat(event.invert().isViolation()).isFalse();
        assertThat(renderings).hasValue(0);

        assertThat(event.getDescriptionLines()).isEqualTo(singletonList("message"));
        assertThat(renderings).hasValue(1);
    }

    @Test
    public void hands_message_and_corresponding_object_to_handler() {
        ConditionEvent event = new LazyMessageConditionEvent("obj", true, () -> "message").invert();

        StringBuilder result = new StringBuilder();
        event.handleWith((correspondingObjects, message) -> result.append(correspondingObjects).append(message));

        assertThat(result.toString()).isEqualTo("[obj]message");
    }
}
