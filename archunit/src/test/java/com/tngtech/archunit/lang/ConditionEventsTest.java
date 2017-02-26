package com.tngtech.archunit.lang;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
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

    private static ConditionEvents events(ConditionEvent... events) {
        ConditionEvents result = new ConditionEvents();
        for (ConditionEvent event : events) {
            result.add(event);
        }
        return result;
    }
}