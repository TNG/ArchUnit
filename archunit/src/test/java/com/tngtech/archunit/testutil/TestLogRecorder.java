package com.tngtech.archunit.testutil;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.stream.Collectors.toList;

public class TestLogRecorder {
    private static final String APPENDER_NAME = "test_appender";

    private final List<RecordedLogEvent> logEvents = new ArrayList<>();
    private Class<?> loggerClass;
    private Level oldLevel;

    public void record(Class<?> loggerClass, Level level) {
        this.loggerClass = loggerClass;
        Appender appender = new AbstractAppender(APPENDER_NAME, null, PatternLayout.createDefaultLayout(), true, new Property[0]) {
            @Override
            public void append(LogEvent event) {
                logEvents.add(new RecordedLogEvent(event));
            }
        };
        appender.start();
        LoggerContext ctx = getLoggerContext();
        LoggerConfig loggerConfig = ctx.getConfiguration().getLoggerConfig(loggerClass.getName());
        oldLevel = loggerConfig.getLevel();
        loggerConfig.setLevel(level);
        loggerConfig.addAppender(appender, level, null);
        ctx.updateLoggers();
    }

    private LoggerContext getLoggerContext() {
        return (LoggerContext) LogManager.getContext(false);
    }

    public void reset() {
        if (loggerClass == null) {
            return;
        }

        LoggerContext ctx = getLoggerContext();
        LoggerConfig loggerConfig = ctx.getConfiguration().getLoggerConfig(loggerClass.getName());
        loggerConfig.setLevel(oldLevel);
        loggerConfig.removeAppender(APPENDER_NAME);
        ctx.updateLoggers();
    }

    public List<RecordedLogEvent> getEvents() {
        return logEvents;
    }

    public List<RecordedLogEvent> getEvents(Level level) {
        return logEvents.stream().filter(input -> input.getLevel().equals(level)).collect(toList());
    }

    public static class RecordedLogEvent {
        private final Level level;
        private final String message;
        private final Throwable thrown;

        private RecordedLogEvent(Level level, String message, Throwable thrown) {
            this.level = level;
            this.message = message;
            this.thrown = thrown;
        }

        RecordedLogEvent(LogEvent event) {
            this(event.getLevel(), event.getMessage().getFormattedMessage(), event.getThrown());
        }

        Level getLevel() {
            return level;
        }

        Throwable getThrown() {
            return thrown;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return toStringHelper(this)
                    .add("level", level)
                    .add("message", message)
                    .add("thrown", thrown)
                    .toString();
        }
    }
}
