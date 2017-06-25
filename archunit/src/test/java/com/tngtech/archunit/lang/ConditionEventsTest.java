package com.tngtech.archunit.lang;

import java.util.HashSet;
import java.util.Set;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ConditionEventsTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @DataProvider
    public static Object[][] eventsWithEmpty() {
        return $$(
                $(events(SimpleConditionEvent.satisfied("irrelevant", "irrelevant")), false),
                $(events(SimpleConditionEvent.violated("irrelevant", "irrelevant")), false),
                $(new ConditionEvents(), true));
    }

    @Test
    @UseDataProvider("eventsWithEmpty")
    public void isEmpty(ConditionEvents events, boolean expectedEmpty) {
        assertThat(events.isEmpty()).as("events are empty").isEqualTo(expectedEmpty);
    }

    @Test
    public void handleViolations_reports_only_violations_referring_to_the_correct_type() {
        ConditionEvents events = events(
                SimpleConditionEvent.satisfied(new CorrectType("don't handle"), "I'm not violated"),
                SimpleConditionEvent.violated(new WrongType(), "I'm violated, but wrong type"),
                SimpleConditionEvent.violated(new WrongSuperType(), "I'm violated, but wrong type"),
                SimpleConditionEvent.violated(new CorrectType("handle type"), "I'm violated and correct type"),
                SimpleConditionEvent.violated(new CorrectSubType("handle sub type"), "I'm violated and correct sub type"));

        final Set<String> handledFailures = new HashSet<>();
        events.handleViolations(new ObjectToStringAndMessageJoiningTestHandler(handledFailures));

        assertThat(handledFailures).containsOnly(
                "handle type: I'm violated and correct type",
                "handle sub type: I'm violated and correct sub type");

        handledFailures.clear();
        events.handleViolations(new ViolationHandler<CorrectType>() {
            @Override
            public void handle(CorrectType violatingObject, String message) {
                new ObjectToStringAndMessageJoiningTestHandler(handledFailures).handle(violatingObject, message);
            }
        });

        assertThat(handledFailures).containsOnly(
                "handle type: I'm violated and correct type",
                "handle sub type: I'm violated and correct sub type");
    }

    @Test
    public void handleViolations_joins_lines_with_new_line() {
        ConditionEvents events = events(new SimpleConditionEvent(new CorrectType("ignore"), false, "ignore") {
            @Override
            public void describeTo(CollectsLines messages) {
                messages.add("line one");
                messages.add("line two");
            }
        });

        final Set<String> onlyMessage = new HashSet<>();
        events.handleViolations(new ViolationHandler<CorrectType>() {
            @Override
            public void handle(CorrectType violatingObject, String message) {
                onlyMessage.add(message);
            }
        });
        assertThat(onlyMessage).containsOnly("line one" + System.lineSeparator() + "line two");
    }

    @Test
    public void handleViolations_reports_error_on_finding_handle_method() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(getClass().getName() + "$");
        thrown.expectMessage("unique method");
        thrown.expectMessage("handle(T, String.class)");

        events().handleViolations(new ViolationHandler<Object>() {
            @Override
            public void handle(Object violatingObject, String message) {
            }

            public void handle(String violatingObject, String message) {
            }
        });
    }

    private static ConditionEvents events(ConditionEvent... events) {
        ConditionEvents result = new ConditionEvents();
        for (ConditionEvent event : events) {
            result.add(event);
        }
        return result;
    }

    private static class CorrectSubType extends CorrectType {
        CorrectSubType(String message) {
            super(message);
        }
    }

    static class CorrectType extends WrongSuperType {
        String message;

        CorrectType(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    private static class WrongType {
    }

    private static class WrongSuperType {
    }

}