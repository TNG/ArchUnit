package com.tngtech.archunit.lang;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageFormatFactoryTest {

    @Rule
    public final ArchConfigurationRule configurationRule = new ArchConfigurationRule();

    @Test
    public void message_format_property_is_read() {
        ArchConfiguration.get().setProperty("messageFormat", "com.tngtech.archunit.lang.MessageFormatFactoryTest$TestMessageFormat");

        MessageFormat messageFormat = MessageFormatFactory.create();

        assertThat(messageFormat).isInstanceOfAny(TestMessageFormat.class);

        String message = messageFormat.formatFailure(hasDescription("some-rule"),
                new FailureMessages(ImmutableList.of("some-failure"), Optional.<String>absent()),
                Priority.LOW);

        assertThat(message).isEqualTo("test-format: some-rule has [some-failure] with priority LOW");
    }

    static class TestMessageFormat implements MessageFormat {
        @Override
        public String formatFailure(HasDescription rule, FailureMessages failureMessages, Priority priority) {
            return "test-format: " + rule.getDescription() + " has " + failureMessages + " with priority " + priority;
        }
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
