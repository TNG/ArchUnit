package com.tngtech.archunit.lang;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FailureDisplayFormatFactoryTest {

    @Rule
    public final ArchConfigurationRule configurationRule = new ArchConfigurationRule();

    @Test
    public void configured_failure_display_format_is_used() {
        ArchConfiguration.get()
                .setProperty("failureDisplayFormat", TestFailureDisplayFormat.class.getName());

        FailureDisplayFormat failureDisplayFormat = FailureDisplayFormatFactory.create();

        assertThat(failureDisplayFormat).isInstanceOf(TestFailureDisplayFormat.class);

        String message = failureDisplayFormat.formatFailure(hasDescription("some-rule"),
                new FailureMessages(ImmutableList.of("some-failure"), Optional.<String>empty()),
                Priority.LOW);

        assertThat(message).isEqualTo("test-format: some-rule has [some-failure] with priority LOW");
    }

    static class TestFailureDisplayFormat implements FailureDisplayFormat {
        @Override
        public String formatFailure(HasDescription rule, FailureMessages failureMessages, Priority priority) {
            return "test-format: " + rule.getDescription() + " has " + failureMessages + " with priority " + priority;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private HasDescription hasDescription(final String description) {
        return new HasDescription() {
            @Override
            public String getDescription() {
                return description;
            }
        };
    }
}
