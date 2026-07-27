package com.tngtech.archunit.junit.internal.testutil;

import java.util.List;

import com.tngtech.archunit.testutil.TestLogRecorder;
import com.tngtech.archunit.testutil.TestLogRecorder.RecordedLogEvent;
import org.apache.logging.log4j.Level;

public class LogCaptor {
    private final TestLogRecorder logRecorder = new TestLogRecorder();

    public void watch(Class<?> loggerClass, Level level) {
        logRecorder.record(loggerClass, level);
    }

    public void cleanUp() {
        logRecorder.reset();
    }

    public List<RecordedLogEvent> getEvents(Level level) {
        return logRecorder.getEvents(level);
    }
}
