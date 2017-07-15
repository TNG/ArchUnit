package com.tngtech.archunit.testutil;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.Assert;
import org.junit.rules.ExternalResource;

public class LogTestRule extends ExternalResource {
    private static final String APPENDER_NAME = "test_appender";

    private final List<LogEvent> logEvents = new ArrayList<>();
    private Class<?> loggerClass;

    public void watch(Class<?> loggerClass) {
        this.loggerClass = loggerClass;
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        Appender appender = new AbstractAppender(APPENDER_NAME, null, PatternLayout.createDefaultLayout()) {
            @Override
            public void append(LogEvent event) {
                logEvents.add(event);
            }
        };
        appender.start();
        LoggerConfig loggerConfig = config.getLoggerConfig(loggerClass.getName());
        loggerConfig.addAppender(appender, Level.ALL, null);
        ctx.updateLoggers();
    }

    @Override
    protected void after() {
        if (loggerClass == null) {
            return;
        }

        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.getConfiguration().getLoggerConfig(loggerClass.getName()).removeAppender(APPENDER_NAME);
        ctx.updateLoggers();
    }

    public void assertLogMessage(Level level, String messagePart) {
        for (LogEvent event : filterByLevel(logEvents, level)) {
            if (event.getMessage().getFormattedMessage().contains(messagePart)) {
                return;
            }
        }

        Assert.fail(String.format(
                "Couldn't find any message with level %s that contains '%s' in%n%s",
                level, messagePart, logEvents));
    }

    public void assertException(Level level, Class<?> exceptionType, String messagePart) {
        for (LogEvent event : filterByLevel(logEvents, level)) {
            if (exceptionType.isInstance(event.getThrown()) && event.getThrown().getMessage().contains(messagePart)) {
                return;
            }
        }

        Assert.fail(String.format(
                "Couldn't find any log event with level %s that contains a %s with message containing '%s' in%n%s",
                level, exceptionType.getSimpleName(), messagePart, logEvents));
    }

    private Iterable<LogEvent> filterByLevel(List<LogEvent> events, final Level level) {
        return FluentIterable.from(events).filter(new Predicate<LogEvent>() {
            @Override
            public boolean apply(LogEvent input) {
                return input.getLevel().equals(level);
            }
        });
    }
}
