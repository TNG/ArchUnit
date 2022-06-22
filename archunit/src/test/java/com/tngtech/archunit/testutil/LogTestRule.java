package com.tngtech.archunit.testutil;

import java.util.List;
import java.util.stream.Stream;

import com.tngtech.archunit.testutil.TestLogRecorder.RecordedLogEvent;
import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.rules.ExternalResource;

public class LogTestRule extends ExternalResource {
    private final TestLogRecorder testLogRecorder = new TestLogRecorder();

    public void watch(Class<?> loggerClass, Level level) {
        testLogRecorder.record(loggerClass, level);
    }

    @Override
    protected void after() {
        testLogRecorder.reset();
    }

    public void assertLogMessage(Level level, String messagePart) {
        List<RecordedLogEvent> events = testLogRecorder.getEvents(level);
        if (events.stream().noneMatch(e -> e.getMessage().contains(messagePart))) {
            Assert.fail(String.format(
                    "Couldn't find any message with level %s that contains '%s' in%n%s",
                    level, messagePart, testLogRecorder.getEvents()));
        }
    }

    public void assertException(Level level, Class<?> exceptionType, String messagePart) {
        List<RecordedLogEvent> events = testLogRecorder.getEvents(level);
        Stream<RecordedLogEvent> eventsWithException = events.stream().filter(e -> exceptionType.isInstance(e.getThrown()));

        if (eventsWithException.noneMatch(e -> e.getThrown().getMessage().contains(messagePart))) {
            Assert.fail(String.format(
                    "Couldn't find any log event with level %s that contains a %s with message containing '%s' in%n%s",
                    level, exceptionType.getSimpleName(), messagePart, testLogRecorder.getEvents()));
        }
    }
}
