package com.tngtech.archunit.exampletest.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class NewConfigurationEvent {
    private final Map<String, String> properties;

    NewConfigurationEvent(Properties properties) {
        this.properties = copy(properties);
    }

    private Map<String, String> copy(Properties props) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            checkArgument(entry.getKey() instanceof String, "Found non String key in properties");
            checkArgument(entry.getValue() instanceof String, "Found non String value in properties");
            result.put((String) entry.getKey(), (String) entry.getValue());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final NewConfigurationEvent other = (NewConfigurationEvent) obj;
        return Objects.equals(this.properties, other.properties);
    }

    @Override
    public String toString() {
        return "NewConfigurationEvent{" +
                "props=" + properties +
                '}';
    }

    private static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
