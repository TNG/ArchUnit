package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.Convertible;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ConditionEventsTest {
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
                SimpleConditionEvent.satisfied(new CorrectType("do not handle"), "I'm not violated"),
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
            public void handle(Collection<CorrectType> violatingObject, String message) {
                new ObjectToStringAndMessageJoiningTestHandler(handledFailures).handle(violatingObject, message);
            }
        });

        assertThat(handledFailures).containsOnly(
                "handle type: I'm violated and correct type",
                "handle sub type: I'm violated and correct sub type");
    }

    @Test
    public void handles_erased_generics_as_upper_bound() {
        ConditionEvents events = events(
                SimpleConditionEvent.violated(new CorrectType("ignore"), "correct"),
                SimpleConditionEvent.violated(new WrongType(), "wrong"));

        Set<String> handledFailureMessages = new HashSet<>();
        events.handleViolations(genericBoundByCorrectType(handledFailureMessages));

        assertThat(handledFailureMessages).containsOnly("correct");

        handledFailureMessages = new HashSet<>();
        events.handleViolations(unboundGeneric(handledFailureMessages));

        assertThat(handledFailureMessages).containsOnly("correct", "wrong");
    }

    private <T extends CorrectType> ViolationHandler<?> genericBoundByCorrectType(final Set<String> handledFailureMessages) {
        return new ViolationHandler<T>() {
            @Override
            public void handle(Collection<T> violatingObjects, String message) {
                handledFailureMessages.add(message);
            }
        };
    }

    private <T> ViolationHandler<?> unboundGeneric(final Set<String> handledFailureMessages) {
        return new ViolationHandler<T>() {
            @Override
            public void handle(Collection<T> violatingObjects, String message) {
                handledFailureMessages.add(message);
            }
        };
    }

    @Test
    public void can_handle_with_generic_superclasses() {
        ConditionEvents events = events(
                SimpleConditionEvent.violated(new Object(), "ignore"),
                SimpleConditionEvent.violated("correct", "ignore"));

        StringHandler handler = new StringHandler();
        events.handleViolations(handler);

        assertThat(handler.getRecorded()).containsOnly("correct");
    }

    @Test
    public void reports_description_lines_of_events() {
        List<String> expectedDescriptionLines = ImmutableList.of("another event message", "some event message");
        ConditionEvents events = events(
                SimpleConditionEvent.violated(new Object(), expectedDescriptionLines.get(1)),
                SimpleConditionEvent.violated(new Object(), expectedDescriptionLines.get(0)));

        assertThat(events.getFailureMessages()).containsExactlyElementsOf(expectedDescriptionLines);
        assertThat(events.getFailureDescriptionLines()).containsExactlyElementsOf(expectedDescriptionLines);
    }

    @Test
    public void handles_converted_types() {
        ConditionEvents events = events(
                SimpleConditionEvent.violated(new CorrectType("correct type"), "message"),
                SimpleConditionEvent.violated(new WrongType(), "message"),
                SimpleConditionEvent.violated(new ConvertibleToSingleCorrectType("correctly converted"), "message"),
                SimpleConditionEvent.violated(new ConvertibleToMultipleCorrectTypes("correctly converted1", "correctly converted2"), "message"));

        CorrectTypeHandler handler = new CorrectTypeHandler();
        events.handleViolations(handler);

        assertThat(handler.getRecorded()).containsOnly(
                new CorrectType("correct type"),
                new CorrectType("correctly converted"),
                new CorrectType("correctly converted1"),
                new CorrectType("correctly converted2"));
    }

    private static class BaseHandler<T> implements ViolationHandler<T> {
        private final List<T> recorded = new ArrayList<>();

        @Override
        public void handle(Collection<T> violatingObjects, String message) {
            recorded.addAll(violatingObjects);
        }

        List<T> getRecorded() {
            return recorded;
        }
    }

    private static class StringHandler extends BaseHandler<String> {
    }

    private static class CorrectTypeHandler extends BaseHandler<CorrectType> {
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

        @Override
        public int hashCode() {
            return Objects.hash(message);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final CorrectType other = (CorrectType) obj;
            return Objects.equals(this.message, other.message);
        }
    }

    private static class WrongType {
    }

    private static class WrongSuperType {
    }

    private static class ConvertibleToSingleCorrectType implements Convertible {
        String message;

        ConvertibleToSingleCorrectType(String message) {
            this.message = message;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Set<T> convertTo(Class<T> type) {
            if (CorrectType.class.isAssignableFrom(type)) {
                return (Set<T>) singleton(new CorrectType(message));
            }
            return emptySet();
        }
    }

    private static class ConvertibleToMultipleCorrectTypes implements Convertible {
        private final String message1;
        private final String message2;

        ConvertibleToMultipleCorrectTypes(String message1, String message2) {
            this.message1 = message1;
            this.message2 = message2;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Set<T> convertTo(Class<T> type) {
            if (CorrectType.class.isAssignableFrom(type)) {
                return (Set<T>) ImmutableSet.of(new CorrectType(message1), new CorrectType(message2));
            }
            return emptySet();
        }
    }
}
